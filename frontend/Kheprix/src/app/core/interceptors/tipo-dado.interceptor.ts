import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { Observable, map } from 'rxjs';

@Injectable()
export class TipoDadoInterceptor implements HttpInterceptor {

  private backendToFrontend: any = {
    number: 'numerico',
    string: 'texto',
    boolean: 'logico',
  };

  private frontendToBackend: any = {
    numerico: 'number',
    texto: 'string',
    logico: 'boolean',
  };

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    // FRONT → BACK
    const newReq = req.clone({
      body: this.transformRequest(req.body)
    });

    return next.handle(newReq).pipe(
      map(event => {
        if (event instanceof HttpResponse) {
          return event.clone({
            body: this.transformResponse(event.body)
          });
        }
        return event;
      })
    );
  }

  private transformRequest(body: any): any {
    return this.deepTransform(body, (value, key) => {
      if (key === 'tipo_dado' && this.frontendToBackend[value]) {
        return this.frontendToBackend[value];
      }
      return value;
    });
  }

  private transformResponse(body: any): any {
    return this.deepTransform(body, (value, key) => {
      if (key === 'tipo_dado' && this.backendToFrontend[value]) {
        return this.backendToFrontend[value];
      }
      return value;
    });
  }

  private deepTransform(obj: any, fn: (value: any, key: string) => any): any {
    if (!obj) return obj;

    if (Array.isArray(obj)) {
      return obj.map(item => this.deepTransform(item, fn));
    }

    if (typeof obj === 'object') {
      const newObj: any = {};
      for (const key of Object.keys(obj)) {
        const value = this.deepTransform(obj[key], fn);
        newObj[key] = fn(value, key);
      }
      return newObj;
    }

    return obj;
  }
}