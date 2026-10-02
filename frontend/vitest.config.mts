import { defineConfig } from "vitest/config"

/**
 * Component tests only. Next.js server components that await data cannot be rendered here, so
 * those routes stay covered by the end-to-end walk rather than by this suite.
 *
 * `@vitejs/plugin-react` is deliberately absent: it only adds Fast Refresh, which tests do not
 * want, and its current release pulls a Babel toolchain that conflicts with the shadcn
 * dependency tree. Vitest transforms TSX through esbuild, which picks up `"jsx": "react-jsx"`
 * from tsconfig.json, so there is no JSX setting to keep in sync here.
 */
export default defineConfig({
  // Resolves the "@/*" aliases from tsconfig.json natively.
  resolve: { tsconfigPaths: true },
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.{ts,tsx}"],
    setupFiles: ["src/test-setup.ts"],
    restoreMocks: true,
  },
})