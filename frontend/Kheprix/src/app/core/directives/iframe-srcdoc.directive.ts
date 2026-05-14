import {
  AfterViewInit,
  Directive,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
} from "@angular/core";

// Setter imperativo de `srcdoc` para iframes que renderizam HTML gerado por
// backend (gráficos Plotly). O binding nativo `[srcdoc]` do Angular esbarra
// num problema chato: quando o iframe acaba de ser inserido no DOM e o
// atributo recebe o HTML inicial, o browser frequentemente não dispara o load
// (script da tag <script src=cdn.plot.ly> não roda e o iframe fica em branco).
// Manualmente "piscar" srcdoc com string vazia antes do valor real força o
// reload e o ciclo de boot do iframe finaliza certinho.
@Directive({
  selector: "iframe[appIframeSrcdoc]",
  standalone: true,
})
export class IframeSrcdocDirective implements OnChanges, AfterViewInit, OnDestroy {
  @Input("appIframeSrcdoc") html: string | null | undefined = null;

  private viewReady = false;
  private timeoutId: ReturnType<typeof setTimeout> | null = null;

  constructor(private elRef: ElementRef<HTMLIFrameElement>) {}

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.aplicar();
  }

  ngOnChanges(_changes: SimpleChanges): void {
    if (this.viewReady) this.aplicar();
  }

  ngOnDestroy(): void {
    if (this.timeoutId !== null) clearTimeout(this.timeoutId);
  }

  // Aguarda o iframe processar srcdoc="" (firing seu load com about:srcdoc
  // vazio) antes de injetar o HTML real. Sem isso o browser otimiza os dois
  // sets como equivalentes e o iframe fica em branco — sintoma reproduzido
  // com Plotly nas análises pareadas (pearson/jaccard/glm) tanto em fresh
  // quanto ao revisitar.
  private aplicar(): void {
    const iframe = this.elRef.nativeElement;
    const novoHtml = this.html ?? "";

    if (this.timeoutId !== null) clearTimeout(this.timeoutId);

    const aplicaConteudo = () => {
      iframe.srcdoc = novoHtml;
    };

    // Listener one-shot pro load do srcdoc="". Quando dispara, sabemos que o
    // browser registrou o reset e está pronto pra carregar o conteúdo real.
    const onLoad = () => {
      iframe.removeEventListener("load", onLoad);
      aplicaConteudo();
    };
    iframe.addEventListener("load", onLoad);
    iframe.srcdoc = "";

    // Fallback: se o load não disparar dentro de 200ms (browser otimizando
    // demais, ou iframe já no estado vazio), tenta a atribuição direta.
    this.timeoutId = setTimeout(() => {
      iframe.removeEventListener("load", onLoad);
      aplicaConteudo();
      this.timeoutId = null;
    }, 200);
  }
}
