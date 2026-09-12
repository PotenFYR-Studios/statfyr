import { StrictMode } from "react";
import { hydrateRoot, createRoot } from "react-dom/client";
import App from "./App";
import "./styles.css";

/* Production pages ship prerendered markup (scripts/prerender.ts): hydrate
   it. Dev serves an empty root, so fall back to a plain client render. */
const rootEl = document.getElementById("root")!;
const tree = (
  <StrictMode>
    <App />
  </StrictMode>
);

if (rootEl.hasChildNodes()) {
  hydrateRoot(rootEl, tree);
} else {
  createRoot(rootEl).render(tree);
}
