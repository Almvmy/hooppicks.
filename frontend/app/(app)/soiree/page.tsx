import type { Metadata } from "next";
import { NightThread } from "@/components/night-thread";

export const metadata: Metadata = { title: "Le fil de la soirée" };

export default function SoireePage() {
  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-4">
      <h1 className="font-heading text-2xl font-bold">Le fil de la soirée</h1>
      <NightThread />
    </div>
  );
}
