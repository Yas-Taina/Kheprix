import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  computed,
  ViewEncapsulation,
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router";
import { EstudoService } from "../../../core/services/estudo.service";
import { DashboardCompletoService } from "../../../core/services/dashboardcompleto.service";
import { Estudo, TipoAgrupamento, FormatoExportacao } from "../../../models";
import {
  EstudoDashboard,
  AgrupamentoTempo,
} from "../../../models/dashboard.model";
import { UtilService } from "../../../core/services/util.service";
import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  DoughnutController,
  ArcElement,
  BarController,
  BarElement,
  Tooltip,
  Legend,
  Filler,
} from "chart.js";
import * as L from "leaflet";

Chart.register(
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  DoughnutController,
  ArcElement,
  BarController,
  BarElement,
  Tooltip,
  Legend,
  Filler,
);

@Component({
  selector: "app-estudo-detalhe",
  standalone: true,
  templateUrl: "./estudo-detalhe.component.html",
  styleUrls: ["./estudo-detalhe.component.css"],
  imports: [CommonModule, FormsModule],
  encapsulation: ViewEncapsulation.None,
})
export class EstudoDetalheComponent implements OnInit, OnDestroy {
  estudo: Estudo | null = null;
  loading = true;
  estudoId!: number;
  showExportar = false;
  agrupamento: TipoAgrupamento = "registro_ocorrencia";
  formato: FormatoExportacao = "csv";
  exportLoading = false;
  exportMsg = "";

  dashboard = signal<EstudoDashboard | null>(null);
  dashboardLoading = signal(false);
  dashboardError = signal<string | null>(null);
  selectedEspecie = signal<string>("");
  selectedAgrupamento = signal<AgrupamentoTempo>("mes");

  especiesDisponiveis = computed(() => {
    const d = this.dashboard();
    if (!d) return [];
    return [
      ...new Set(d.registros_por_especie_tempo.map((r) => r.nome_cientifico)),
    ].sort();
  });

  private chartRegistrosData?: Chart;
  private chartOcorrencias?: Chart;
  private chartEspecieTempo?: Chart;
  private chartNativasInvasoras?: Chart;
  private chartEspeciesDistintas?: Chart;
  private chartEspeciesDistintasTempo?: Chart;
  private map?: L.Map;

  constructor(
    private estudoService: EstudoService,
    private dashboardService: DashboardCompletoService,
    private route: ActivatedRoute,
    public router: Router,
    public util: UtilService,
  ) {}

