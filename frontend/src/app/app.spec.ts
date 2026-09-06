import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';

const authServiceMock = {
  isAuthenticated: () => false,
  displayName: () => 'Sesion no iniciada',
  email: () => 'Conecta tu cuenta',
  roles: () => [] as string[],
  lastMessage: () => 'Ready to authenticate with Azure AD.',
  refreshState: () => 'idle',
  hasAnyRole: () => false,
  loginPopup: () => Promise.resolve(),
  loginRedirect: () => undefined,
  logout: () => undefined,
  refreshSession: () => Promise.resolve(),
};

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceMock }],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Pedidos360');
  });
});
