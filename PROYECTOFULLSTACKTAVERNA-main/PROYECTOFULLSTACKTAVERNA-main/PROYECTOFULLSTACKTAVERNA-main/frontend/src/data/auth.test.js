import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
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

import {
  ACCESS_TOKEN_EXPIRES_KEY,
  ACCESS_TOKEN_KEY,
  getAuthState,
  login,
  logout,
  restoreSession,
} from './auth.js';

const user = { id: 'customer-1', correo: 'cliente@example.com', role: 'CUSTOMER' };

function authResponse() {
  return {
    status: 200,
    data: {
      accessToken: 'signed-test-token',
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      user,
    },
  };
}

describe('auth session', () => {
  beforeEach(async () => {
    localStorage.clear();
    await logout();
    api.get.mockReset();
    api.post.mockReset();
    api.put.mockReset();
  });

  it('stores the access token and validated user returned by login', async () => {
    api.post.mockResolvedValueOnce(authResponse());

    await login({ correo: user.correo, password: 'secure-password' });

    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('signed-test-token');
    expect(Date.parse(localStorage.getItem(ACCESS_TOKEN_EXPIRES_KEY))).toBeGreaterThan(Date.now());
    expect(getAuthState()).toEqual({ user, ready: true });
  });

  it('restores a persisted session only after validating it with the API', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'persisted-token');
    localStorage.setItem(ACCESS_TOKEN_EXPIRES_KEY, new Date(Date.now() + 60_000).toISOString());
    api.get.mockResolvedValueOnce({ status: 200, data: user });

    await expect(restoreSession()).resolves.toEqual(user);

    expect(api.get).toHaveBeenCalledWith('/auth/me');
    expect(getAuthState()).toEqual({ user, ready: true });
  });

  it('clears an expired token without trusting cached account data', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'expired-token');
    localStorage.setItem(ACCESS_TOKEN_EXPIRES_KEY, new Date(Date.now() - 1_000).toISOString());
    localStorage.setItem('usuarioActivo', JSON.stringify(user));

    await expect(restoreSession()).resolves.toBeNull();

    expect(api.get).not.toHaveBeenCalled();
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
    expect(getAuthState()).toEqual({ user: null, ready: true });
  });

  it('clears the session immediately when the access token expires', async () => {
    vi.useFakeTimers();
    try {
      const response = authResponse();
      response.data.expiresAt = new Date(Date.now() + 5_000).toISOString();
      api.post.mockResolvedValueOnce(response);
      await login({ correo: user.correo, password: 'secure-password' });

      await vi.advanceTimersByTimeAsync(5_000);

      expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
      expect(getAuthState()).toEqual({ user: null, ready: true });
    } finally {
      vi.useRealTimers();
    }
  });

  it('clears the session when the backend rejects the persisted token', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'revoked-token');
    localStorage.setItem(ACCESS_TOKEN_EXPIRES_KEY, new Date(Date.now() + 60_000).toISOString());
    api.get.mockRejectedValueOnce({ response: { status: 401 } });

    await expect(restoreSession()).resolves.toBeNull();

    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
    expect(getAuthState()).toEqual({ user: null, ready: true });
  });

  it('clears local session state after server logout', async () => {
    api.post
      .mockResolvedValueOnce(authResponse())
      .mockResolvedValueOnce({ status: 204, data: null });
    await login({ correo: user.correo, password: 'secure-password' });

    await logout();

    expect(api.post).toHaveBeenLastCalledWith('/auth/logout');
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(ACCESS_TOKEN_EXPIRES_KEY)).toBeNull();
    expect(getAuthState()).toEqual({ user: null, ready: true });
  });
});