  ngOnInit(): void {
    this.estudoId = +this.route.snapshot.params["estudo_id"];

    this.estudoService.listar().subscribe({
      next: (lista) => {
        this.estudo = lista.find((e) => e.id === this.estudoId) ?? null;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });

    this.carregarDashboard();
  }

  ngOnDestroy(): void {
    this.chartRegistrosData?.destroy();
    this.chartOcorrencias?.destroy();
    this.chartEspecieTempo?.destroy();
    this.chartNativasInvasoras?.destroy();
    this.chartEspeciesDistintas?.destroy();
    this.chartEspeciesDistintasTempo?.destroy();
    this.map?.remove();
  }

  irParaCampanhas() {
    this.router.navigate(["/estudos", this.estudoId, "campanhas"]);
  }
  irParaNovaEspecie() {
    this.router.navigate(["/estudos", this.estudoId, "especies", "novo"]);
  }
  irParaEspecies() {
    this.router.navigate(["/estudos", this.estudoId, "especies"]);
  }
  irParaEditar() {
    this.router.navigate(["/estudos", this.estudoId, "editar"]);
  }
  irParaAnalises() {
    this.router.navigate(["/estudos", this.estudoId, "analises"]);
  }

  abrirExportar() {
    this.showExportar = true;
    this.exportMsg = "";
  }
  fecharExportar() {
    this.showExportar = false;
  }
 
  exportar(): void {
    this.exportLoading = true;
    this.exportMsg = "";
    this.estudoService
      .exportarDados(this.estudoId, this.agrupamento, this.formato)
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement("a");
          a.href = url;
          a.download = `dados_estudo_${this.estudoId}.${this.formato}`;

          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          setTimeout(() => URL.revokeObjectURL(url), 100);
 
          this.exportLoading = false;
          this.exportMsg = "Arquivo baixado!";
        },
        error: () => {
          this.exportLoading = false;
          this.exportMsg = "Erro ao exportar. Tente novamente.";
        },
      });
  }

  carregarDashboard(): void {
    this.dashboardLoading.set(true);
    this.dashboardError.set(null);
    this.dashboardService.getDashboard(this.estudoId).subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.dashboardLoading.set(false);
        setTimeout(() => this.inicializarGraficos(), 60);
      },
      error: () => {
        this.dashboardError.set(
          "Não foi possível carregar os dados do dashboard.",
        );
        this.dashboardLoading.set(false);
      },
    });
  }

  private inicializarGraficos(): void {
    const d = this.dashboard();
    if (!d) return;
    this.inicializarMapa(d);
    this.buildRegistrosData(d);
    this.buildOcorrencias(d);
    this.buildEspecieTempo(d);
    this.buildNativasInvasoras(d);
    this.buildEspeciesDistintas(d);
    this.buildEspeciesDistintasTempo(d);
  }

  private inicializarMapa(d: EstudoDashboard): void {
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }

    const container = document.getElementById("mapa-registros");
    if (!container) return;

    this.map = L.map(container, {
      zoomControl: true,
      preferCanvas: true,
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "© OpenStreetMap contributors",
      maxZoom: 18,
      tileSize: 256,
      zoomOffset: 0,
    }).addTo(this.map);

    d.pontos_mapa.forEach((p) => {
      L.circleMarker([p.latitude, p.longitude], {
        radius: 7,
        fillColor: "#C0392B",
        color: "#fff",
        weight: 1.5,
        opacity: 1,
        fillOpacity: 0.9,
      })
        .bindPopup(`<b>${p.nome_cientifico}</b><br>Qtd: ${p.quantidade}`)
        .addTo(this.map!);
    });

    if (d.pontos_mapa.length > 0) {
      const bounds = L.latLngBounds(
        d.pontos_mapa.map((p) => [p.latitude, p.longitude] as [number, number]),
      );
      this.map.fitBounds(bounds, { padding: [32, 32] });
    } else {
      this.map.setView([-15, -50], 5);
    }

    setTimeout(() => this.map?.invalidateSize(), 200);
  }

  private buildRegistrosData(d: EstudoDashboard): void {
    this.chartRegistrosData?.destroy();
    const ctx = document.getElementById(
      "chart-registros-data",
    ) as HTMLCanvasElement;
    if (!ctx) return;
    this.chartRegistrosData = new Chart(ctx, {
      type: "line",
      data: {
        labels: d.registros_por_data.map((r) => this.util.formatDate(r.data)),
        datasets: [
          {
            data: d.registros_por_data.map((r) => r.total),
            borderColor: "#6B7D5E",
            backgroundColor: "rgba(107,125,94,0.10)",
            tension: 0.4,
            fill: true,
            pointRadius: 3,
            pointBackgroundColor: "#6B7D5E",
          },
        ],
      },
      options: this.opcoesLinha("Quantidade", "Data"),
    });
  }

  private buildOcorrencias(d: EstudoDashboard): void {
    this.chartOcorrencias?.destroy();
    const ctx = document.getElementById(
      "chart-ocorrencias",
    ) as HTMLCanvasElement;
    if (!ctx) return;
    const cores = [
      "#4A4035",
      "#6B7D5E",
      "#8FA082",
      "#A8B99C",
      "#C5BA9F",
      "#B3C4A7",
      "#DDD8C4",
    ];
    this.chartOcorrencias = new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: d.ocorrencias_por_especie.map(
          (e) => e.nome_popular || e.nome_cientifico,
        ),
        datasets: [
          {
            data: d.ocorrencias_por_especie.map((e) => e.total),
            backgroundColor: cores,
            borderWidth: 0,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "60%",
        plugins: {
          legend: {
            position: "right",
            labels: {
              font: this.fontCormorant(12),
              color: "#4A4035",
              boxWidth: 13,
            },
          },
        },
      },
    });
  }

  buildEspecieTempo(d?: EstudoDashboard): void {
    const data = d ?? this.dashboard();
    if (!data) return;
    this.chartEspecieTempo?.destroy();
    const ctx = document.getElementById(
      "chart-especie-tempo",
    ) as HTMLCanvasElement;
    if (!ctx) return;

    const especie = this.selectedEspecie();
    const filtrado = especie
      ? data.registros_por_especie_tempo.filter(
          (r) => r.nome_cientifico === especie,
        )
      : data.registros_por_especie_tempo;

    const agrupado = new Map<string, number>();
    filtrado.forEach((r) => {
      const key = `${String(r.mes).padStart(2, "0")}/${r.ano}`;
      agrupado.set(key, (agrupado.get(key) ?? 0) + r.total);
    });
    const keys = [...agrupado.keys()].sort();

    this.chartEspecieTempo = new Chart(ctx, {
      type: "line",
      data: {
        labels: keys,
        datasets: [
          {
            data: keys.map((k) => agrupado.get(k)!),
            borderColor: "#6B7D5E",
            backgroundColor: "rgba(107,125,94,0.10)",
            tension: 0.4,
            fill: true,
            pointRadius: 3,
            pointBackgroundColor: "#6B7D5E",
          },
        ],
      },
      options: this.opcoesLinha("Quantidade", "Data"),
    });
  }

  private buildNativasInvasoras(d: EstudoDashboard): void {
    this.chartNativasInvasoras?.destroy();
    const ctx = document.getElementById(
      "chart-nativas-invasoras",
    ) as HTMLCanvasElement;
    if (!ctx) return;
    this.chartNativasInvasoras = new Chart(ctx, {
      type: "bar",
      data: {
        labels: [""],
        datasets: [
          {
            label: "Espécies Nativas",
            data: [d.resumo.especies_nativas],
            backgroundColor: "#4A4035",
            borderRadius: 3,
          },
          {
            label: "Espécies Invasoras",
            data: [d.resumo.especies_invasoras],
            backgroundColor: "#8FA082",
            borderRadius: 3,
          },
        ],
      },
      options: {
        indexAxis: "y",
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: "top",
            labels: {
              font: this.fontCormorant(12),
              color: "#4A4035",
              boxWidth: 12,
            },
          },
        },
        scales: {
          x: {
            grid: { color: "rgba(0,0,0,0.05)" },
            ticks: { font: this.fontCormorant(), color: "#6B5F52" },
          },
          y: { display: false },
        },
      },
    });
  }

  buildEspeciesDistintas(d?: EstudoDashboard): void {
    const data = d ?? this.dashboard();
    if (!data) return;
    this.chartEspeciesDistintas?.destroy();
    const ctx = document.getElementById(
      "chart-especies-distintas",
    ) as HTMLCanvasElement;
    if (!ctx) return;

    const meses = [
      "Jan",
      "Fev",
      "Mar",
      "Abr",
      "Mai",
      "Jun",
      "Jul",
      "Ago",
      "Set",
      "Out",
      "Nov",
      "Dez",
    ];
    let labels: string[];
    let valores: number[];

    if (this.selectedAgrupamento() === "ano") {
      const anos = new Map<string, number>();
      data.especies_distintas_por_mes.forEach((r) => {
        const k = String(r.ano);
        anos.set(k, (anos.get(k) ?? 0) + r.total);
      });
      labels = [...anos.keys()].sort();
      valores = labels.map((k) => anos.get(k)!);
    } else {
      const por = new Map<string, number>();
      data.especies_distintas_por_mes.forEach((r) => {
        por.set(`${meses[r.mes - 1]}/${r.ano}`, r.total);
      });
      labels = [...por.keys()];
      valores = labels.map((k) => por.get(k)!);
    }

    const paleta = ["#4A4035", "#6B7D5E", "#8FA082", "#A8B99C"];
    this.chartEspeciesDistintas = new Chart(ctx, {
      type: "bar",
      data: {
        labels,
        datasets: [
          {
            data: valores,
            backgroundColor: labels.map((_, i) => paleta[i % paleta.length]),
            borderRadius: 3,
          },
        ],
      },
      options: {
        indexAxis: "y",
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: {
            grid: { color: "rgba(0,0,0,0.05)" },
            ticks: { font: this.fontCormorant(), color: "#6B5F52" },
          },
          y: { ticks: { font: this.fontCormorant(11), color: "#4A4035" } },
        },
      },
    });
  }

  onEspecieChange(): void {
    this.buildEspecieTempo();
  }
  onAgrupamentoChange(): void {
    this.buildEspeciesDistintas();
    this.buildEspeciesDistintasTempo();
  }

  buildEspeciesDistintasTempo(d?: EstudoDashboard): void {
    const data = d ?? this.dashboard();
    if (!data) return;
    this.chartEspeciesDistintasTempo?.destroy();
    const ctx = document.getElementById(
      "chart-especies-distintas-tempo",
    ) as HTMLCanvasElement;
    if (!ctx) return;

    const meses = [
      "Jan",
      "Fev",
      "Mar",
      "Abr",
      "Mai",
      "Jun",
      "Jul",
      "Ago",
      "Set",
      "Out",
      "Nov",
      "Dez",
    ];

    let labels: string[];
    let valores: number[];

    if (this.selectedAgrupamento() === "ano") {
      const anos = new Map<string, number>();
      data.especies_distintas_por_mes.forEach((r) => {
        const k = String(r.ano);
        anos.set(k, (anos.get(k) ?? 0) + r.total);
      });
      labels = [...anos.keys()].sort();
      valores = labels.map((k) => anos.get(k)!);
    } else {
      const ordenado = [...data.especies_distintas_por_mes].sort((a, b) =>
        a.ano !== b.ano ? a.ano - b.ano : a.mes - b.mes,
      );
      labels = ordenado.map(
        (r) => `${String(r.mes).padStart(2, "0")}/${r.ano}`,
      );
      valores = ordenado.map((r) => r.total);
    }

    this.chartEspeciesDistintasTempo = new Chart(ctx, {
      type: "line",
      data: {
        labels,
        datasets: [
          {
            data: valores,
            borderColor: "#4A4035",
            backgroundColor: "rgba(74,64,53,0.08)",
            tension: 0.3,
            fill: true,
            pointRadius: 4,
            pointBackgroundColor: "#4A4035",
            pointBorderColor: "#fff",
            pointBorderWidth: 1.5,
          },
        ],
      },
      options: this.opcoesLinha("Quantidade", "Data"),
    });
  }

  private fontCormorant(size = 13) {
    return { family: "'Cormorant Garamond', Georgia, serif", size };
  }

  private opcoesLinha(yLabel: string, xLabel: string): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: {
          title: {
            display: true,
            text: yLabel,
            font: this.fontCormorant(11),
            color: "#6B5F52",
          },
          grid: { color: "rgba(0,0,0,0.05)" },
          ticks: { font: this.fontCormorant(), color: "#6B5F52" },
        },
        x: {
          title: {
            display: true,
            text: xLabel,
            font: this.fontCormorant(11),
            color: "#6B5F52",
          },
          grid: { display: false },
          ticks: {
            font: this.fontCormorant(10),
            color: "#6B5F52",
            maxRotation: 45,
          },
        },
      },
    };
  }
}
