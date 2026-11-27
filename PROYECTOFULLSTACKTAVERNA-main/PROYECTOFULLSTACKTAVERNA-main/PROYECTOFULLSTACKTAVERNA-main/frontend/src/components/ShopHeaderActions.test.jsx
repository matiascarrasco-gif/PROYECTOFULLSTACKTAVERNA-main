// Pruebas de acceso, sesión, carrito, reseñas y edición de perfil.
import React from 'react';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ShopHeaderActions from './ShopHeaderActions.jsx';
import SiteHeader from './SiteHeader.jsx';
import CartPage from '../pages/CartPage.jsx';
import { login, logout } from '../data/auth.js';

const api = vi.hoisted(() => ({
  request: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      ...api,
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    })),
  },
}));

function makeUser(correo, role = 'CUSTOMER') {
  return {
    id: 'account-1',
    correo,
    nombres: 'Camila',
    apellidoPaterno: 'Rojas',
    apellidoMaterno: 'Pérez',
    rut: '123456785',
    telefono: '912345678',
    direccion: 'Calle de prueba',
    role,
    isAdmin: role === 'ADMIN',
    puntos: 0,
  };
}

describe('ShopHeaderActions', () => {
  beforeEach(async () => {
    localStorage.clear();
    await logout();
    api.post.mockReset().mockImplementation((path, body) => {
      if (path === '/auth/logout') return Promise.resolve({ status: 204, data: null });
      const correo = body.correo;
      const role = correo === 'admin@example.com' ? 'ADMIN' : 'CUSTOMER';
      return Promise.resolve({
        status: path === '/auth/register' ? 201 : 200,
        data: {
          accessToken: 'test-jwt-token',
          expiresAt: new Date(Date.now() + 60_000).toISOString(),
          user: makeUser(correo, role),
        },
      });
    });
    api.put.mockImplementation((_path, profile) => Promise.resolve({
      status: 200,
      data: { ...makeUser('camila@example.com'), ...profile },
    }));
  });

  it('opens the login and registration forms', () => {
    render(<ShopHeaderActions />);

    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(screen.getByRole('dialog', { name: /Mi Cuenta/ })).toBeTruthy();

    fireEvent.click(screen.getByRole('tab', { name: 'Registrarse' }));
    expect(screen.getByLabelText('Nombres')).toBeTruthy();
  });

  it('opens registration directly from the create-account header button', () => {
    render(<ShopHeaderActions />);

    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(screen.getByLabelText('Nombres')).toBeTruthy();
    expect(screen.getByLabelText('Correo electrónico')).toBeTruthy();
  });

  it('does not expose the cart until a customer has signed in', () => {
    localStorage.setItem('carrito', JSON.stringify([
      { id: 'console-1', titulo: 'Consola de prueba', precio: 100000, imagen: '/images/consolas/ps1.png', cantidad: 1 },
    ]));
    render(<ShopHeaderActions />);

    expect(screen.queryByRole('link', { name: 'Abrir carrito, 1 productos' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeTruthy();
  });

  it('shows the cart link for an active account', async () => {
    await login({ correo: 'gamer@example.com', password: 'secure-password' });
    localStorage.setItem('carrito', JSON.stringify([
      { id: 'console-1', titulo: 'Consola de prueba', precio: 100000, cantidad: 1 },
    ]));
    render(<ShopHeaderActions />);

    expect(screen.getByRole('link', { name: 'Abrir carrito, 1 productos' }).getAttribute('href')).toBe('/carrito.html');
  });

  it('signs in through the API and permits sign out', async () => {
    render(<ShopHeaderActions />);
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'gamer@duoc.cl' } });
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByLabelText('Correo electrónico').closest('form'));

    await waitFor(() => expect(JSON.parse(localStorage.getItem('usuarioActivo')).correo).toBe('gamer@duoc.cl'));
    expect(localStorage.getItem('taverna_access_token')).toBe('test-jwt-token');
    fireEvent.click(screen.getAllByRole('button', { name: 'Mi cuenta' })[0]);
    expect(screen.getByRole('dialog', { name: 'Mi Perfil' })).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar Sesión' }));
    await waitFor(() => expect(localStorage.getItem('usuarioActivo')).toBeNull());
    expect(screen.queryByRole('link', { name: /Abrir carrito/ })).toBeNull();
  });

  it('validates and saves profile RUT and synchronizes account details with checkout', async () => {
    await login({ correo: 'camila@example.com', password: 'secure-password' });
    localStorage.setItem('carrito', JSON.stringify([
      { id: 'game-1', titulo: 'Juego', precio: 1000, cantidad: 1 },
    ]));
    render(<><ShopHeaderActions /><CartPage /></>);

    fireEvent.click(screen.getAllByRole('button', { name: 'Mi cuenta' })[0]);
    fireEvent.change(screen.getByLabelText('Apellido paterno'), { target: { value: 'González' } });
    fireEvent.change(screen.getByLabelText('RUT (sin puntos ni guion)'), { target: { value: '123456784' } });
    expect(screen.getByRole('alert').textContent).toContain('dígito verificador');

    fireEvent.change(screen.getByLabelText('RUT (sin puntos ni guion)'), { target: { value: '123456785' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.getByLabelText('Apellido paterno *').value).toBe('González'));
    expect(screen.getByLabelText('RUT').value).toBe('123456785');
    expect(JSON.parse(localStorage.getItem('usuarioActivo')).apellidoPaterno).toBe('González');
  });

  it('requires a session before adding a product and adds it after login', async () => {
    render(<ShopHeaderActions />);
    const product = { id: 'p1', title: 'Control', price: 50000, image: '/control.png', category: 'Accesorios' };
    act(() => {
      window.dispatchEvent(new CustomEvent('taverna:login-required', { detail: { product } }));
    });

    expect(screen.getByRole('status').textContent).toContain('primero debes iniciar sesión');
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'gamer@example.com' } });
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByLabelText('Correo electrónico').closest('form'));

    await waitFor(() => expect(JSON.parse(localStorage.getItem('carrito'))[0].titulo).toBe('Control'));
    expect(screen.getByRole('link', { name: 'Abrir carrito, 1 productos' })).toBeTruthy();
  });

  it('requests sign-in for a review without adding a product to the cart', async () => {
    render(<ShopHeaderActions />);
    act(() => {
      window.dispatchEvent(new CustomEvent('taverna:login-required', { detail: { action: 'review' } }));
    });

    expect(screen.getByRole('status').textContent).toContain('dejar una reseña');
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'gamer@example.com' } });
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByLabelText('Correo electrónico').closest('form'));

    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('compartir tu reseña'));
    expect(localStorage.getItem('carrito')).toBeNull();
  });

  it('offers administrator sign-in only on the Blog page', () => {
    window.history.replaceState({}, '', '/blog.html');
    const { unmount } = render(<ShopHeaderActions />);
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(screen.getByRole('button', { name: 'Iniciar sesión como administrador' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Registrar administrador' })).toBeTruthy();
    unmount();

    window.history.replaceState({}, '', '/');
    render(<ShopHeaderActions />);
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(screen.queryByRole('button', { name: 'Iniciar sesión como administrador' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Registrar administrador' })).toBeNull();
  });

  it('sends administrator registration through the invitation-protected endpoint', async () => {
    const invitationKey = 'a-long-test-invitation-key-with-32-plus-characters';
    api.post.mockRejectedValueOnce({
      response: { data: { detail: 'La clave de invitación no es válida.' } },
    });
    window.history.replaceState({}, '', '/blog.html');
    render(<ShopHeaderActions />);

    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    fireEvent.click(screen.getByRole('button', { name: 'Registrar administrador' }));
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Camila' } });
    fireEvent.change(screen.getByLabelText('Apellido paterno'), { target: { value: 'Rojas' } });
    fireEvent.change(screen.getByLabelText('Apellido materno'), { target: { value: 'Pérez' } });
    fireEvent.change(screen.getByLabelText('RUT (sin puntos ni guion)'), { target: { value: '123456785' } });
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'admin-nuevo@example.com' } });
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'clave-admin-segura-123' } });
    fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), { target: { value: '1990-01-01' } });
    fireEvent.change(screen.getByLabelText('Teléfono'), { target: { value: '912345678' } });
    fireEvent.change(screen.getByLabelText('Dirección'), { target: { value: 'Calle de prueba' } });
    fireEvent.change(screen.getByLabelText('Código de invitación (mínimo 32 caracteres)'), {
      target: { value: invitationKey },
    });
    fireEvent.submit(screen.getByLabelText('Correo electrónico').closest('form'));

    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('clave de invitación'));
    expect(api.post).toHaveBeenCalledWith(
      '/auth/admin/register',
      expect.objectContaining({ correo: 'admin-nuevo@example.com' }),
      { headers: { 'X-Admin-Registration-Key': invitationKey } },
    );
  });

  it('does not retain a customer session after a denied administrator login', async () => {
    window.history.replaceState({}, '', '/blog.html');
    render(<ShopHeaderActions />);
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión como administrador' }));
    fireEvent.change(screen.getByLabelText('Correo de administrador'), {
      target: { value: 'cliente@example.com' },
    });
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secure-password' } });
    fireEvent.submit(screen.getByLabelText('Correo de administrador').closest('form'));

    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toContain('no tiene permisos de administrador');
      expect(localStorage.getItem('taverna_access_token')).toBeNull();
    });
  });

  it('shows catalog and add-product navigation only to administrators after technical service', async () => {
    render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><SiteHeader /></MemoryRouter>);
    const navigation = screen.getByRole('navigation', { name: 'Navegación principal' });

    expect(within(navigation).queryByRole('link', { name: 'Catálogo' })).toBeNull();
    await act(async () => {
      await login({ correo: 'cliente@example.com', password: 'secure-password' });
    });
    expect(within(navigation).queryByRole('link', { name: 'Agregar producto' })).toBeNull();

    await act(async () => {
      await logout();
      await login({ correo: 'admin@example.com', password: 'secure-password' });
    });

    const catalogLink = await within(navigation).findByRole('link', { name: 'Catálogo' });
    const addProductLink = within(navigation).getByRole('link', { name: 'Agregar producto' });
    const navigationLabels = within(navigation).getAllByRole('link').map((link) => link.textContent);

    expect(navigationLabels.indexOf('Servicio Técnico')).toBeLessThan(navigationLabels.indexOf('Catálogo'));
    expect(catalogLink.getAttribute('href')).toBe('/admin-catalogo.html');
    expect(addProductLink.getAttribute('href')).toBe('/admin-agregar-producto.html');
  });
});
