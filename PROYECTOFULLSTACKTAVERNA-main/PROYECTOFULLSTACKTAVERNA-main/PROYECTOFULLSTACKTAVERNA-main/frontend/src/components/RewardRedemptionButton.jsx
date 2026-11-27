import React, { useState, useSyncExternalStore } from 'react';
import { getAuthState, subscribeAuth } from '../data/auth.js';
import { requestLoginForProduct } from '../data/cart.js';
import { redeemProduct } from '../data/rewards.js';

export default function RewardRedemptionButton({ product, className = '' }) {
  const { user, ready } = useSyncExternalStore(subscribeAuth, getAuthState, getAuthState);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const pointsCost = Number(product.pointsCost);

  if (!Number.isInteger(pointsCost) || pointsCost <= 0) return null;

  async function handleRedeem() {
    setMessage('');
    if (!ready) return;
    if (!user) {
      requestLoginForProduct(null, 'redeem');
      return;
    }

    setBusy(true);
    try {
      await redeemProduct(product);
      setMessage(`Canje realizado por ${pointsCost} puntos. Se agregó al carrito.`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'No se pudo canjear el producto.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <span className="levelup-redemption">
      <button
        className={`levelup-redeem-button ${className}`.trim()}
        type="button"
        disabled={!ready || busy}
        onClick={handleRedeem}
      >
        {busy ? 'Procesando canje…' : `🎁 Canjear (${pointsCost} pts)`}
      </button>
      {message && <small className="levelup-redemption-message" role="status">{message}</small>}
    </span>
  );
}
