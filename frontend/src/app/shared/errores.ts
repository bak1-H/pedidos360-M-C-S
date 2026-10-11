import { HttpErrorResponse } from '@angular/common/http';

export function codigoDeError(causa: unknown): string | null {
  const codigo = causa instanceof HttpErrorResponse ? causa.error?.codigo : null;

  return typeof codigo === 'string' ? codigo : null;
}
