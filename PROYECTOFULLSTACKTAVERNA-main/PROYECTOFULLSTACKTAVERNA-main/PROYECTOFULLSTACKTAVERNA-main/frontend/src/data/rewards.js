import { apiClient, updateAuthenticatedUser } from './auth.js';
import { addRedeemedProductToCart } from './cart.js';

export async function redeemProduct(product) {
  try {
    const response = await apiClient.post('/rewards/redemptions', { productId: product.id });
    updateAuthenticatedUser(response.data.user);
    addRedeemedProductToCart(product, response.data.redemptionId);
    return response.data;
  } catch (error) {
    const body = error?.response?.data;
    const message = body?.detail || body?.message || body?.error || error?.message;
    throw new Error(typeof message === 'string' && message
      ? message
      : 'No se pudo canjear el producto.');
  }
}
