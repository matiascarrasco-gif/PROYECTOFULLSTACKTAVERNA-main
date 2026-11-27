import axios from 'axios';

const API_BASE = import.meta.env?.VITE_API_URL || '/api';
export const ACCESS_TOKEN_KEY = 'taverna_access_token';
export const ACCESS_TOKEN_EXPIRES_KEY = 'taverna_access_token_expires_at';
const USER_KEY = 'usuarioActivo';
const listeners = new Set();
let restoreRequest;
let expiryTimer;

let authState = {
  user: null,
  ready: !localStorage.getItem(ACCESS_TOKEN_KEY),
};

function publish(user, ready = true) {
  authState = { user, ready };
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  else localStorage.removeItem(USER_KEY);
  listeners.forEach((listener) => listener());
}

function clearStoredSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(ACCESS_TOKEN_EXPIRES_KEY);
  window.clearTimeout(expiryTimer);
  expiryTimer = undefined;
}

function scheduleExpiry(expiresAt) {
  window.clearTimeout(expiryTimer);
  if (!expiresAt) return;
  const remaining = new Date(expiresAt).getTime() - Date.now();
  if (!Number.isFinite(remaining) || remaining <= 0) {
    clearStoredSession();
    publish(null);
    return;
  }
  expiryTimer = window.setTimeout(() => {
    clearStoredSession();
    publish(null);
  }, remaining);
}

function getAuthError(error) {
  const body = error?.response?.data;
  const message = body?.detail || body?.message || body?.error || error?.message;
  return new Error(typeof message === 'string' && message
    ? message
    : 'No se pudo completar la operación de cuenta.');
}

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || '';
    if (error.response?.status === 401 && !url.endsWith('/auth/login') && !url.endsWith('/auth/register')) {
      clearStoredSession();
      publish(null);
    }
    return Promise.reject(error);
  },
);

export function getAuthState() {
  return authState;
}

export function subscribeAuth(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

function applyAuthResponse(response) {
  localStorage.setItem(ACCESS_TOKEN_KEY, response.data.accessToken);
  localStorage.setItem(ACCESS_TOKEN_EXPIRES_KEY, response.data.expiresAt);
  scheduleExpiry(response.data.expiresAt);
  publish(response.data.user);
  return response.data.user;
}

export async function login(credentials) {
  try {
    return applyAuthResponse(await apiClient.post('/auth/login', credentials));
  } catch (error) {
    throw getAuthError(error);
  }
}

export async function registerAccount(account) {
  try {
    return applyAuthResponse(await apiClient.post('/auth/register', account));
  } catch (error) {
    throw getAuthError(error);
  }
}

export async function registerAdminAccount(account, invitationKey) {
  try {
    return applyAuthResponse(await apiClient.post('/auth/admin/register', account, {
      headers: { 'X-Admin-Registration-Key': invitationKey },
    }));
  } catch (error) {
    throw getAuthError(error);
  }
}

export async function updateAccount(profile) {
  try {
    const response = await apiClient.put('/auth/me', profile);
    publish(response.data);
    return response.data;
  } catch (error) {
    throw getAuthError(error);
  }
}

export async function restoreSession() {
  // Al recargar, confirma con el backend que el token guardado siga vigente.
  const token = getAccessToken();
  if (!token) {
    publish(null);
    return null;
  }
  const expiresAt = localStorage.getItem(ACCESS_TOKEN_EXPIRES_KEY);
  const expirationTimestamp = expiresAt ? new Date(expiresAt).getTime() : null;
  if (expiresAt && (!Number.isFinite(expirationTimestamp) || expirationTimestamp <= Date.now())) {
    clearStoredSession();
    publish(null);
    return null;
  }
  if (restoreRequest) return restoreRequest;

  authState = { ...authState, ready: false };
  listeners.forEach((listener) => listener());
  restoreRequest = apiClient.get('/auth/me')
    .then((response) => {
      scheduleExpiry(expiresAt);
      publish(response.data);
      return response.data;
    })
    .catch((error) => {
      if (error.response?.status === 401) {
        clearStoredSession();
        publish(null);
        return null;
      }
      publish(null);
      throw getAuthError(error);
    })
    .finally(() => {
      restoreRequest = undefined;
    });
  return restoreRequest;
}

export async function logout() {
  const token = getAccessToken();
  try {
    if (token) await apiClient.post('/auth/logout');
  } catch (error) {
    throw getAuthError(error);
  } finally {
    clearStoredSession();
    publish(null);
  }
}

export function getStoredUser() {
  return authState.user;
}

export function updateAuthenticatedUser(user) {
  if (!getAccessToken()) {
    throw new Error('No hay una sesión activa para actualizar.');
  }
  publish(user);
}

window.addEventListener('storage', (event) => {
  if (event.key === ACCESS_TOKEN_KEY && !event.newValue) {
    clearStoredSession();
    publish(null);
  } else if (event.key === ACCESS_TOKEN_KEY && event.newValue) {
    publish(null, false);
    restoreSession().catch((error) => {
      window.dispatchEvent(new CustomEvent('taverna:auth-error', {
        detail: { message: error instanceof Error ? error.message : 'No se pudo validar la sesión.' },
      }));
    });
  }
});
