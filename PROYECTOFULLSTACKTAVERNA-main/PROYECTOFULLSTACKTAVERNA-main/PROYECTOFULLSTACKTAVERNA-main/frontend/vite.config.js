// Configuración de Vite, React y el entorno de pruebas con Vitest.
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Redirige las llamadas React que comienzan con /api hacia Spring Boot.
  server: {
    proxy: {
      '/api': 'http://localhost:8082',
    },
  },
  // jsdom permite probar componentes como si estuvieran en el navegador.
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.js'],
    restoreMocks: true,
  },
});
