"use client";

import { GestaoGuard } from "@/components/auth/GestaoGuard";
import { CadastroUsuariosWorkspace } from "@/components/cadastros/CadastroUsuariosWorkspace";

export default function UsuariosPage() {
  return (
    <GestaoGuard>
      <CadastroUsuariosWorkspace />
    </GestaoGuard>
  );
}
