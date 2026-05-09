import { ApplicationConfig, provideZoneChangeDetection } from "@angular/core";

import { provideRouter, withComponentInputBinding } from "@angular/router";

import {
  provideHttpClient,
  withInterceptorsFromDi,
  HTTP_INTERCEPTORS,
} from "@angular/common/http";

import { routes } from "./app.routes";

import { TipoDadoInterceptor } from "./core/interceptors/tipo-dado.interceptor";

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
  ],
};
