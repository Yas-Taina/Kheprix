import {
  Component,
  OnInit,
  AfterViewChecked,
  OnDestroy,
  ElementRef,
  ViewChild,
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Location } from "@angular/common";
import { ChatbotService } from "../../core/services/chatbot.service";
import { EstudoService } from "../../core/services/estudo.service";
import {
  MensagemChat,
  EstudoOpcao,
  DadosTabela,
  InsightsMetricas,
} from "../../models";

@Component({
  selector: "app-chatbot",
  standalone: true,
  templateUrl: "./chatbot.component.html",
  styleUrls: ["./chatbot.component.css"],
  imports: [CommonModule, FormsModule],
})
export class ChatbotComponent implements OnInit, AfterViewChecked, OnDestroy {
  @ViewChild("chatArea") private chatArea!: ElementRef<HTMLDivElement>;

  mensagens: MensagemChat[] = [];
  perguntaAtual = "";
  carregando = false;
  estudos: EstudoOpcao[] = [];
  aguardandoSelecaoEstudos = false;
  mensagemCarregando = "";
  private deveRolarChat = false;
  private intervaloCarregando: ReturnType<typeof setInterval> | null = null;
  private readonly STORAGE_KEY = "kheprix_chat_historico";

  private readonly mensagensQuery = [
    "Analisando sua pergunta...",
    "Gerando a consulta...",
    "Consultando os dados...",
    "Processando os resultados...",
    "Interpretando os dados...",
    "Verificando os registros...",
    "Preparando a resposta...",
  ];

  private readonly mensagensInsights = [
    "Coletando métricas dos estudos...",
    "Analisando espécies registradas...",
    "Calculando índices de diversidade...",
    "Verificando status de conservação...",
    "Analisando padrões sazonais...",
    "Gerando relatório analítico...",
    "Interpretando os dados...",
  ];

  constructor(
    private chatbotService: ChatbotService,
    private estudoService: EstudoService,
    private location: Location,
  ) {}

  ngOnInit(): void {
    this.estudoService.listar().subscribe({
      next: (lista) => {
        this.estudos = lista.map((e) => ({
          id: e.id,
          nome: e.nome,
          selecionado: false,
        }));
      },
    });

    const historico = this.carregarHistorico();
    if (historico.length > 0) {
      this.mensagens = historico;
      this.deveRolarChat = true;
    } else {
      this.adicionarBoasVindas();
    }
  }

  ngAfterViewChecked(): void {
    if (this.deveRolarChat) {
      this.rolarParaBaixo();
      this.deveRolarChat = false;
    }
  }

  enviarPergunta(): void {
    const texto = this.perguntaAtual.trim();
    if (!texto || this.carregando || this.aguardandoSelecaoEstudos) return;

    this.adicionarMensagem({ tipo: "usuario", conteudo: texto });
    this.perguntaAtual = "";
    this.iniciarCarregando("query");
    this.carregando = true;

    const ids = this.estudos.map((e) => e.id);

    this.chatbotService.pergunta(texto, ids).subscribe({
      next: (res) => {
        this.removerCarregando();
        if (res.erro) {
          this.adicionarMensagem({
            tipo: "bot",
            conteudo: res.resposta ?? "Ocorreu um erro. Tente novamente.",
          });
          return;
        }
        const tabela = this.montarTabela(res.dados);
        this.adicionarMensagem({
          tipo: "bot",
          conteudo: res.resposta ?? "Consulta realizada.",
          tabela,
          sql: res.sql ?? undefined,
          mostrarSql: false,
        });
      },
      error: (err) => {
        this.removerCarregando();
        this.carregando = false;
        this.adicionarMensagem({
          tipo: "bot",
          conteudo: this.extrairMensagemErro(
            err,
            "Não foi possível conectar ao assistente. Tente novamente.",
          ),
        });
      },
      complete: () => {
        this.carregando = false;
      },
    });
  }

