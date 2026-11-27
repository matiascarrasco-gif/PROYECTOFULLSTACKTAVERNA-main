import { apiClient } from './auth.js';

// Adaptador REST compartido: centraliza todas las llamadas a Spring Boot.
// listeners mantiene las vistas React suscritas a cambios del catálogo.
const listeners = new Set();
// products guarda temporalmente la lista recibida desde la API.
let products = [];
// Evita que el catálogo se inicialice varias veces.
let initialized = false;
// Permite ejecutar CRUD localmente durante las pruebas de Vitest.
let localMode = false;
// loadError conserva el último error ocurrido al consultar o modificar datos.
let loadError = '';

// Informa a las vistas suscritas que React debe volver a leer el catálogo.
function notify() {
  listeners.forEach((listener) => listener());
}

// Completa campos opcionales para mantener compatible la respuesta JSON con las vistas existentes.
function normalizeProduct(product) {
  const id = product.id;
  return {
    ...product,
    id,
    sourceId: product.sourceId ?? id,
    pointsCost: product.puntosCanje ?? product.pointsCost ?? null,
    sourceRoute: product.sourceRoute || '',
    platform: product.platform || '',
    genre: product.genre || '',
    subcategory: product.subcategory || '',
  };
}

// Convierte errores de Axios en mensajes comprensibles para las vistas.
function getApiError(error) {
  const body = error.response?.data;
  const validationErrors = body?.errors && typeof body.errors === 'object'
    ? Object.values(body.errors).filter((message) => typeof message === 'string').join(' ')
    : '';
  const detail = validationErrors
    || body?.detail
    || body?.message
    || body?.error
    || error.message
    || 'Error al comunicarse con el servidor.';
  return new Error(detail);
}

// Ejecuta una llamada REST y normaliza sus errores para la interfaz.
async function apiRequest(path, options = {}) {
  try {
    const response = await apiClient.request({ url: path, ...options });
    return response.status === 204 ? null : response.data;
  } catch (error) {
    throw getApiError(error);
  }
}

// Valida los datos en React antes de enviarlos a Spring Boot.
function validateProduct(input) {
  const title = String(input.title ?? '').trim();
  const description = String(input.description ?? '').trim();
  const category = String(input.category ?? '').trim();
  const image = String(input.image ?? '').trim();
  const price = Number(input.price);

  if (title.length < 2 || title.length > 120) {
    throw new Error('El nombre debe tener entre 2 y 120 caracteres.');
  }
  if (!description || description.length > 2000) {
    throw new Error('La descripción es obligatoria y no puede superar 2000 caracteres.');
  }
  if (!category) throw new Error('Selecciona una categoría.');
  if (!image || image.length > 500) throw new Error('La ruta de imagen es obligatoria.');
  if (!Number.isFinite(price) || price <= 0) throw new Error('El precio debe ser un número mayor que cero.');

  return {
    title,
    description,
    category,
    image,
    price,
    sourceRoute: input.sourceRoute || '',
    platform: input.platform || '',
    genre: input.genre || '',
    subcategory: input.subcategory || '',
  };
}

// Carga el catálogo inicial mediante GET /api/productos al iniciar React.
export async function initializeProductDatabase(seedProducts = []) {
  if (initialized) return;
  initialized = true;

  try {
    const data = await apiRequest('/productos');
    products = Array.isArray(data) ? data.map(normalizeProduct) : [];
    loadError = '';
  } catch (error) {
    loadError = error instanceof Error
      ? `No se pudo conectar con la API: ${error.message}`
      : 'No se pudo conectar con la API.';
    products = Array.isArray(seedProducts) ? [...seedProducts] : [];
  }
  notify();
}

