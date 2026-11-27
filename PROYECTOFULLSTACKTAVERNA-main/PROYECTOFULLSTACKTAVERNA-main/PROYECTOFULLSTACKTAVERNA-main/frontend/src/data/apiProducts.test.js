import { beforeEach, describe, expect, it, vi } from 'vitest';

const axiosRequest = vi.hoisted(() => vi.fn());

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      request: axiosRequest,
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    })),
  },
}));

import {
  createProduct,
  deleteProduct,
  getProducts,
  getProductImageUrl,
  initializeProductDatabase,
  uploadProductImage,
  updateProduct,
} from './apiProducts.js';

const initialProduct = {
  id: 41,
  title: 'Teclado de prueba',
  description: 'Producto para probar Axios.',
  price: 54990,
  image: '/images/accesorios/teclado.webp',
  category: 'Accesorios',
};

const input = {
  title: 'Teclado actualizado',
  description: 'Producto actualizado mediante Axios.',
  price: '59990',
  image: '/images/accesorios/teclado.webp',
  category: 'Accesorios',
};

describe('apiProducts Axios adapter', () => {
  beforeEach(() => {
    axiosRequest.mockReset();
  });

  it('loads products and sends create, update and delete requests to the REST API', async () => {
    axiosRequest
      .mockResolvedValueOnce({ status: 200, data: [initialProduct] })
      .mockResolvedValueOnce({
        status: 201,
        data: { ...initialProduct, id: 42, title: 'Teclado nuevo' },
      })
      .mockResolvedValueOnce({
        status: 200,
        data: { ...initialProduct, title: input.title, price: 59990 },
      })
      .mockResolvedValueOnce({ status: 204, data: null });

    await initializeProductDatabase();
    expect(axiosRequest).toHaveBeenNthCalledWith(1, { url: '/productos' });
    expect(getProducts()).toHaveLength(1);

    await createProduct({ ...input, title: 'Teclado nuevo' });
    expect(axiosRequest).toHaveBeenNthCalledWith(2, {
      url: '/productos',
      method: 'POST',
      data: {
        ...input,
        title: 'Teclado nuevo',
        price: 59990,
        sourceRoute: '',
        platform: '',
        genre: '',
        subcategory: '',
      },
    });

    await updateProduct(41, input);
    expect(axiosRequest).toHaveBeenNthCalledWith(3, {
      url: '/productos/41',
      method: 'PUT',
      data: {
        ...input,
        price: 59990,
        sourceRoute: '',
        platform: '',
        genre: '',
        subcategory: '',
      },
    });

    await deleteProduct(41);
    expect(axiosRequest).toHaveBeenNthCalledWith(4, {
      url: '/productos/41',
      method: 'DELETE',
    });
    expect(getProducts().map((product) => product.id)).toEqual([42]);
  });

  it('uploads supported product images and resolves their backend URL', async () => {
    const file = new File(['image data'], 'producto.png', { type: 'image/png' });
    axiosRequest.mockResolvedValueOnce({
      status: 200,
      data: { image: '/uploads/product-images/0b48d35f-2ae9-4475-a8d1-c0d6edcdb412.png' },
    });

    const image = await uploadProductImage(file);

    expect(image).toBe('/uploads/product-images/0b48d35f-2ae9-4475-a8d1-c0d6edcdb412.png');
    expect(axiosRequest).toHaveBeenCalledWith(expect.objectContaining({
      url: '/uploads/product-images',
      method: 'POST',
      headers: { 'Content-Type': 'multipart/form-data' },
    }));
    expect(axiosRequest.mock.calls[0][0].data.get('file')).toBe(file);
    expect(getProductImageUrl(image)).toBe(`/api${image}`);
  });

  it('rejects unsupported image types before sending a request', async () => {
    const file = new File(['not an image'], 'producto.svg', { type: 'image/svg+xml' });

    await expect(uploadProductImage(file)).rejects.toThrow('Selecciona una imagen PNG, JPG o GIF.');
    expect(axiosRequest).not.toHaveBeenCalled();
  });
});
