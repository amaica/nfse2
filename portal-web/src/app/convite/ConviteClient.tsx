"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { LoginShell } from "@/components/auth/LoginShell";
import { BaseInput } from "@/components/ui/BaseInput";
import { BaseButton } from "@/components/ui/BaseButton";
import { saveAppSession } from "@/lib/app-session";
import { apiBaseUrl } from "@/lib/api-base";

const API_URL = apiBaseUrl();

type ConviteInfo = {
  email: string;
  papel: string;
  empresaNome: string;
  empresaCnpj: string;
};

export default function ConviteClient() {
  const router = useRouter();
  const params = useSearchParams();
  const token = params.get("token") ?? "";

  const [info, setInfo] = useState<ConviteInfo | null>(null);
  const [nome, setNome] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);

  useEffect(() => {
    if (!token) {
      setErro("Link de convite invalido");
      setBooting(false);
      return;
    }
    fetch(`${API_URL}/api/auth/convite?token=${encodeURIComponent(token)}`)
      .then(async (res) => {
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          throw new Error((body as { erro?: string }).erro ?? "Convite invalido");
        }
        return res.json() as Promise<ConviteInfo>;
      })
      .then(setInfo)
      .catch((e: Error) => setErro(e.message))
      .finally(() => setBooting(false));
  }, [token]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro("");
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/auth/convite/aceitar`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, nome, senha }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new Error((body as { erro?: string }).erro ?? "Falha ao aceitar convite");
      }
      const data = body as {
        token: string;
        refreshToken?: string;
        empresaId: number;
        empresaNome: string;
        empresaCnpj: string;
        nome: string;
        email: string;
        papel?: string;
        contaId?: number;
      };
      saveAppSession({
        token: data.token,
        refreshToken: data.refreshToken,
        empresaId: data.empresaId,
        empresaNome: data.empresaNome,
        empresaCnpj: data.empresaCnpj,
        nome: data.nome,
        email: data.email,
        papel: data.papel,
        contaId: data.contaId,
      });
      router.replace("/painel");
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Erro ao aceitar convite");
    } finally {
      setLoading(false);
    }
  }

  if (booting) {
    return <div className="app-loading">Carregando convite…</div>;
  }

  return (
    <LoginShell title="Aceitar convite" subtitle="Entre na equipe da sua conta SyncNota">
      {info && (
        <div className="alert-agro-success mb-6 rounded-xl border px-4 py-3 text-sm">
          <p className="font-medium text-agro-body">{info.empresaNome}</p>
          <p className="text-agro-muted">
            {info.email} · perfil {info.papel}
          </p>
        </div>
      )}

      {erro && !info && (
        <p className="mb-4 text-center text-sm font-medium text-rose-600">{erro}</p>
      )}

      {info && (
        <form className="space-y-5" onSubmit={onSubmit}>
          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
              Seu nome
            </label>
            <BaseInput
              id="nome"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              placeholder="Nome completo"
            />
          </div>
          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
              Senha
            </label>
            <BaseInput
              id="senha"
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              placeholder="Crie ou confirme sua senha"
            />
          </div>
          {erro && <p className="text-center text-sm font-medium text-rose-600">{erro}</p>}
          <BaseButton type="submit" loading={loading}>
            Entrar na conta
          </BaseButton>
        </form>
      )}

      <Link href="/login" className="link-agro mt-6 block text-center text-sm">
        Voltar ao login
      </Link>
    </LoginShell>
  );
}
