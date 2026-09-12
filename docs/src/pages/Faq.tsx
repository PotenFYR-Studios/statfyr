import { useEffect, useState } from "react";
import { applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { AccordionItem } from "../components/magicui";
import { Callout, CodeBlock } from "../components/ui";

/* These three items mirror the FAQPage JSON-LD emitted for this route. */
const FAQS = [
  {
    q: "Does Statfyr impact server TPS?",
    a: (
      <>
        <p>
          No. Statistics are read asynchronously on a background thread pool, and the embedded HTTP
          server runs its own executor, so API requests never touch the main server thread. A
          30-second statistic cache means repeated polls hit memory, not the Bukkit API.
        </p>
        <p>
          You can confirm live behavior any time: <code>/statfyr status</code> shows the async
          worker state, and <code>/api/health</code> reports the executor's active threads.
        </p>
      </>
    ),
  },
  {
    q: "Does Statfyr support offline players?",
    a: (
      <>
        <p>
          Yes. For players currently online, stats are read live from the Bukkit Statistic API. For
          offline players, statfyr parses the vanilla <code>world/stats/&lt;uuid&gt;.json</code>{" "}
          files, so leaderboards and lookups include everyone who has ever played.
        </p>
        <p>
          Names are resolved the same way in both cases: a UUID always works, and an exact current
          name works for any player the server has seen before.
        </p>
      </>
    ),
  },
  {
    q: "What Minecraft versions are supported?",
    a: (
      <p>
        Paper, Spigot, Purpur, and compatible forks running 1.16.5 through 1.21.x. The plugin
        compiles against the Paper 1.16.5 API and uses only stable Bukkit API surface.
      </p>
    ),
  },
];

export default function Faq() {
  const [open, setOpen] = useState(0);

  useEffect(() => {
    applyMeta({
      title: "FAQ · Statfyr Docs",
      description:
        "Answers about Statfyr: TPS impact, offline player support, supported Minecraft versions, HTTPS, CORS, and where to get help.",
      path: "/docs/faq",
    });
  }, []);

  return (
    <DocsShell
      crumbs={[
        { label: "Docs", to: "/docs" },
        { label: "v1.0.0-BETA" },
        { label: "FAQ" },
      ]}
      title="Frequently Asked Questions"
      lede="Short answers to the questions that come up most. Anything else lands in the issue tracker."
      toc={[{ id: "answers", label: "Answers" }, { id: "still-stuck", label: "Still stuck?" }]}
    >
      <h2 id="answers">Answers</h2>
      <div className="flex flex-col gap-3">
        {FAQS.map((f, i) => (
          <AccordionItem
            key={f.q}
            title={f.q}
            isOpen={open === i}
            onToggle={() => setOpen(open === i ? -1 : i)}
          >
            {f.a}
          </AccordionItem>
        ))}
      </div>

      <h2 id="still-stuck">Still stuck?</h2>
      <p>
        Bug reports, feature ideas, and questions each have a template in the
        <a href="https://github.com/PotenFYR-Studios/statfyr/issues/new/choose" target="_blank" rel="noopener noreferrer">
          {" "}issue chooser
        </a>. For anything sensitive, see the contact channel in
        <a href="https://github.com/PotenFYR-Studios/statfyr/blob/master/SECURITY.md" target="_blank" rel="noopener noreferrer"> SECURITY.md</a>.
      </p>
      <Callout kind="warn">
        When posting logs, scrub your API key, keystore password, and any player IPs first; the
        bug template includes the same reminder.
      </Callout>
      <p>
        Before filing, it helps to capture one failing request with debug mode on:
      </p>
      <CodeBlock
        lang="yaml"
        code={`# plugins/statfyr/config.yml
debug: true`}
      />
    </DocsShell>
  );
}