  onEnter(event: KeyboardEvent): void {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      this.enviarPergunta();
    }
  }

  iniciarInsights(): void {
    if (this.carregando || this.aguardandoSelecaoEstudos) return;
    this.aguardandoSelecaoEstudos = true;
    this.adicionarMensagem({ tipo: "sistema", conteudo: "" });
  }

  confirmarInsights(): void {
    const ids = this.estudos.filter((e) => e.selecionado).map((e) => e.id);
    if (!ids.length) return;

    this.removerUltimaSistema();
    this.aguardandoSelecaoEstudos = false;
    this.estudos.forEach((e) => (e.selecionado = false));

    const nomes = this.estudos
      .filter((e) => ids.includes(e.id))
      .map((e) => e.nome)
      .join(", ");
    this.adicionarMensagem({
      tipo: "usuario",
      conteudo: `Gerar insights para: ${nomes}`,
    });
    this.iniciarCarregando("insights");
    this.carregando = true;

    this.chatbotService.insights(ids).subscribe({
      next: (res) => {
        this.removerCarregando();
        if (res.erro) {
          this.adicionarMensagem({
            tipo: "bot",
            conteudo: res.narrativa ?? "Ocorreu um erro. Tente novamente.",
          });
          return;
        }
        this.adicionarMensagem({
          tipo: "insights",
          conteudo: res.narrativa,
          metricas: res.metricas as InsightsMetricas,
          mostrarMetricas: false,
        });
      },
      error: (err) => {
        this.removerCarregando();
        this.carregando = false;
        this.adicionarMensagem({
          tipo: "bot",
          conteudo: this.extrairMensagemErro(
            err,
            "Não foi possível gerar os insights. Tente novamente.",
          ),
        });
      },
      complete: () => {
        this.carregando = false;
      },
    });
  }

  cancelarInsights(): void {
    this.removerUltimaSistema();
    this.aguardandoSelecaoEstudos = false;
  }

  toggleEstudo(estudo: EstudoOpcao): void {
    estudo.selecionado = !estudo.selecionado;
  }

  toggleMetricas(msg: MensagemChat): void {
    msg.mostrarMetricas = !msg.mostrarMetricas;
  }

  voltar(): void {
    this.location.back();
  }

  ngOnDestroy(): void {
    this.pararMensagemCarregando();
  }

  limparConversa(): void {
    this.pararMensagemCarregando();
    this.mensagens = [];
    this.aguardandoSelecaoEstudos = false;
    this.carregando = false;
    localStorage.removeItem(this.STORAGE_KEY);
    this.adicionarBoasVindas();
  }

  get algumEstudoSelecionado(): boolean {
    return this.estudos.some((e) => e.selecionado);
  }

  colunasDaTabela(linhas: Record<string, unknown>[]): string[] {
    if (!linhas?.length) return [];
    return Object.keys(linhas[0]);
  }

  metricasSections(
    metricas: InsightsMetricas,
  ): { chave: string; label: string; dados: Record<string, unknown>[] }[] {
    return [
      { chave: "resumo", label: "Resumo Geral", dados: metricas.resumo ?? [] },
      {
        chave: "top_especies",
        label: "Top Espécies",
        dados: metricas.top_especies ?? [],
      },
      {
        chave: "conservacao",
        label: "Conservação",
        dados: metricas.conservacao ?? [],
      },
      {
        chave: "sazonalidade",
        label: "Sazonalidade",
        dados: metricas.sazonalidade ?? [],
      },
      {
        chave: "taxonomia",
        label: "Taxonomia",
        dados: metricas.taxonomia ?? [],
      },
    ].filter((s) => s.dados.length > 0);
  }

  private adicionarBoasVindas(): void {
    this.adicionarMensagem({
      tipo: "bot",
      conteudo:
        "Olá! Sou o assistente IA do Kheprix. Posso responder perguntas sobre seus dados de campo ou gerar um relatório de insights. Como posso ajudar?",
    });
  }

  private adicionarMensagem(msg: MensagemChat): void {
    this.mensagens.push(msg);
    this.deveRolarChat = true;
    this.salvarHistorico();
  }

  private salvarHistorico(): void {
    const persistiveis = this.mensagens.filter(
      (m) => m.tipo !== "carregando" && m.tipo !== "sistema",
    );
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(persistiveis));
  }

  private carregarHistorico(): MensagemChat[] {
    try {
      const raw = localStorage.getItem(this.STORAGE_KEY);
      return raw ? (JSON.parse(raw) as MensagemChat[]) : [];
    } catch {
      return [];
    }
  }

  private iniciarCarregando(tipo: "query" | "insights"): void {
    const lista =
      tipo === "insights" ? this.mensagensInsights : this.mensagensQuery;
    let idx = 0;
    this.mensagemCarregando = lista[0];
    this.adicionarMensagem({ tipo: "carregando", conteudo: "" });
    this.intervaloCarregando = setInterval(() => {
      idx = (idx + 1) % lista.length;
      this.mensagemCarregando = lista[idx];
    }, 2200);
  }

  private pararMensagemCarregando(): void {
    if (this.intervaloCarregando !== null) {
      clearInterval(this.intervaloCarregando);
      this.intervaloCarregando = null;
    }
    this.mensagemCarregando = "";
  }

  private removerCarregando(): void {
    this.pararMensagemCarregando();
    for (let i = this.mensagens.length - 1; i >= 0; i--) {
      if (this.mensagens[i].tipo === "carregando") {
        this.mensagens.splice(i, 1);
        break;
      }
    }
  }

  private removerUltimaSistema(): void {
    for (let i = this.mensagens.length - 1; i >= 0; i--) {
      if (this.mensagens[i].tipo === "sistema") {
        this.mensagens.splice(i, 1);
        break;
      }
    }
  }

  private montarTabela(
    dados: Record<string, unknown>[],
  ): DadosTabela | undefined {
    if (!dados?.length) return undefined;
    return { colunas: Object.keys(dados[0]), linhas: dados };
  }

  private extrairMensagemErro(err: unknown, fallback: string): string {
    const body = (err as { error?: { erro?: string; detail?: unknown } })
      ?.error;
    if (body?.erro) return body.erro;
    if (typeof body?.detail === "string") return body.detail;
    return fallback;
  }

  private rolarParaBaixo(): void {
    try {
      this.chatArea.nativeElement.scrollTop =
        this.chatArea.nativeElement.scrollHeight;
    } catch {}
  }
}
