import {
  AfterViewInit,
  Directive,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
} from "@angular/core";

// Atribuição imperativa de srcdoc. Com [srcdoc] do Angular o browser
// frequentemente pula o load inicial do iframe e o <script cdn.plot.ly>
// do conteúdo não roda — iframe fica em branco. Setar srcdoc="" antes
// do HTML real força o ciclo de load e o Plotly inicializa.
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

  private aplicar(): void {
    const iframe = this.elRef.nativeElement;
    const novoHtml = this.html ?? "";

    if (this.timeoutId !== null) clearTimeout(this.timeoutId);

    const aplicaConteudo = () => {
      iframe.srcdoc = novoHtml;
    };

    const onLoad = () => {
      iframe.removeEventListener("load", onLoad);
      aplicaConteudo();
    };
    iframe.addEventListener("load", onLoad);
    iframe.srcdoc = "";

    // Fallback caso o load do srcdoc="" não dispare em 200ms.
    this.timeoutId = setTimeout(() => {
      iframe.removeEventListener("load", onLoad);
      aplicaConteudo();
      this.timeoutId = null;
    }, 200);
  }
}
