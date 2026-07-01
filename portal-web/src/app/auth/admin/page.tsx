"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AuthImmersiveShell } from "@/components/auth/AuthImmersiveShell";
import { saveAdminKey } from "@/lib/app-session";

const lbl = "mb-1 block text-[10px] font-semibold uppercase tracking-[0.18em] text-white/90";
const inp =
  "w-full rounded-xl border border-gray-200/90 bg-white px-3.5 py-2.5 text-sm text-gray-900 shadow-sm outline-none";
const btnPrimary =
  "inline-flex w-full items-center justify-center rounded-xl bg-[#3d9c3d] px-4 py-2.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-white";

export default function AdminAuthPage() {
  const router = useRouter();
  const [key, setKey] = useState("");
  const [erro, setErro] = useState("");

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!key.trim()) {
      setErro("Informe a chave admin");
      return;
    }
    saveAdminKey(key.trim());
    router.push("/cadastros/empresa");
  }

  return (
    <AuthImmersiveShell>
      <form className="space-y-4" onSubmit={onSubmit}>
        <h2 className="text-center text-sm font-medium text-white">Administração</h2>
        <p className="text-center text-xs text-white/60">Chave NFSE_ADMIN_SECRET</p>
        <div>
          <label htmlFor="admin-key" className={lbl}>
            Chave
          </label>
          <input
            id="admin-key"
            type="password"
            className={inp}
            value={key}
            onChange={(e) => setKey(e.target.value)}
            autoComplete="off"
          />
        </div>
        {erro && <p className="text-sm text-red-300">{erro}</p>}
        <button type="submit" className={btnPrimary}>
          Entrar
        </button>
      </form>
    </AuthImmersiveShell>
  );
}
