import {
  Directive,
  ElementRef,
  HostListener,
  Input,
  forwardRef,
} from "@angular/core";
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from "@angular/forms";

@Directive({
  selector: "[dmsMask]",
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DmsMaskDirective),
      multi: true,
    },
  ],
})
export class DmsMaskDirective implements ControlValueAccessor {
  @Input("dmsMask") mode: "lat" | "lng" = "lat";

  private onChange: (v: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private el: ElementRef<HTMLInputElement>) {}

  private get degDigits(): number {
    return this.mode === "lng" ? 3 : 2;
  }

  private get totalDigits(): number {
    return this.degDigits + 4;
  }

  writeValue(value: string): void {
    this.el.nativeElement.value = value ?? "";
  }
  registerOnChange(fn: (v: string) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  @HostListener("focus")
  onFocus(): void {
    const el = this.el.nativeElement;
    setTimeout(() => el.setSelectionRange(el.value.length, el.value.length), 0);
  }

  @HostListener("blur")
  onBlur(): void {
    this.onTouched();
  }

  @HostListener("keydown", ["$event"])
  onKeydown(event: KeyboardEvent): void {
    const key = event.key;

    if (["Tab", "ArrowLeft", "ArrowRight", "Home", "End"].includes(key)) {
      return;
    }

    if (key === "Backspace") {
      event.preventDefault();
      this.handleBackspace();
      return;
    }

    if (key === "Delete") {
      event.preventDefault();
      return;
    }

    if (key === "-") {
      event.preventDefault();
      this.handleMinus();
      return;
    }

    if (!/^\d$/.test(key)) {
      event.preventDefault();
      return;
    }

    event.preventDefault();
    this.handleDigit(key);
  }

  @HostListener("paste", ["$event"])
  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData("text") ?? "";
    const digits = pasted.replace(/[^\d]/g, "").slice(0, this.totalDigits);
    const negative = pasted.trim().startsWith("-");
    const rebuilt = this.buildMask(digits, negative);
    this.el.nativeElement.value = rebuilt;
    this.onChange(rebuilt);
  }

  private getDigits(value: string): string {
    return value.replace(/[^\d]/g, "");
  }

  private isNegative(value: string): boolean {
    return value.startsWith("-");
  }

  private buildMask(digits: string, negative: boolean): string {
    const dd = this.degDigits;
    const total = this.totalDigits;

    const d = digits.padEnd(total, "").slice(0, total);
    const deg = d.slice(0, dd);
    const min = d.slice(dd, dd + 2);
    const sec = d.slice(dd + 2, dd + 4);

    const hasMin = digits.length > dd;
    const hasSec = digits.length > dd + 2;

    let result = negative ? "-" : "";
    result += deg;
    if (digits.length > 0) result += "°";
    if (hasMin) result += min + "'";
    if (hasSec) result += sec + '"';

    return result;
  }

  private handleDigit(digit: string): void {
    const el = this.el.nativeElement;
    const digits = this.getDigits(el.value);
    const negative = this.isNegative(el.value);

    if (digits.length >= this.totalDigits) return;

    const newDigits = digits + digit;
    const rebuilt = this.buildMask(newDigits, negative);
    el.value = rebuilt;
    this.onChange(rebuilt);
  }

  private handleBackspace(): void {
    const el = this.el.nativeElement;
    const digits = this.getDigits(el.value);
    const negative = this.isNegative(el.value);

    if (digits.length === 0) {
      if (negative) {
        el.value = "";
        this.onChange("");
      }
      return;
    }

    const newDigits = digits.slice(0, -1);
    const rebuilt =
      newDigits.length === 0 && !negative
        ? ""
        : this.buildMask(newDigits, negative);

    el.value = rebuilt;
    this.onChange(rebuilt);
  }

  private handleMinus(): void {
    const el = this.el.nativeElement;
    const digits = this.getDigits(el.value);
    const negative = this.isNegative(el.value);

    const newNegative = !negative;
    const rebuilt =
      digits.length === 0
        ? newNegative
          ? "-"
          : ""
        : this.buildMask(digits, newNegative);

    el.value = rebuilt;
    this.onChange(rebuilt);
  }
}
