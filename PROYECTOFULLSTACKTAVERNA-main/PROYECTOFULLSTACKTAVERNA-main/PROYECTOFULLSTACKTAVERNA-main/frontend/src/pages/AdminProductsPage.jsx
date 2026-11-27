// Panel de administración: permite crear, editar, buscar y eliminar productos.
import React, { useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import ProductCard from '../components/ProductCard.jsx';
import { getAuthState, subscribeAuth } from '../data/auth.js';
import {
  createProduct,
  deleteProduct,
  getProducts,
  getProductImageUrl,
  subscribeProducts,
  uploadProductImage,
  updateProduct,
} from '../data/apiProducts.js';

const categories = [
  'Accesorios',
  'Consolas',
  'Juegos',
  'Mouse / Mousepad',
  'PC Gamers',
  'Poleras Personalizadas',
  'Polerones Gamers Personalizados',
  'Sillas Gamers',
];
// Estado inicial reutilizado cuando se limpia o cancela el formulario.
const emptyForm = { title: '', description: '', price: '', image: '', category: 'Accesorios' };

export default function AdminProductsPage({ view = 'all' }) {
  // El catálogo se actualiza en vivo cuando el CRUD local hace cambios.
  const location = useLocation();
  const navigate = useNavigate();
  const products = useSyncExternalStore(subscribeProducts, getProducts, getProducts);
  const authState = useSyncExternalStore(subscribeAuth, getAuthState, getAuthState);
  const authorized = authState.ready && (authState.user?.isAdmin || authState.user?.role === 'ADMIN');
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState('');
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [message, setMessage] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const imageInput = useRef(null);
  const loadedEditId = useRef('');
  const editId = new URLSearchParams(location.search).get('edit');
  const pageTitle = view === 'catalog'
    ? 'Catálogo de productos'
    : view === 'form' ? 'Agregar producto' : 'Gestión de productos';

  useEffect(() => {
    if (view !== 'form' || editId) return;
    loadedEditId.current = '';
    setForm(emptyForm);
    setEditingId('');
    setImageFile(null);
    setMessage('');
    setError('');
  }, [view, editId]);

  useEffect(() => {
    if (view !== 'form' || !editId || loadedEditId.current === editId) return;
    const product = products.find((item) => String(item.id) === editId);
    if (!product) return;
    loadedEditId.current = editId;
    setEditingId(String(product.id));
    setForm({
      title: product.title,
      description: product.description,
      price: String(product.price),
      image: product.image,
      category: product.category,
    });
    setImageFile(null);
    setMessage('');
    setError('');
  }, [view, editId, products]);

  useEffect(() => {
    if (!imageFile) {
      setImagePreview('');
      return undefined;
    }
    const preview = typeof URL.createObjectURL === 'function'
      ? URL.createObjectURL(imageFile)
      : '';
    setImagePreview(preview);
    return () => {
      if (preview) URL.revokeObjectURL(preview);
    };
  }, [imageFile]);

  // Búsqueda administrativa por nombre y categoría.
  const filteredProducts = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('es');
    return query ? products.filter((product) =>
      `${product.title} ${product.category}`.toLocaleLowerCase('es').includes(query),
    ) : products;
  }, [products, search]);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
    if (event.target.name === 'image') {
      setImageFile(null);
      if (imageInput.current) imageInput.current.value = '';
    }
  }

  function resetForm() {
    setForm(emptyForm);
    setEditingId('');
    setError('');
    setImageFile(null);
    if (imageInput.current) imageInput.current.value = '';
  }

  // Sube la foto elegida antes de guardar el producto en la API.
  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return;
    setError('');
    setMessage('');
    setSubmitting(true);
    try {
      const image = imageFile ? await uploadProductImage(imageFile) : form.image;
      const product = { ...form, image };
      const wasEditing = Boolean(editingId);
      if (wasEditing) {
        await updateProduct(editingId, product);
        setMessage('Producto actualizado.');
      } else {
        await createProduct(product);
        setMessage('Producto creado.');
      }
      resetForm();
      if (wasEditing && view === 'form') {
        navigate('/admin-agregar-producto.html', { replace: true });
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo guardar el producto.');
    } finally {
      setSubmitting(false);
    }
  }

  // Carga un producto existente en el formulario para editarlo.
  function editProduct(product) {
    if (view === 'catalog') {
      navigate(`/admin-agregar-producto.html?edit=${encodeURIComponent(product.id)}`);
      return;
    }
    setEditingId(product.id);
    setForm({
      title: product.title,
      description: product.description,
      price: String(product.price),
      image: product.image,
      category: product.category,
    });
    setImageFile(null);
    if (imageInput.current) imageInput.current.value = '';
    setMessage('');
    setError('');
    const formElement = document.getElementById('product-admin-form');
    if (typeof formElement?.scrollIntoView === 'function') {
      formElement.scrollIntoView({ behavior: 'smooth' });
    }
  }

  function cancelEditing() {
    resetForm();
    if (view === 'form') {
      navigate('/admin-agregar-producto.html', { replace: true });
    }
  }

  // Envía DELETE para eliminar el producto y espera el resultado antes de mostrar el aviso.
  async function removeProduct(id) {
    setError('');
    setMessage('');
    try {
      await deleteProduct(id);
      if (String(editingId) === String(id)) resetForm();
      setMessage('Producto eliminado.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo eliminar el producto.');
    }
  }

  // El formulario y el listado se muestran solo a la sesión administrativa.
  return (
    <main className="container py-4 react-page">
        {/* Resumen de administración y cantidad total de productos. */}
        <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
          <div>
            <p className="text-uppercase text-secondary fw-semibold mb-1">Administración</p>
            <h1 className="h2 mb-0">{pageTitle}</h1>
          </div>
          <span className="badge text-bg-dark">{products.length} productos</span>
        </div>

        {!authorized ? (
          <div className="alert alert-warning" role="alert">
            <h2 className="h5">Acceso restringido</h2>
            <p className="mb-2">Inicia sesión como administrador desde la tienda para gestionar el catálogo.</p>
            <a className="btn btn-primary" href="/">Ir a la tienda</a>
          </div>
        ) : (
          <>
            {/* Formulario reutilizado para crear un producto o editarlo. */}
            {view !== 'catalog' && (
            <section className="card p-3 p-md-4 mb-4" aria-labelledby="product-admin-heading">
              <h2 id="product-admin-heading" className="h4">
                {editingId ? 'Editar producto' : 'Agregar producto'}
              </h2>
              <form id="product-admin-form" className="row g-3" onSubmit={handleSubmit}>
                <div className="col-12 col-md-6">
                  <label className="form-label" htmlFor="product-title">Nombre</label>
                  <input className="form-control" id="product-title" name="title" value={form.title} onChange={updateField} minLength={2} maxLength={120} required />
                </div>
                <div className="col-12 col-sm-6 col-md-3">
                  <label className="form-label" htmlFor="product-price">Precio (CLP)</label>
                  <input className="form-control" id="product-price" name="price" type="number" min="1" step="1" value={form.price} onChange={updateField} required />
                </div>
                <div className="col-12 col-sm-6 col-md-3">
                  <label className="form-label" htmlFor="product-category">Categoría</label>
                  <select className="form-select" id="product-category" name="category" value={form.category} onChange={updateField} required>
                    {categories.map((category) => <option key={category}>{category}</option>)}
                  </select>
                </div>
                <div className="col-12">
                  <label className="form-label" htmlFor="product-description">Descripción</label>
                  <textarea className="form-control" id="product-description" name="description" rows="3" maxLength={2000} value={form.description} onChange={updateField} required />
                </div>
                <div className="col-12">
                  <label className="form-label" htmlFor="product-image">Ruta de imagen o URL</label>
                  <input className="form-control" id="product-image" name="image" type="text" value={form.image} onChange={updateField} maxLength={500} placeholder="/images/accesorios/producto.webp" required={!imageFile} />
                  <label className="form-label mt-3" htmlFor="product-image-file">O selecciona una imagen desde tu computador</label>
                  <input
                    className="form-control"
                    id="product-image-file"
                    ref={imageInput}
                    type="file"
                    accept="image/png,image/jpeg,image/gif"
                    aria-describedby="product-image-help"
                    disabled={submitting}
                    onChange={(event) => {
                      const file = event.target.files?.[0] ?? null;
                      setError('');
                      if (file && !['image/png', 'image/jpeg', 'image/gif'].includes(file.type)) {
                        setImageFile(null);
                        event.target.value = '';
                        setError('Selecciona una imagen PNG, JPG o GIF.');
                        return;
                      }
                      if (file && file.size > 5 * 1024 * 1024) {
                        setImageFile(null);
                        event.target.value = '';
                        setError('La imagen no puede superar los 5 MB.');
                        return;
                      }
                      setImageFile(file);
                    }}
                  />
                  <div id="product-image-help" className="form-text">PNG, JPG o GIF; máximo 5 MB. Se guardará en el servidor, fuera de la carpeta del proyecto.</div>
                  {imageFile && <p className="form-text mb-0" role="status">Se subirá: {imageFile.name}</p>}
                  {(imagePreview || (form.image && !imageFile)) && (
                    <img
                      className="img-thumbnail mt-2"
                      src={imagePreview || getProductImageUrl(form.image)}
                      alt="Vista previa de la imagen del producto"
                      style={{ maxWidth: '180px', maxHeight: '180px', objectFit: 'contain' }}
                    />
                  )}
                </div>
                {error && <div className="col-12"><div className="alert alert-danger mb-0" role="alert">{error}</div></div>}
                {message && <div className="col-12"><div className="alert alert-success mb-0" role="status">{message}</div></div>}
                <div className="col-12 d-flex gap-2">
                  <button className="btn btn-primary" type="submit" disabled={submitting}>{submitting ? 'Guardando...' : editingId ? 'Guardar cambios' : 'Crear producto'}</button>
                  {editingId && <button className="btn btn-outline-secondary" type="button" onClick={cancelEditing} disabled={submitting}>Cancelar edición</button>}
                </div>
              </form>
            </section>
            )}

            {/* Listado filtrable con acciones para modificar o quitar productos. */}
            {view !== 'form' && (
            <section aria-labelledby="product-list-heading">
              <div className="d-flex flex-wrap align-items-end justify-content-between gap-3 mb-3">
                <h2 id="product-list-heading" className="h4 mb-0">Catálogo</h2>
                <div>
                  <label className="form-label visually-hidden" htmlFor="admin-product-search">Buscar producto en el catálogo</label>
                  <input className="form-control" id="admin-product-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar producto..." />
                </div>
              </div>
              {filteredProducts.length === 0 ? (
                <p className="alert alert-info" role="status">No hay productos para mostrar.</p>
              ) : (
                <div className="row row-cols-1 row-cols-sm-2 row-cols-lg-3 row-cols-xl-4 g-4">
                  {filteredProducts.map((product) => (
                    <div className="col" key={product.id}>
                      <div className="card h-100 react-product-card">
                        <img className="card-img-top react-product-image" src={getProductImageUrl(product.image)} alt={product.title} loading="lazy" />
                        <div className="card-body d-flex flex-column">
                          <span className="badge text-bg-secondary align-self-start mb-2">{product.category}</span>
                          <h3 className="card-title h5">{product.title}</h3>
                          <p className="card-text text-body-secondary flex-grow-1">{product.description}</p>
                          <div className="d-flex gap-2 mt-auto">
                            <button className="btn btn-outline-primary flex-grow-1" type="button" aria-label={`Editar ${product.title}`} onClick={() => editProduct(product)}>Editar</button>
                            <button className="btn btn-outline-danger" type="button" aria-label={`Eliminar ${product.title}`} onClick={() => removeProduct(product.id)}>Eliminar</button>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
            )}
          </>
        )}
    </main>
  );
}
