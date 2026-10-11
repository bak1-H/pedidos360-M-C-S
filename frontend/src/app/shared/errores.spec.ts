import { HttpErrorResponse } from '@angular/common/http';
import { codigoDeError } from './errores';

describe('codigoDeError', () => {
  it('reads the codigo of the BFF error body', () => {
    const causa = new HttpErrorResponse({ status: 409, error: { status: 409, codigo: 'ENVIO_DUPLICADO' } });

    expect(codigoDeError(causa)).toBe('ENVIO_DUPLICADO');
  });

  it('returns null when there is no codigo or the failure is not an http error', () => {
    expect(codigoDeError(new HttpErrorResponse({ status: 502 }))).toBeNull();
    expect(codigoDeError(new HttpErrorResponse({ status: 500, error: 'texto' }))).toBeNull();
    expect(codigoDeError(new Error('x'))).toBeNull();
  });
});
