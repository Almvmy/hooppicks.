export const LogoSymbol = ({ className = "w-10 h-10" }: { className?: string }) => (
  <svg 
    viewBox="0 0 100 100" 
    fill="none" 
    xmlns="http://www.w3.org/2000/svg" 
    className={className}
  >
    <defs>
      <linearGradient id="hp-glow" x1="0%" y1="100%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="#FF5E00" />
        <stop offset="100%" stopColor="#00E5FF" />
      </linearGradient>
    </defs>
    {/* Ballon de Basketball (Lignes de fond) */}
    <circle cx="45" cy="55" r="36" stroke="#262F40" strokeWidth="4" />
    <path d="M 12 55 A 33 33 0 0 1 78 55" stroke="#262F40" strokeWidth="3" />
    <path d="M 45 22 A 33 33 0 0 1 45 88" stroke="#262F40" strokeWidth="3" />
    <path d="M 20 30 C 35 42, 35 68, 20 80" stroke="#262F40" strokeWidth="3" />
    <path d="M 70 30 C 55 42, 55 68, 70 80" stroke="#262F40" strokeWidth="3" />
    
    {/* Flèche Pick / Growth (Élément Néon) */}
    <path 
      d="M 15 65 L 35 48 L 50 60 L 85 20" 
      stroke="url(#hp-glow)" 
      strokeWidth="9" 
      strokeLinecap="round" 
      strokeLinejoin="round" 
    />
    <path 
      d="M 62 20 H 85 V 43" 
      stroke="url(#hp-glow)" 
      strokeWidth="9" 
      strokeLinecap="round" 
      strokeLinejoin="round" 
    />
  </svg>
)