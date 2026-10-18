import type { Metadata } from "next";

// Page ouverte sans compte : c'est elle qu'on partage pour inviter à pronostiquer.
export const metadata: Metadata = {
  title: "Pronostics de saison",
  description: "Champion NBA, finalistes, meilleur bilan : fais tes pronostics de saison sur HoopPicks.",
};

export default function SeasonLayout({ children }: { children: React.ReactNode }) {
  return children;
}
