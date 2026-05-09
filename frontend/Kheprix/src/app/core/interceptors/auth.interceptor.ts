import { Injectable, inject } from "@angular/core";
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse,
} from "@angular/common/http";
import { Router } from "@angular/router";
import { Observable, throwError } from "rxjs";
import { catchError } from "rxjs/operators";
import { AuthService } from "../services/auth.service";

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  intercept(
    req: HttpRequest<unknown>,
    next: HttpHandler,
  ): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          const isPublicRoute = this.isAuthRoute(this.router.url);

          if (!isPublicRoute) {
            this.authService.logout();
            this.router.navigate(["/login"], {
              queryParams: { sessionExpired: true },
            });
          }
        }

        return throwError(() => error);
      }),
    );
  }

  private isAuthRoute(url: string): boolean {
    const publicPaths = ["/login", "/cadastro", "/redefinir-senha"];
    return publicPaths.some((path) => url.startsWith(path));
  }
}