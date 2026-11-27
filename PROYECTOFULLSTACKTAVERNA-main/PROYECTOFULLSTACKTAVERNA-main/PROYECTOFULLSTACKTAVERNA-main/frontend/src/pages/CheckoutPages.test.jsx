// Pruebas del checkout, validaciones, precarga de perfil y resultado de pago.
import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import CartPage from './CartPage.jsx';
import PurchaseResultPage from './PurchaseResultPage.jsx';
import { validateCheckout } from '../data/cart.js';
import { ACCESS_TOKEN_EXPIRES_KEY, ACCESS_TOKEN_KEY, login, logout, restoreSession } from '../data/auth.js';

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

describe('checkout and purchase result pages', () => {
  beforeEach(async () => {
    localStorage.clear();
    await logout();
    api.post.mockReset().mockImplementation((_path, body) => Promise.resolve({
      status: 200,
      data: {
        accessToken: 'customer-test-token',
        expiresAt: new Date(Date.now() + 60_000).toISOString(),
        user: {
          id: 'customer-1',
          nombres: 'Gamer',
          apellidoPaterno: 'Rojas',
          apellidoMaterno: 'Pérez',
          correo: body.correo,
          rut: '123456785',
          role: 'CUSTOMER',
        },
      },
    }));
    window.history.replaceState({}, '', '/');
  });

  it('requires an account before displaying the checkout form', () => {
    render(<CartPage />);

    expect(screen.getByRole('heading', { name: 'Inicia sesión para acceder al carrito' })).toBeTruthy();
    expect(screen.queryByLabelText('Calle y número *')).toBeNull();
  });

  it('waits for and reflects a restored session after the cart has mounted', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'persisted-customer-token');
    localStorage.setItem(ACCESS_TOKEN_EXPIRES_KEY, new Date(Date.now() + 60_000).toISOString());
    localStorage.setItem('carrito', JSON.stringify([
      { id: 'mouse-1', titulo: 'Mouse gamer', precio: 25990, imagen: '/mouse.webp', cantidad: 1 },
    ]));
    let resolveSession;
    api.get.mockImplementationOnce(() => new Promise((resolve) => {
      resolveSession = resolve;
    }));

    const restoration = restoreSession();
    render(<CartPage />);

    expect(screen.getByRole('heading', { name: 'Comprobando tu sesión' })).toBeTruthy();
    expect(screen.queryByRole('heading', { name: 'Inicia sesión para acceder al carrito' })).toBeNull();

    await act(async () => {
      resolveSession({ status: 200, data: {
        id: 'customer-1',
        nombres: 'Gamer',
        apellidoPaterno: 'Rojas',
        apellidoMaterno: 'Pérez',
        correo: 'gamer@example.com',
        role: 'CUSTOMER',
      } });
      await restoration;
    });

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Carrito de compras' })).toBeTruthy());
    expect(screen.getByRole('heading', { name: 'Mouse gamer' })).toBeTruthy();
    expect(screen.getByLabelText('Correo electrónico *').value).toBe('gamer@example.com');
  });

  it('shows item details and the delivery information form for signed-in customers', async () => {
    await login({ correo: 'gamer@example.com', password: 'secure-password' });
    localStorage.setItem('carrito', JSON.stringify([
      { id: 'mouse-1', titulo: 'Mouse gamer', precio: 25990, imagen: '/mouse.webp', cantidad: 2, seccion: 'Accesorios' },
    ]));
    render(<CartPage />);

    expect(screen.getByRole('heading', { name: 'Carrito de compras' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Mouse gamer' })).toBeTruthy();
    expect(screen.getByLabelText('Nombres *').value).toBe('Gamer');
    expect(screen.getByLabelText('Correo electrónico *').value).toBe('gamer@example.com');
    expect(screen.getByLabelText('Departamento (opcional)')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Finalizar compra simulada' }).disabled).toBe(false);
  });

  it('finalizes the simulated purchase, awards backend points, and empties the cart', async () => {
    api.post.mockImplementation((path, body) => {
      if (path === '/orders') {
        return Promise.resolve({
          status: 201,
          data: {
            orderId: 'order-123',
            subtotal: 200000,
            discount: 0,
            total: 200000,
            pointsEarned: 20,
            puntosSaldo: 20,
            user: {
              id: 'customer-1',
              nombres: 'Gamer',
              apellidoPaterno: 'Rojas',
              apellidoMaterno: 'Pérez',
              correo: 'gamer@example.com',
              rut: '123456785',
              puntos: 20,
              role: 'CUSTOMER',
            },
          },
        });
      }
      return Promise.resolve({
        status: 200,
        data: {
          accessToken: 'customer-test-token',
          expiresAt: new Date(Date.now() + 60_000).toISOString(),
          user: {
            id: 'customer-1',
            nombres: 'Gamer',
            apellidoPaterno: 'Rojas',
            apellidoMaterno: 'Pérez',
            correo: body.correo,
            rut: '123456785',
            role: 'CUSTOMER',
          },
        },
      });
    });

    await login({ correo: 'gamer@example.com', password: 'secure-password' });
    localStorage.setItem('carrito', JSON.stringify([
      { id: 1, productId: 1, titulo: 'Mouse gamer', precio: 200000, cantidad: 1 },
    ]));
    render(<CartPage />);

    fireEvent.change(screen.getByLabelText('Calle y número *'), { target: { value: 'Av. Siempre Viva 123' } });
    fireEvent.change(screen.getByLabelText('Región *'), { target: { value: 'Metropolitana' } });
    fireEvent.change(screen.getByLabelText('Comuna *'), { target: { value: 'Santiago' } });
    fireEvent.click(screen.getByRole('button', { name: 'Finalizar compra simulada' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/orders', expect.objectContaining({
      items: [{ productId: 1, quantity: 1 }],
    })));
    await waitFor(() => expect(JSON.parse(localStorage.getItem('carrito'))).toEqual([]));
    expect(screen.getByRole('heading', { name: /Compra finalizada \(simulación\)/ })).toBeTruthy();
    expect(screen.getByText('Tu pedido fue registrado correctamente. No se realizó ningún cobro real.')).toBeTruthy();
    expect(screen.getByRole('status').textContent).toContain('Ganaste 20 Puntos LevelUp.');
    const savedOrder = JSON.parse(localStorage.getItem('taverna:last-order'));
    expect(savedOrder.status).toBe('success');
    expect(savedOrder.pointsEarned).toBe(20);
    expect(savedOrder.puntosSaldo).toBe(20);
  });

  it('keeps the cart and displays the incident reference when the backend fails', async () => {
    api.post.mockImplementation((path) => {
      if (path === '/orders') {
        return Promise.reject({
          response: {
            data: {
              detail: 'Ocurrió un error interno en el servidor.',
              properties: { incidentId: 'incident-123' },
            },
          },
        });
      }
      return Promise.resolve({
        status: 200,
        data: {
          accessToken: 'customer-test-token',
          expiresAt: new Date(Date.now() + 60_000).toISOString(),
          user: {
            id: 'customer-1',
            nombres: 'Gamer',
            apellidoPaterno: 'Rojas',
            apellidoMaterno: 'Pérez',
            correo: 'gamer@example.com',
            rut: '123456785',
            role: 'CUSTOMER',
          },
        },
      });
    });

    await login({ correo: 'gamer@example.com', password: 'secure-password' });
    const savedCart = [{ id: 1, productId: 1, titulo: 'Mouse gamer', precio: 200000, cantidad: 1 }];
    localStorage.setItem('carrito', JSON.stringify(savedCart));
    render(<CartPage />);

    fireEvent.change(screen.getByLabelText('Calle y número *'), { target: { value: 'Av. Siempre Viva 123' } });
    fireEvent.change(screen.getByLabelText('Región *'), { target: { value: 'Metropolitana' } });
    fireEvent.change(screen.getByLabelText('Comuna *'), { target: { value: 'Santiago' } });
    fireEvent.click(screen.getByRole('button', { name: 'Finalizar compra simulada' }));

    const errorMessage = (await screen.findByRole('alert')).textContent;
    expect(errorMessage).toContain('Referencia: incident-123');
    expect(errorMessage.match(/incident-123/g)).toHaveLength(1);
    expect(JSON.parse(localStorage.getItem('carrito'))).toEqual(savedCart);
    expect(screen.queryByRole('heading', { name: /Compra finalizada/ })).toBeNull();
  });

  it('prefills both surnames and RUT from the validated account', async () => {
    await login({ correo: 'camila@example.com', password: 'secure-password' });
    localStorage.setItem('carrito', JSON.stringify([
      { id: 'game-1', titulo: 'Juego', precio: 1000, cantidad: 1 },
    ]));
    render(<CartPage />);

    expect(screen.getByLabelText('Apellido paterno *').value).toBe('Rojas');
    expect(screen.getByLabelText('Apellido materno *').value).toBe('Pérez');
    expect(screen.getByLabelText('RUT').value).toBe('123456785');
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('shows a RUT validation warning inline when the checkout RUT is invalid', async () => {
    await login({ correo: 'camila@example.com', password: 'secure-password' });
    render(<CartPage />);
    fireEvent.change(screen.getByLabelText('RUT'), { target: { value: '123456784' } });

    expect(screen.getByRole('alert').textContent).toContain('dígito verificador');
  });

  it('reports missing customer and delivery fields for unsuccessful payments', () => {
    const errors = validateCheckout({
      nombres: '',
      apellidos: '',
      correo: 'invalid-email',
      calle: '',
      region: '',
      comuna: '',
    }, [{ id: 'item-1' }]);

    expect(errors).toEqual([
      'El nombre es obligatorio.',
      'Los apellidos son obligatorios.',
      'Ingresa un correo electrónico válido.',
      'La calle y número son obligatorios.',
      'La región es obligatoria.',
      'La comuna es obligatoria.',
    ]);
  });

  it('accepts a complete delivery address for simulated payment', () => {
    expect(validateCheckout({
      nombres: 'Camila',
      apellidos: 'Rojas',
      correo: 'camila@example.com',
      calle: 'Av. Siempre Viva 123',
      region: 'Metropolitana',
      comuna: 'Santiago',
    }, [{ id: 'item-1' }])).toEqual([]);
  });

  it('renders the failed payment number, customer data, and validation messages', () => {
    localStorage.setItem('taverna:last-order', JSON.stringify({
      number: 483216,
      customer: {
        nombres: 'Ana',
        apellidos: 'Pérez',
        correo: 'ana@example.com',
        calle: '',
        departamento: '',
        region: '',
        comuna: 'Santiago',
        indicaciones: '',
      },
      products: [],
      total: 0,
      errors: ['El nombre es obligatorio.', 'La región es obligatoria.'],
    }));
    render(<PurchaseResultPage success={false} />);

    expect(screen.getByRole('heading', { name: 'No se pudo realizar el pago nro. 483216' })).toBeTruthy();
    expect(screen.getByRole('alert').textContent).toContain('La región es obligatoria.');
    expect(screen.getByText('Ana Pérez')).toBeTruthy();
  });

  it('confirms the simulated purchase and explains when no points were earned', () => {
    localStorage.setItem('taverna:last-order', JSON.stringify({
      number: 483216,
      customer: {
        nombres: 'Ana',
        apellidos: 'Pérez',
        correo: 'ana@example.com',
        calle: 'Calle 123',
        departamento: '',
        region: 'Metropolitana',
        comuna: 'Santiago',
        indicaciones: '',
      },
      products: [],
      total: 100000,
      pointsEarned: 0,
      puntosSaldo: 10,
      status: 'success',
    }));
    render(<PurchaseResultPage success />);

    expect(screen.getByRole('heading', { name: 'Compra finalizada (simulación) nro. 483216' })).toBeTruthy();
    expect(screen.getByText('Tu pedido fue registrado correctamente. No se realizó ningún cobro real.')).toBeTruthy();
    expect(screen.getByRole('status').textContent).toContain('no alcanzó el monto mínimo');
    expect(screen.getByRole('status').textContent).toContain('Saldo actual: 10');
  });
});
