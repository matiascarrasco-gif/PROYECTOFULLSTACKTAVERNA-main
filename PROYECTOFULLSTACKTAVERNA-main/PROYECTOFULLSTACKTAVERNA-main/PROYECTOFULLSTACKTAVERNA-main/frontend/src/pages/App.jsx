// Enrutador principal: conecta cada URL con su página React correspondiente.
import React, { useEffect, useState, useSyncExternalStore } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import SiteLayout from '../components/SiteLayout.jsx';
import ProductCatalogPage from './ProductCatalogPage.jsx';
import ProductDetailPage from './ProductDetailPage.jsx';
import AdminProductsPage from './AdminProductsPage.jsx';
import HomePage from './HomePage.jsx';
import BlogPage from './BlogPage.jsx';
import TechnicalServicePage from './TechnicalServicePage.jsx';
import CartPage from './CartPage.jsx';
import PurchaseResultPage from './PurchaseResultPage.jsx';
import SearchResultsPage from './SearchResultsPage.jsx';
import {
  getProductLoadError,
  initializeProductDatabase,
  subscribeProducts,
} from '../data/apiProducts.js';
import {
  getAuthState,
  restoreSession,
  subscribeAuth,
} from '../data/auth.js';
import { getSeedProducts } from '../data/seedProducts.js';

const catalogs = {
  '/accesorios.html': { title: 'Accesorios', category: 'Accesorios' },
  '/consolas.html': { title: 'Consolas', category: 'Consolas' },
  '/juegos.html': { title: 'Juegos Digitales', category: 'Juegos' },
  '/mouse-mousepad.html': { title: 'Mouse / Mousepad', category: 'Mouse / Mousepad' },
  '/pcgamers.html': { title: 'PC Gamers', category: 'PC Gamers' },
  '/poleras.html': { title: 'Poleras Personalizadas', category: 'Poleras Personalizadas' },
  '/poleron.html': { title: 'Polerones Gamers Personalizados', category: 'Polerones Gamers Personalizados' },
  '/sillasgamers.html': { title: 'Sillas Gamers', category: 'Sillas Gamers' },
};
// El seed se comparte al inicializar el catálogo simulado.
const seedProducts = getSeedProducts();

export default function App() {
  const { pathname } = useLocation();
  const path = pathname.toLowerCase();
  const authState = useSyncExternalStore(subscribeAuth, getAuthState, getAuthState);
  const [sessionError, setSessionError] = useState('');
  const productLoadError = useSyncExternalStore(
    subscribeProducts,
    getProductLoadError,
    getProductLoadError,
  );
  // useEffect carga el catálogo una sola vez después de montar App.
  // El arreglo vacío indica que no debe repetirse en actualizaciones posteriores.
  useEffect(() => {
    function showAuthError(event) {
      setSessionError(event.detail?.message || 'No se pudo validar la sesión.');
    }

    initializeProductDatabase(seedProducts);
    window.addEventListener('taverna:auth-error', showAuthError);
    restoreSession().catch((error) => {
      setSessionError(error instanceof Error ? error.message : 'No se pudo validar la sesión.');
    });
    return () => window.removeEventListener('taverna:auth-error', showAuthError);
  }, []);

  return (
    <SiteLayout>
      {productLoadError && (
        <div className="container pt-3">
          <div className="alert alert-warning mb-0" role="status">
            {productLoadError} Se muestran productos de respaldo; los cambios requieren que la API esté disponible.
          </div>
        </div>
      )}
      {sessionError && (
        <div className="container pt-3">
          <div className="alert alert-warning mb-0" role="status">
            No se pudo validar la sesión con el servidor: {sessionError}
          </div>
        </div>
      )}
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/index.html" element={<HomePage />} />
        <Route path="/blog.html" element={<BlogPage />} />
        <Route path="/serviciotecnico.html" element={<TechnicalServicePage />} />
        <Route path="/buscar.html" element={<SearchResultsPage />} />
        <Route path="/carrito.html" element={<CartPage />} />
        <Route path="/compra-realizada.html" element={<PurchaseResultPage success />} />
        <Route path="/pago-fallido.html" element={<PurchaseResultPage success={false} />} />
        {Object.entries(catalogs).map(([route, config]) => (
          <Route key={route} path={route} element={<ProductCatalogPage key={route} {...config} />} />
        ))}
        <Route path="/producto.html" element={<ProductDetailPage />} />
        <Route path="/admin-panel.html" element={<Navigate to="/admin-catalogo.html" replace />} />
        <Route path="/admin-catalogo.html" element={<AdminProductsPage view="catalog" />} />
        <Route path="/admin-agregar-producto.html" element={<AdminProductsPage view="form" />} />
        <Route path="*" element={
          <main className="container py-5">
            <section className="card p-4">
              <h1 className="h3">Página no encontrada</h1>
              <p>La página solicitada no existe.</p>
              <a href="/">Volver al inicio</a>
            </section>
          </main>
        } />
      </Routes>
    </SiteLayout>
  );
}
