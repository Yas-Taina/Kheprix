import { Injectable, inject, OnDestroy } from "@angular/core";
import { Router } from "@angular/router";
import {
  Subscription,
  interval,
  switchMap,
  catchError,
  of,
  filter,
  from,
  fromEvent,
  merge,
} from "rxjs";
import { AuthService } from "./auth.service";

const DEFAULT_INTERVAL_MS = 5 * 60 * 1000;

@Injectable({ providedIn: "root" })
export class TokenValidatorService implements OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private subscription: Subscription | null = null;

  start(intervalMs = DEFAULT_INTERVAL_MS): void {
    this.stop();
    const periodic$ = interval(intervalMs);
    const onVisible$ = fromEvent(document, "visibilitychange").pipe(
      filter(() => document.visibilityState === "visible"),
    );

    const immediate$ = from([0]);

    this.subscription = merge(immediate$, periodic$, onVisible$)
      .pipe(
        filter(() => this.authService.isLoggedIn()),
        switchMap(() =>
          this.authService.validarToken(this.authService.getToken()!).pipe(
            catchError(() => {
              console.warn(
                "TokenValidator: falha ao validar token (cheque sua rede)",
              );
              return of({ valido: true });
            }),
          ),
        ),
      )
      .subscribe(({ valido }) => {
        if (!valido) {
          this.handleInvalidToken();
        }
      });
  }

  stop(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;
  }

  ngOnDestroy(): void {
    this.stop();
  }

  private handleInvalidToken(): void {
    this.authService.logout();
    this.router.navigate(["/login"], {
      queryParams: { sessionExpired: true },
    });
  }
}