// Registra un componente para recibir cambios posteriores del catálogo.
export function subscribeProducts(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

// Entrega a React la versión actual del catálogo en memoria.
export function getProducts() {
  return products;
}

export function getProductLoadError() {
  return loadError;
}

// Las imágenes cargadas se sirven desde la misma base de API que usa Axios.
export function getProductImageUrl(image) {
  if (typeof image !== 'string' || !image.startsWith('/uploads/product-images/')) return image;
  const apiBase = String(apiClient.defaults?.baseURL || '/api').replace(/\/+$/, '');
  return `${apiBase}${image}`;
}

// Sube una imagen al servidor y devuelve la ruta estable que se guarda con el producto.
export async function uploadProductImage(file) {
  if (!file) throw new Error('Selecciona una imagen.');
  if (!['image/png', 'image/jpeg', 'image/gif'].includes(file.type)) {
    throw new Error('Selecciona una imagen PNG, JPG o GIF.');
  }
  if (file.size > 5 * 1024 * 1024) {
    throw new Error('La imagen no puede superar los 5 MB.');
  }

  const formData = new FormData();
  formData.append('file', file);
  const response = await apiRequest('/uploads/product-images', {
    method: 'POST',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.image;
}

// Busca un producto por su identificador REST y por una ruta heredada opcional.
export function getProductById(id, sourceRoute) {
  return products.find((product) =>
    String(product.id) === String(id) && (!sourceRoute || product.sourceRoute.toLowerCase() === sourceRoute.toLowerCase()),
  ) ?? products.find((product) =>
    String(product.sourceId) === String(id) && (!sourceRoute || product.sourceRoute.toLowerCase() === sourceRoute.toLowerCase()));
}

// POST: valida y crea un producto mediante /api/productos.
export async function createProduct(input) {
  const normalized = validateProduct(input);

  if (localMode) {
    const id = `admin-${Date.now()}-${products.length + 1}`;
    const product = normalizeProduct({ id, sourceId: id, ...normalized });
    products = [...products, product];
    notify();
    return product;
  }

  try {
    const product = normalizeProduct(await apiRequest('/productos', {
      method: 'POST',
      data: normalized,
    }));
    products = [...products, product];
    loadError = '';
    notify();
    return product;
  } catch (error) {
    loadError = error instanceof Error ? error.message : 'No se pudo crear el producto.';
    notify();
    throw error;
  }
}

// PUT: reemplaza los datos de un producto existente en /api/productos/{id}.
export async function updateProduct(id, input) {
  const current = getProductById(id);
  if (!current) throw new Error('No se encontró el producto que intentas editar.');
  const normalized = validateProduct(input);

  if (localMode) {
    const updated = normalizeProduct({ ...current, ...normalized });
    products = products.map((product) => String(product.id) === String(id) ? updated : product);
    notify();
    return updated;
  }

  try {
    const updated = normalizeProduct(await apiRequest(`/productos/${encodeURIComponent(id)}`, {
      method: 'PUT',
      data: normalized,
    }));
    products = products.map((product) => String(product.id) === String(id) ? updated : product);
    loadError = '';
    notify();
    return updated;
  } catch (error) {
    loadError = error instanceof Error ? error.message : 'No se pudo actualizar el producto.';
    notify();
    throw error;
  }
}

// DELETE: solicita a Spring que elimine el producto con el ID indicado.
export async function deleteProduct(id) {
  if (!products.some((product) => String(product.id) === String(id))) {
    throw new Error('No se encontró el producto que intentas eliminar.');
  }

  if (localMode) {
    products = products.filter((product) => String(product.id) !== String(id));
    notify();
    return;
  }

  try {
    await apiRequest(`/productos/${encodeURIComponent(id)}`, { method: 'DELETE' });
    products = products.filter((product) => String(product.id) !== String(id));
    loadError = '';
    notify();
  } catch (error) {
    loadError = error instanceof Error ? error.message : 'No se pudo eliminar el producto.';
    notify();
    throw error;
  }
}

export function resetProductDatabase(seedProducts) {
  initialized = true;
  localMode = true;
  loadError = '';
  products = [...seedProducts];
  notify();
}
