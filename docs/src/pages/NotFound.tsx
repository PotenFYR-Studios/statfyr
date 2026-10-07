import { useEffect } from "react";
import { Link, applyMeta } from "../lib/router";

export default function NotFound() {
  useEffect(() => {
    applyMeta({
      title: "Not Found · Statfyr",
      description: "That page doesn't exist.",
      path: "/404",
    });
  }, []);

  return (
    <div className="mx-auto flex min-h-[50vh] max-w-3xl flex-col items-center justify-center px-6 text-center">
      <p className="mono-label">404</p>
      <h1 className="mt-3 text-[clamp(1.8rem,4vw,2.6rem)] font-extrabold text-white">
        This route returned 404
      </h1>
      <p className="mt-4 max-w-md text-sm text-[#9aa0b4]">
        The page you asked for doesn't exist, and unlike a statfyr endpoint, it can't be resolved.
      </p>
      <div className="mt-8 flex gap-3">
        <Link className="btn btn-primary btn-sm" to="/">Back home</Link>
        <Link className="btn btn-ghost btn-sm" to="/docs">Open the docs</Link>
      </div>
      <p className="mt-10 max-w-md text-sm text-[#9aa0b4]">
        Trying to reach the API itself? The plugin serves JSON, not pages; point your client at
        <code className="mx-1 rounded bg-white/5 px-1.5 py-0.5 font-mono text-xs">/api</code>on
        your server's statfyr port instead. Useful places to land:
      </p>
      <ul className="mt-4 grid max-w-md list-disc gap-1.5 text-left text-sm text-[#9aa0b4]">
        <li><Link className="underline decoration-line-light hover:text-white" to="/docs/api">API reference</Link>: every endpoint, parameter, and error shape</li>
        <li><Link className="underline decoration-line-light hover:text-white" to="/examples">Examples</Link>: a hardened config.yml and curl walkthroughs</li>
        <li><Link className="underline decoration-line-light hover:text-white" to="/docs/configuration">Configuration</Link>: every config.yml key and default</li>
        <li><Link className="underline decoration-line-light hover:text-white" to="/about">About</Link>: what statfyr is and who builds it</li>
      </ul>
    </div>
  );
}
