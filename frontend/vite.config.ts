import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'

// https://vite.dev/config/
// The Gradle `buildFrontend` NpmTask sets BUILD_OUTPUT_DIR so that the
// compiled React assets land in `backend/src/main/resources/static`,
// allowing Spring Boot to serve the SPA directly from the jar.
const outDir =
  process.env.BUILD_OUTPUT_DIR ??
  resolve(__dirname, '../backend/src/main/resources/static')

export default defineConfig({
  plugins: [react()],
  build: {
    outDir,
    emptyOutDir: true,
  },
})
