// Carrito y checkout: datos del cliente, dirección, totales y pago simulado.
import React, { useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import { formatPrice } from '../components/ProductCard.jsx';
import { apiClient, getAuthState, subscribeAuth, updateAuthenticatedUser } from '../data/auth.js';
import { createOrderNumber, formatItemPrice, getRutValidationError, readCart, validateCheckout } from '../data/cart.js';
import { getProductImageUrl } from '../data/apiProducts.js';
import PurchaseResultPage from './PurchaseResultPage.jsx';

const orderStorageKey = 'taverna:last-order';

function emptyCustomer(user) {
  // Precarga la cuenta activa y deja vacíos los datos propios de la entrega.
  const surnames = String(user?.apellidos || '').trim().split(/\s+/).filter(Boolean);
  return {
    nombres: user?.nombres || '',
    apellidoPaterno: user?.apellidoPaterno || surnames[0] || '',
    apellidoMaterno: user?.apellidoMaterno || surnames.slice(1).join(' '),
    correo: user?.correo || '',
    rut: user?.rut || '',
    calle: '',
    departamento: '',
    region: '',
    comuna: '',
    indicaciones: '',
  };
}

export default function CartPage() {
  const authState = useSyncExternalStore(subscribeAuth, getAuthState, getAuthState);
  const { user, ready } = authState;
  const [cart, setCart] = useState(readCart);
  const [customer, setCustomer] = useState(() => emptyCustomer(user));
  const [purchaseError, setPurchaseError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [purchaseResult, setPurchaseResult] = useState(null);
  const orderId = useRef(window.crypto.randomUUID());
  const rutError = getRutValidationError(customer.rut);

  // Mantiene los datos de cuenta actualizados y conserva la dirección de entrega.
  useEffect(() => {
    setCustomer((current) => ({
      ...current,
      ...emptyCustomer(user),
      calle: current.calle,
      departamento: current.departamento,
      region: current.region,
      comuna: current.comuna,
      indicaciones: current.indicaciones,
    }));
  }, [user]);

  const subtotal = useMemo(() => cart.reduce(
    (sum, item) => sum + formatItemPrice(item) * (Number(item.cantidad) || 0),
    0,
  ), [cart]);
  const discount = user?.isDuoc ? subtotal * 0.2 : 0;
  const total = subtotal - discount;

  // Actualiza unidades y refleja el cambio tanto en pantalla como en localStorage.
  function changeQuantity(itemId, change) {
    const nextCart = cart
      .map((item) => item.id === itemId
        ? { ...item, cantidad: Math.max(0, (Number(item.cantidad) || 0) + change) }
        : item)
      .filter((item) => item.cantidad > 0);
    localStorage.setItem('carrito', JSON.stringify(nextCart));
    setCart(nextCart);
    window.dispatchEvent(new Event('taverna:cart-updated'));
  }

  function updateCustomer(event) {
    const { name, value } = event.target;
    setCustomer((current) => ({ ...current, [name]: value }));
  }

  // Genera una orden de demostración y envía al resultado según validaciones.
  async function pay(event) {
    event.preventDefault();
    const errors = validateCheckout(customer, cart);
    const orderCustomer = {
      ...customer,
      apellidos: `${customer.apellidoPaterno} ${customer.apellidoMaterno}`.trim(),
    };

    if (errors.length > 0) {
      localStorage.setItem(orderStorageKey, JSON.stringify({
        number: createOrderNumber(),
        customer: orderCustomer,
        products: cart,
        total,
        errors,
        status: 'failed',
        createdAt: new Date().toISOString(),
      }));
      setPurchaseResult(JSON.parse(localStorage.getItem(orderStorageKey)));
      return;
    }

    setSubmitting(true);
    setPurchaseError('');
    try {
      const response = await apiClient.post('/orders', {
        orderId: orderId.current,
        items: cart.map((item) => ({
          productId: Number(item.productId ?? item.id),
          quantity: Number(item.cantidad),
          ...(item.redemptionId ? { redemptionId: item.redemptionId } : {}),
        })),
      });
      const purchase = response.data;
      updateAuthenticatedUser(purchase.user);
      const order = {
        number: createOrderNumber(),
        orderId: purchase.orderId,
        customer: orderCustomer,
        products: cart,
        subtotal: purchase.subtotal,
        discount: purchase.discount,
        total: purchase.total,
        pointsEarned: purchase.pointsEarned,
        puntosSaldo: purchase.puntosSaldo,
        errors: [],
        status: 'success',
        createdAt: new Date().toISOString(),
      };
      localStorage.setItem(orderStorageKey, JSON.stringify(order));
      // El carrito se vacía solo después de que el backend confirma la compra.
      localStorage.setItem('carrito', '[]');
      window.dispatchEvent(new Event('taverna:cart-updated'));
      orderId.current = window.crypto.randomUUID();
      setCart([]);
      setPurchaseResult(order);
    } catch (error) {
      const body = error?.response?.data;
      const message = body?.detail || body?.message || body?.error
        || error?.message || 'No se pudo registrar la compra.';
      const incidentId = body?.properties?.incidentId || body?.incidentId;
      setPurchaseError(incidentId && !message.includes(incidentId)
        ? `${message} Referencia: ${incidentId}`
        : message);
    } finally {
      setSubmitting(false);
    }
  }

  if (!ready) {
    return (
      <main className="contenedor react-page">
        <section className="tarjeta checkout-login-required" role="status">
          <h1>Comprobando tu sesión</h1>
          <p>Un momento, estamos validando tu acceso con el servidor.</p>
        </section>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="contenedor react-page">
          <section className="tarjeta checkout-login-required" role="status">
            <h1>Inicia sesión para acceder al carrito</h1>
            <p>Tu carrito y el proceso de compra están disponibles para clientes con sesión iniciada.</p>
            <a className="checkout-primary-link" href="/">Volver a la tienda e iniciar sesión</a>
          </section>
      </main>
    );
  }

  if (purchaseResult) {
    return <PurchaseResultPage success={purchaseResult.status === 'success'} order={purchaseResult} />;
  }

  return (
    <main className="contenedor react-page checkout-page">
        {/* Resumen del pedido y controles para ajustar unidades. */}
        <section className="tarjeta checkout-title">
          <p className="catalog-eyebrow">La Taverna Gaming</p>
          <h1>Carrito de compras</h1>
          <p>Revisa tus productos y completa los datos de entrega para continuar.</p>
        </section>

        <section className="tarjeta checkout-products" aria-labelledby="checkout-products-title">
          <h2 id="checkout-products-title">Tus productos</h2>
          {cart.length === 0 ? (
            <p role="status">Tu carrito está vacío. Explora la tienda para agregar productos.</p>
          ) : (
            <div className="checkout-product-list">
              {cart.map((item) => (
                <article className="checkout-product" key={item.id}>
                  <img src={getProductImageUrl(item.imagen || item.image)} alt={item.titulo || item.title} />
                  <div className="checkout-product-info">
                    <h3>{item.titulo || item.title}</h3>
                    <p>{item.seccion || item.category}</p>
                    <strong>{formatPrice(formatItemPrice(item))}</strong>
                  </div>
                  <div className="checkout-quantity">
                    <button type="button" aria-label={`Quitar una unidad de ${item.titulo || item.title}`} onClick={() => changeQuantity(item.id, -1)}>−</button>
                    <span aria-label="Cantidad">{item.cantidad}</span>
                    <button type="button" aria-label={`Agregar una unidad de ${item.titulo || item.title}`} disabled={Boolean(item.redemptionId)} onClick={() => changeQuantity(item.id, 1)}>+</button>
                  </div>
                  <strong className="checkout-line-total">
                    {formatPrice(formatItemPrice(item) * (Number(item.cantidad) || 0))}
                  </strong>
                </article>
              ))}
            </div>
          )}
          {discount > 0 && <p className="checkout-discount">Descuento Duoc (20%): −{formatPrice(discount)}</p>}
          <div className="checkout-total"><span>Total</span><strong>{formatPrice(total)}</strong></div>
        </section>

        {/* Datos de cliente y dirección usados para validar el pago simulado. */}
        <form className="tarjeta checkout-form" onSubmit={pay} noValidate>
          {purchaseError && <p className="shop-auth-error" role="alert">{purchaseError}</p>}
          <h2>Información del cliente</h2>
          <div className="checkout-fields">
            <label>Nombres *
              <input name="nombres" autoComplete="given-name" value={customer.nombres} onChange={updateCustomer} />
            </label>
            <label>Apellido paterno *
              <input name="apellidoPaterno" autoComplete="family-name" value={customer.apellidoPaterno} onChange={updateCustomer} />
            </label>
            <label>Apellido materno *
              <input name="apellidoMaterno" value={customer.apellidoMaterno} onChange={updateCustomer} />
            </label>
            <label>Correo electrónico *
              <input name="correo" type="email" autoComplete="email" value={customer.correo} onChange={updateCustomer} />
            </label>
            <label>RUT
              <input name="rut" value={customer.rut} onChange={updateCustomer} aria-describedby="checkout-rut-error" />
              {rutError && <small id="checkout-rut-error" className="shop-field-error" role="alert">{rutError}</small>}
            </label>
          </div>

          <h2>Dirección de entrega</h2>
          <div className="checkout-fields">
            <label className="checkout-field-wide">Calle y número *
              <input name="calle" autoComplete="street-address" value={customer.calle} onChange={updateCustomer} />
            </label>
            <label>Departamento (opcional)
              <input name="departamento" value={customer.departamento} onChange={updateCustomer} />
            </label>
            <label>Región *
              <input name="region" autoComplete="address-level1" value={customer.region} onChange={updateCustomer} />
            </label>
            <label>Comuna *
              <input name="comuna" autoComplete="address-level2" value={customer.comuna} onChange={updateCustomer} />
            </label>
            <label className="checkout-field-wide">Indicaciones de entrega (opcional)
              <textarea name="indicaciones" rows="3" value={customer.indicaciones} onChange={updateCustomer} />
            </label>
          </div>
          <p className="checkout-demo-note">Compra simulada: se registrará el pedido y se calcularán tus Puntos LevelUp, pero no se realizará ningún cobro real.</p>
          <button className="checkout-pay-button" type="submit" disabled={cart.length === 0 || submitting}>
            {submitting ? 'Finalizando compra…' : 'Finalizar compra simulada'}
          </button>
        </form>
    </main>
  );
}
