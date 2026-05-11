import {
  ApplicationConfig,
  APP_INITIALIZER,
  provideZoneChangeDetection,
} from "@angular/core";
import { provideRouter, withComponentInputBinding } from "@angular/router";
import {
  provideHttpClient,
  withInterceptorsFromDi,
  HTTP_INTERCEPTORS,
} from "@angular/common/http";

import { routes } from "./app.routes";
import { TipoDadoInterceptor } from "./core/interceptors/tipo-dado.interceptor";
import { AuthInterceptor } from "./core/interceptors/auth.interceptor";
import { TokenValidatorService } from "./core/services/token-validator.service";

function initTokenValidator(validator: TokenValidatorService) {
  return () => {
    validator.start();
    return Promise.resolve();
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({
      eventCoalescing: true,
    }),

    provideRouter(routes, withComponentInputBinding()),

    provideHttpClient(withInterceptorsFromDi()),

    {
      provide: HTTP_INTERCEPTORS,
      useClass: TipoDadoInterceptor,
      multi: true,
    },
    /*     {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },

    {
      provide: APP_INITIALIZER,
      useFactory: initTokenValidator,
      deps: [TokenValidatorService],
      multi: true,
    }, */
  ],
};
