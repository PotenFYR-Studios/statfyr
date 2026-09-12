import { useEffect, useState } from "react";
import { usePath } from "./lib/router";
import { Header, Footer, CommandPalette } from "./components/layout";
import Landing from "./pages/Landing";
import DocsPortal from "./pages/DocsPortal";
import GettingStarted from "./pages/GettingStarted";
import Configuration from "./pages/Configuration";
import ApiReference from "./pages/ApiReference";
import Examples from "./pages/Examples";
import ExamplesSetup from "./pages/ExamplesSetup";
import Faq from "./pages/Faq";
import About from "./pages/About";
import License from "./pages/License";
import NotFound from "./pages/NotFound";

export default function App() {
  const path = usePath();
  const [paletteOpen, setPaletteOpen] = useState(false);

  /* Scroll to top on navigation (unless it's a hash link). */
  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, [path]);

  /* ⌘K / Ctrl+K opens the palette. */
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setPaletteOpen((o) => !o);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  let page;
  switch (path) {
    case "/":
      page = <Landing />;
      break;
    case "/docs":
      page = <DocsPortal />;
      break;
    case "/docs/getting-started":
      page = <GettingStarted />;
      break;
    case "/docs/configuration":
      page = <Configuration />;
      break;
    case "/docs/api":
      page = <ApiReference />;
      break;
    case "/docs/examples":
      page = <Examples />;
      break;
    case "/docs/faq":
      page = <Faq />;
      break;
    case "/about":
      page = <About />;
      break;
    case "/license":
      page = <License />;
      break;
    case "/examples":
      page = <ExamplesSetup />;
      break;
    default:
      page = <NotFound />;
  }

  return (
    <div className="relative z-[1] flex min-h-screen flex-col">
      <Header onOpenPalette={() => setPaletteOpen(true)} />
      <main className="flex-1">{page}</main>
      <Footer />
      <CommandPalette open={paletteOpen} onClose={() => setPaletteOpen(false)} />
    </div>
  );
}
