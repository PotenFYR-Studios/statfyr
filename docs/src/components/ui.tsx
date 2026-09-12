import { useState } from "react";

/* ------------------------------------------------------------- CodeBlock */

export function CodeBlock({
  code,
  lang = "bash",
  filename,
}: {
  code: string;
  lang?: string;
  filename?: string;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code.trim());
      setCopied(true);
      setTimeout(() => setCopied(false), 1400);
    } catch {
      /* clipboard unavailable */
    }
  };

  return (
    <div className="relative my-4">
      {filename && (
        <div className="flex items-center justify-between rounded-t-[10px] border border-b-0 border-line bg-[#12152a] px-4 py-1.5 font-mono text-xs text-[#9aa0b4]">
          <span>{filename}</span>
          <span className="text-[10px] uppercase tracking-wider text-[#6a7089]">{lang}</span>
        </div>
      )}
      <pre data-lang={lang} className={filename ? "!mt-0 !rounded-t-none" : undefined}>
        <button
          type="button"
          onClick={handleCopy}
          className={copied ? "copy-btn ok" : "copy-btn"}
          aria-label="Copy code to clipboard"
        >
          {copied ? "Copied!" : "Copy"}
        </button>
        <code>{code.trim()}</code>
      </pre>
    </div>
  );
}

/* --------------------------------------------------------- MethodBadge */

export function MethodBadge({ method }: { method: string }) {
  return (
    <span className={`method-badge method-${method.toLowerCase()}`}>{method}</span>
  );
}

/* --------------------------------------------------------- EndpointCard */

export type EndpointParam = {
  name: string;
  type: string;
  def?: string;
  desc: string;
};

export function EndpointCard({
  method = "GET",
  path,
  title,
  description,
  auth,
  params,
  children,
}: {
  method?: string;
  path: string;
  title: string;
  description: string;
  auth?: string;
  params?: EndpointParam[];
  children?: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  return (
    <article className="doc-card !p-0 overflow-hidden">
      <header className="flex flex-wrap items-center gap-x-3 gap-y-2 border-b border-line-light bg-white/[0.02] px-5 py-3.5">
        <MethodBadge method={method} />
        <code className="font-mono text-[13.5px] text-[#d8ccfe]">{path}</code>
        {auth && <span className="chip ml-auto">{auth}</span>}
      </header>
      <div className="px-5 py-4">
        <h3 className="!mt-0 !text-[16.5px] !text-white">{title}</h3>
        <p className="!text-sm">{description}</p>

        {params && params.length > 0 && (
          <>
            <button
              type="button"
              onClick={() => setOpen(!open)}
              aria-expanded={open}
              className="mono-label mt-3 flex cursor-pointer items-center gap-1.5 !text-[#a78bfa] hover:!text-white"
            >
              <span
                aria-hidden
                className="inline-block transition-transform"
                style={{ transform: open ? "rotate(90deg)" : "none" }}
              >
                ▸
              </span>
              {open ? "Hide" : "Show"} query parameters ({params.length})
            </button>
            {open && (
              <div className="table-scroll mt-3">
                <table className="doc-table">
                  <thead>
                    <tr>
                      <th>Parameter</th>
                      <th>Type</th>
                      <th>Default</th>
                      <th>Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    {params.map((p) => (
                      <tr key={p.name}>
                        <td><code>{p.name}</code></td>
                        <td className="text-[#9aa0b4]">{p.type}</td>
                        <td className="text-[#9aa0b4]">{p.def ?? "n/a"}</td>
                        <td className="text-[#9aa0b4]">{p.desc}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
        {children}
      </div>
    </article>
  );
}

/* ------------------------------------------------------------- StatTile */

export function StatTile({
  icon,
  label,
  value,
  suffix,
  field,
}: {
  icon: string;
  label: string;
  value: number;
  suffix?: string;
  field: string;
}) {
  return (
    <div className="stat-tile">
      <div className="icon-tile" aria-hidden>{icon}</div>
      <div className="stat-tile-value">
        {value.toLocaleString("en-US")}
        {suffix && <span className="stat-tile-suffix">{suffix}</span>}
      </div>
      <div className="stat-tile-label">{label}</div>
      <code className="stat-tile-field">{field}</code>
    </div>
  );
}

/* -------------------------------------------------------------- Callout */

export function Callout({
  kind = "note",
  title,
  children,
}: {
  kind?: "note" | "warn" | "todo";
  title?: string;
  children: React.ReactNode;
}) {
  const meta = {
    note: { glyph: "✦", label: "Note", cls: "callout-note" },
    warn: { glyph: "▲", label: "Heads up", cls: "callout-warn" },
    todo: { glyph: "⚑", label: "Owner TODO", cls: "callout-todo" },
  }[kind];
  return (
    <aside className={`callout ${meta.cls}`}>
      <div className="callout-head">
        <span aria-hidden>{meta.glyph}</span>
        <span className="mono-label">{title ?? meta.label}</span>
      </div>
      <div className="[&_code]:text-[#d8ccfe]">{children}</div>
    </aside>
  );
}

/* --------------------------------------------------------------- Eyebrow */

export function Eyebrow({ children }: { children: React.ReactNode }) {
  return <p className="eyebrow">{children}</p>;
}
