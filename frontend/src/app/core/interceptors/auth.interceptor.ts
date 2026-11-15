import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { APP_ENVIRONMENT } from '../config/app-environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const isApiRequest = req.url.startsWith(APP_ENVIRONMENT.apiBaseUrl);
  const token = auth.accessToken();

  const authReq =
    isApiRequest && token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authReq).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && isApiRequest) {
        if (err.status === 401) {
          auth.clearSessionLocal();
          void router.navigate(['/login']);
        } else if (err.status === 403 && !router.url.startsWith('/acesso-negado')) {
          void router.navigate(['/acesso-negado']);
        }
      }
      return throwError(() => err);
    }),
  );
};
