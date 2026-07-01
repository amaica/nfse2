"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { AuthImmersiveShell } from "@/components/auth/AuthImmersiveShell";
import { api } from "@/lib/api";
import { hasPortalAccess, getAdminKey, saveAppSession } from "@/lib/app-session";

const lbl = "mb-1 block text-[10px] font-semibold uppercase tracking-[0.18em] text-white/90";
const inp =
  "w-full rounded-xl border border-gray-200/90 bg-white px-3.5 py-2.5 text-sm text-gray-900 shadow-sm outline-none transition placeholder:text-gray-400 focus:border-gray-300 focus:ring-2 focus:ring-white/25";
const btnPrimary =
  "inline-flex w-full items-center justify-center gap-2 rounded-xl border border-white/10 bg-[#3d9c3d] px-4 py-2.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-white shadow-sm transition hover:brightness-105";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [cnpj, setCnpj] = useState("");
  const [senha, setSenha] = useState("");
  const [modo, setModo] = useState<"email" | "cnpj">("email");
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);

  useEffect(() => {
    if (hasPortalAccess()) {
      router.replace(getAdminKey() ? "/cadastros/empresa" : "/");
      return;
    }
    setBooting(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- checagem única na montagem
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro("");
    setLoading(true);
    try {
      const res = await api.login(
        modo === "cnpj"
          ? { cnpj: cnpj.replace(/\D/g, ""), senha }
          : { email: email.trim(), senha },
      );
      saveAppSession({
        token: res.token,
        empresaId: res.empresaId,
        empresaNome: res.empresaNome,
        empresaCnpj: res.empresaCnpj,
        nome: res.nome,
        email: res.email,
      });
      router.replace("/");
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Falha no login");
    } finally {
      setLoading(false);
    }
  }

  if (booting) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-900 text-white/80">
        Carregando…
      </div>
    );
  }

  return (
    <AuthImmersiveShell>
      <form className="flex flex-col gap-3.5" onSubmit={onSubmit}>
        <div className="mb-2 flex gap-2 text-xs">
          <button
            type="button"
            className={`flex-1 rounded-lg py-1.5 ${modo === "cnpj" ? "bg-white/20 text-white" : "text-white/60"}`}
            onClick={() => setModo("cnpj")}
          >
            CPF/CNPJ
          </button>
          <button
            type="button"
            className={`flex-1 rounded-lg py-1.5 ${modo === "email" ? "bg-white/20 text-white" : "text-white/60"}`}
            onClick={() => setModo("email")}
          >
            E-mail
          </button>
        </div>

        {modo === "cnpj" ? (
          <div>
            <label htmlFor="cnpj" className={lbl}>
              CPF/CNPJ
            </label>
            <input
              id="cnpj"
              className={inp}
              value={cnpj}
              onChange={(e) => setCnpj(e.target.value)}
              placeholder="Documento da empresa"
              autoComplete="username"
            />
          </div>
        ) : (
          <div>
            <label htmlFor="email" className={lbl}>
              Usuário
            </label>
            <input
              id="email"
              type="email"
              className={inp}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
            />
          </div>
        )}

        <div>
          <label htmlFor="senha" className={lbl}>
            Senha
          </label>
          <input
            id="senha"
            type="password"
            className={inp}
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            autoComplete="current-password"
          />
        </div>

        {erro && <p className="text-sm text-red-300">{erro}</p>}

        <button type="submit" className={btnPrimary} disabled={loading}>
          {loading ? "Entrando…" : "Login"}
        </button>

        <Link href="/auth/admin" className="text-center text-xs text-white/70 hover:text-white">
          Acesso administrador (cadastro de empresas)
        </Link>
        <p className="text-center text-[11px] text-white/45">
          Demo: admin@synki.demo / demo123
        </p>
      </form>
      <p className="mt-6 text-center text-[10px] uppercase tracking-widest text-white/50">
        © {new Date().getFullYear()} SyncNota
      </p>
    </AuthImmersiveShell>
  );
}
