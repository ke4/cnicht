import { defineConfig } from "cypress";

export default defineConfig({
  allowCypressEnv: false,
  component: {
    devServer: {
      framework: "react",
      bundler: "vite",
    },
    supportFile: "cypress/support/component.tsx",
    indexHtmlFile: "cypress/support/component-index.html",
  },
});
