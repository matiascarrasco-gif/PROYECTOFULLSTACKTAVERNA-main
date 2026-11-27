// Pruebas del panel: permisos, formulario y acciones del catálogo.
import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AdminProductsPage from './AdminProductsPage.jsx';
import { getProducts, resetProductDatabase } from '../data/apiProducts.js';
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

const fixture = {
  id: 'fixture-console',
  sourceId: 'fixture-console',
  title: 'Consola de prueba',
  description: 'Consola para pruebas unitarias.',
  price: 100000,
  image: '/images/consolas/ps1.png',
  category: 'Consolas',
  sourceRoute: 'Consolas.html',
  platform: '',
  genre: '',
  subcategory: '',
};

function renderAdminPage(path = '/admin-panel.html') {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/admin-panel.html" element={<AdminProductsPage />} />
        <Route path="/admin-catalogo.html" element={<AdminProductsPage view="catalog" />} />
        <Route path="/admin-agregar-producto.html" element={<AdminProductsPage view="form" />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('AdminProductsPage', () => {
  beforeEach(async () => {
    localStorage.clear();
    await logout();
    api.request.mockReset();
    api.post.mockReset().mockImplementation((path, body) => {
      if (path === '/auth/logout') return Promise.resolve({ status: 204, data: null });
      return Promise.resolve({
        status: 200,
        data: {
          accessToken: 'admin-test-token',
          expiresAt: new Date(Date.now() + 60_000).toISOString(),
          user: { id: 'admin-1', correo: body.correo, role: 'ADMIN', isAdmin: true },
        },
      });
    });
    await login({ correo: 'admin@example.com', password: 'secure-password' });
    resetProductDatabase([fixture]);
  });

  it('requires an administrator session before displaying product management', async () => {
    await logout();
    renderAdminPage();

    expect(screen.getByRole('heading', { name: 'Acceso restringido' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Crear producto' })).toBeNull();
  });

  it('creates a product and displays validation errors for invalid prices', async () => {
    renderAdminPage();
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Teclado React' } });
    fireEvent.change(screen.getByLabelText('Precio (CLP)'), { target: { value: '0' } });
    fireEvent.change(screen.getByLabelText('Descripción'), { target: { value: 'Teclado para pruebas.' } });
    fireEvent.change(screen.getByLabelText('Ruta de imagen o URL'), { target: { value: '/images/accesorios/teclado1.webp' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Crear producto' }).closest('form'));

    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toContain('mayor que cero');
      expect(getProducts()).toHaveLength(1);
    });

    fireEvent.change(screen.getByLabelText('Precio (CLP)'), { target: { value: '54990' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Crear producto' }).closest('form'));

    await waitFor(() => {
      expect(screen.getByRole('status').textContent).toBe('Producto creado.');
      expect(getProducts()).toHaveLength(2);
    });
  });

  it('uploads a selected image before creating the product', async () => {
    api.request.mockResolvedValue({
      status: 200,
      data: { image: '/uploads/product-images/0b48d35f-2ae9-4475-a8d1-c0d6edcdb412.png' },
    });
    renderAdminPage();
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Teclado con foto' } });
    fireEvent.change(screen.getByLabelText('Precio (CLP)'), { target: { value: '54990' } });
    fireEvent.change(screen.getByLabelText('Descripción'), { target: { value: 'Teclado para probar la carga.' } });
    fireEvent.change(screen.getByLabelText('O selecciona una imagen desde tu computador'), {
      target: { files: [new File(['imagen'], 'teclado.png', { type: 'image/png' })] },
    });
    fireEvent.submit(screen.getByRole('button', { name: 'Crear producto' }).closest('form'));

    await waitFor(() => {
      expect(api.request).toHaveBeenCalledWith(expect.objectContaining({
        url: '/uploads/product-images',
        method: 'POST',
      }));
      expect(getProducts().at(-1).image).toBe('/uploads/product-images/0b48d35f-2ae9-4475-a8d1-c0d6edcdb412.png');
      expect(screen.getByRole('status').textContent).toBe('Producto creado.');
    });
  });

  it('updates and deletes the selected catalogue product', async () => {
    renderAdminPage();
    fireEvent.click(screen.getByRole('button', { name: 'Editar Consola de prueba' }));
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Consola actualizada' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => {
      expect(getProducts()[0].title).toBe('Consola actualizada');
    });

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Consola actualizada' }));
    await waitFor(() => {
      expect(getProducts()).toHaveLength(0);
      expect(screen.getByText('Producto eliminado.')).toBeTruthy();
    });
  });

  it('separates the catalog and add-product views into different routes', () => {
    const catalog = renderAdminPage('/admin-catalogo.html');
    expect(screen.getByRole('heading', { name: 'Catálogo de productos' })).toBeTruthy();
    expect(screen.queryByLabelText('Nombre')).toBeNull();
    catalog.unmount();

    renderAdminPage('/admin-agregar-producto.html');
    expect(screen.getByRole('heading', { name: 'Agregar producto', level: 1 })).toBeTruthy();
    expect(screen.getByLabelText('Nombre')).toBeTruthy();
    expect(screen.queryByRole('heading', { name: 'Catálogo', exact: true })).toBeNull();
  });

  it('opens the separate add-product route with the selected product loaded for editing', async () => {
    renderAdminPage('/admin-catalogo.html');
    fireEvent.click(screen.getByRole('button', { name: 'Editar Consola de prueba' }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Editar producto' })).toBeTruthy();
      expect(screen.getByLabelText('Nombre').value).toBe('Consola de prueba');
    });
  });
});
