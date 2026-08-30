"use client";

import { GestaoGuard } from "@/components/auth/GestaoGuard";
import { NfseEmitidasWorkspace } from "@/components/nfse/NfseEmitidasWorkspace";

export default function NfseEmitidasPage() {
  return (
    <GestaoGuard>
      <NfseEmitidasWorkspace />
    </GestaoGuard>
  );
}
