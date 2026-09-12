import { useEffect, useRef, useState } from "react";
import { clsx } from "clsx";

/* Local Magic UI pattern set for statfyr docs (landing only, per SPEC §7).
   Adapted from potenfyr-nest/src/components/magicui.tsx */

/** Magic UI · Number Ticker: counts up to `value` (rAF, reduced-motion safe). */
export function NumberTicker({
  value,
  className,
}: {
  value: number;
  className?: string;
}) {
  const [display, setDisplay] = useState(0);
  const prev = useRef(0);

  useEffect(() => {
    if (
      typeof document !== "undefined" &&
      (document.hidden ||
        window.matchMedia("(prefers-reduced-motion: reduce)").matches)
    ) {
      prev.current = value;
      setDisplay(value);
      return;
    }
    const from = prev.current;
    const start = performance.now();
    const duration = 900;
    let raf = 0;
    const step = (t: number) => {
      const p = Math.min(1, (t - start) / duration);
      const eased = 1 - Math.pow(1 - p, 3);
      setDisplay(Math.round(from + (value - from) * eased));
      if (p < 1) raf = requestAnimationFrame(step);
      else prev.current = value;
    };
    raf = requestAnimationFrame(step);
    return () => cancelAnimationFrame(raf);
  }, [value]);

  return (
    <span className={className} aria-label={String(value)}>
      {display.toLocaleString("en-US")}
    </span>
  );
}

/** Magic UI · Marquee: seamless infinite scroller (pauses on hover). */
export function Marquee({
  children,
  reverse = false,
  pause = true,
  className,
}: {
  children: React.ReactNode;
  reverse?: boolean;
  pause?: boolean;
  className?: string;
}) {
  return (
    <div
      className={clsx(
        "group flex w-full overflow-hidden [--duration:35s] [--gap:3rem] [gap:var(--gap)]",
        className,
      )}
    >
      {[0, 1].map((i) => (
        <div
          key={i}
          aria-hidden={i === 1}
          style={{
            animation: `nestMarqueeScroll var(--duration) linear infinite`,
            animationDirection: reverse ? "reverse" : "normal",
          }}
          className={clsx(
            "flex shrink-0 items-center justify-around [gap:var(--gap)] min-w-full",
            pause && "group-hover:[animation-play-state:paused]",
          )}
        >
          {children}
        </div>
      ))}
      <style>{`
        @keyframes nestMarqueeScroll {
          from { transform: translateX(0); }
          to { transform: translateX(calc(-100% - var(--gap))); }
        }
      `}</style>
    </div>
  );
}

/** Magic UI · Dot Pattern: decorative dotted backdrop (hero scope). */
export function DotPattern({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden
      className={clsx("pointer-events-none absolute inset-0 h-full w-full", className)}
    >
      <defs>
        <pattern id="statfyr-dots" width="28" height="28" patternUnits="userSpaceOnUse">
          <circle cx="2" cy="2" r="1.2" fill="rgba(16,185,129,0.16)" />
        </pattern>
      </defs>
      <rect width="100%" height="100%" fill="url(#statfyr-dots)" />
    </svg>
  );
}

/** Magic UI · Meteors: streaking comets (hero only). */
export function Meteors({ number = 14 }: { number?: number }) {
  const meteors = Array.from({ length: number }, (_, i) => ({
    id: i,
    left: (i * 137) % 100,
    delay: ((i * 2.3) % 8).toFixed(1),
    dur: (4.5 + ((i * 1.1) % 4)).toFixed(1),
  }));
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden">
      {meteors.map((m) => (
        <span
          key={m.id}
          className="absolute h-0.5 w-0.5 rotate-[215deg] rounded-full bg-brand-pink shadow-[0_0_0_1px_rgba(236,72,153,0.12)] before:content-[''] before:absolute before:top-1/2 before:h-px before:w-20 before:-translate-y-1/2 before:bg-gradient-to-r before:from-[#ec4899] before:to-transparent"
          style={{
            left: `${m.left}%`,
            top: "-8%",
            animation: `meteor-fall ${m.dur}s linear ${m.delay}s infinite`,
          }}
        />
      ))}
    </div>
  );
}

/** Ambient glow orb (hero only, SPEC §5.14). */
export function GlowOrb({
  className,
  color = "rgba(139, 92, 246, 0.18)",
  size = 500,
}: {
  className?: string;
  color?: string;
  size?: number;
}) {
  return (
    <div
      aria-hidden
      className={clsx(
        "pointer-events-none absolute rounded-full blur-[140px] will-change-transform",
        className,
      )}
      style={{
        width: size,
        height: size,
        background: color,
        animation: "orb-pulse 3.2s ease-in-out infinite",
      }}
    />
  );
}

/** Interactive FAQ accordion item. */
export function AccordionItem({
  title,
  children,
  isOpen,
  onToggle,
}: {
  title: string;
  children: React.ReactNode;
  isOpen: boolean;
  onToggle: () => void;
}) {
  return (
    <div className="relative overflow-hidden rounded-2xl border border-line-light bg-white/[0.02] transition-colors hover:border-white/20">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={isOpen}
        className="flex w-full items-center justify-between gap-4 p-5 text-left"
      >
        <span className="font-semibold text-white text-[15.5px]">{title}</span>
        <span
          className="shrink-0 text-[#9aa0b4] transition-transform duration-200"
          style={{ transform: isOpen ? "rotate(180deg)" : "none" }}
          aria-hidden
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="m6 9 6 6 6-6" />
          </svg>
        </span>
      </button>
      <div
        className="overflow-hidden transition-all duration-200"
        style={{
          maxHeight: isOpen ? "640px" : "0px",
          opacity: isOpen ? 1 : 0,
        }}
      >
        <div className="px-5 pb-5 pt-0 text-sm leading-relaxed text-[#9aa0b4] border-t border-white/[0.06]">
          <div className="pt-3 [&_code]:text-[#d8ccfe]">{children}</div>
        </div>
      </div>
    </div>
  );
}
