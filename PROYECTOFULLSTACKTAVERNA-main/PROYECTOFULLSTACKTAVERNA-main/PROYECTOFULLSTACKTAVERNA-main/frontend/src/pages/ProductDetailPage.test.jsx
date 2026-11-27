// Pruebas del detalle del producto y del acceso requerido para reseñas.
import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import SiteLayout from '../components/SiteLayout.jsx';
import ProductDetailPage from './ProductDetailPage.jsx';
import { resetProductDatabase } from '../data/apiProducts.js';
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

const product = {
  id: 'juegos-gta6',
  sourceId: 'gta6',
  title: 'Grand Theft Auto VI',
  description: 'Juego de prueba.',
  price: 96000,
  image: '/images/juegos/grand-theft-auto-vi-ps5.webp',
  category: 'Juegos',
  sourceRoute: 'Juegos.html',
  platform: 'PS5',
  genre: 'accion',
  subcategory: '',
};

describe('ProductDetailPage reviews', () => {
  beforeEach(async () => {
    localStorage.clear();
    await logout();
    api.post.mockReset().mockImplementation((_path, body) => Promise.resolve({
      status: 200,
      data: {
        accessToken: 'customer-test-token',
        expiresAt: new Date(Date.now() + 60_000).toISOString(),
        user: { id: 'customer-1', nombres: 'Matías Gamer', correo: body.correo, role: 'CUSTOMER' },
      },
    }));
    resetProductDatabase([product]);
    window.history.replaceState({}, '', '/producto.html?id=juegos-gta6');
  });

  it('loads reviews saved with the legacy product key', () => {
    localStorage.setItem('resenas_gta6', JSON.stringify([
      { usuario: 'Camila', estrellas: 4, comentario: 'Muy buen juego.' },
    ]));

    render(<SiteLayout><ProductDetailPage /></SiteLayout>);

    expect(screen.getByText('Muy buen juego.')).toBeTruthy();
    expect(screen.getByText('— Camila')).toBeTruthy();
  });

  it('requires a signed-in user before publishing a review', () => {
    render(<SiteLayout><ProductDetailPage /></SiteLayout>);
    fireEvent.change(screen.getByLabelText('Comentario'), { target: { value: 'Excelente producto.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enviar reseña' }));

    expect(screen.getByRole('dialog', { name: /Mi Cuenta/ })).toBeTruthy();
    expect(localStorage.getItem('resenas_gta6')).toBeNull();
  });

  it('saves a review for the signed-in user', async () => {
    await login({ correo: 'gamer@example.com', password: 'secure-password' });
    render(<SiteLayout><ProductDetailPage /></SiteLayout>);
    fireEvent.change(screen.getByLabelText('Comentario'), { target: { value: 'Excelente producto.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enviar reseña' }));

    const savedReviews = JSON.parse(localStorage.getItem('resenas_gta6'));
    expect(savedReviews[0].usuario).toBe('Matías');
    expect(savedReviews[0].comentario).toBe('Excelente producto.');
    expect(screen.getByText('Excelente producto.')).toBeTruthy();
  });
});
