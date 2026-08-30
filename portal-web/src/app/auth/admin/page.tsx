"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { KeyRound } from "lucide-react";
import { LoginShell } from "@/components/auth/LoginShell";
import { BaseInput } from "@/components/ui/BaseInput";
import { BaseButton } from "@/components/ui/BaseButton";
import { saveAdminKey } from "@/lib/app-session";
import { adminApi, ApiError } from "@/lib/api";

export default function AdminAuthPage() {
  const router = useRouter();
  const [key, setKey] = useState("");
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!key.trim()) {
      setErro("Informe a chave admin");
      return;
    }
    setErro("");
    setLoading(true);
    try {
      const { token } = await adminApi.login(key.trim());
      saveAdminKey(token);
      router.push("/cadastros/empresa");
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : "Falha ao autenticar");
    } finally {
      setLoading(false);
    }
  }

  return (
    <LoginShell title="Administração" subtitle="Chave NFSE_ADMIN_SECRET para gestão de empresas">
      <form className="space-y-5" onSubmit={onSubmit}>
        <div>
          <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
            Chave administrativa
          </label>
          <BaseInput
            id="admin-key"
            type="password"
            value={key}
            onChange={(e) => setKey(e.target.value)}
            placeholder="••••••••••••"
            autoComplete="off"
            icon={<KeyRound className="h-4 w-4" />}
          />
        </div>

        {erro && <p className="text-center text-sm font-medium text-rose-600">{erro}</p>}

        <BaseButton type="submit" loading={loading}>Entrar</BaseButton>

        <Link href="/login" className="link-agro block text-center text-sm">
          Voltar ao login
        </Link>
      </form>
    </LoginShell>
  );
}
