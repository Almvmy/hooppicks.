type LogoSymbolProps = {
  className?: string;
  /** "full" shows all basketball seams (hero/marketing use). "compact" shows a single seam, tuned for small header/nav sizes. */
  variant?: "full" | "compact";
};

export const LogoSymbol = ({ className = "w-10 h-10", variant = "full" }: LogoSymbolProps) => (
  <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg" className={className}>
    <circle cx="58" cy="62" r="42" stroke="#FF7A1A" strokeWidth={variant === "full" ? 6 : 7} />
    <path d="M16 62 H100" stroke="#FF7A1A" strokeWidth={variant === "full" ? 4 : 5} />
    {variant === "full" && (
      <>
        <path d="M26 34 C42 48 42 76 26 90" stroke="#FF7A1A" strokeWidth="4" />
        <path d="M90 34 C74 48 74 76 90 90" stroke="#FF7A1A" strokeWidth="4" />
      </>
    )}
    <path d="M58 20 V104" stroke="#FF7A1A" strokeWidth={variant === "full" ? 4 : 5} />
    <path
      d="M32 66 L54 88 L108 16"
      stroke="#0B1120"
      strokeWidth={variant === "full" ? 23 : 24}
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M32 66 L54 88 L108 16"
      stroke="#FF7A1A"
      strokeWidth={variant === "full" ? 11 : 12}
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);
