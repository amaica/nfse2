"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Building2, Eye, EyeOff, Lock, Mail } from "lucide-react";
import { LoginShell } from "@/components/auth/LoginShell";
import { BaseInput } from "@/components/ui/BaseInput";
import { BaseButton } from "@/components/ui/BaseButton";
import { api, ApiError } from "@/lib/api";
import { mapAuthError } from "@/lib/assinatura";
import { hasPortalAccess, getAdminKey, saveLoginResponse, isOnboardingSession } from "@/lib/app-session";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [cnpj, setCnpj] = useState("");
  const [senha, setSenha] = useState("");
  const [modo, setModo] = useState<"email" | "cnpj">("email");
  const [showPassword, setShowPassword] = useState(false);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);

  useEffect(() => {
    if (hasPortalAccess()) {
      if (isOnboardingSession()) {
        router.replace("/onboarding");
      } else {
        router.replace(getAdminKey() ? "/cadastros/empresa" : "/painel");
      }
    } else {
      setBooting(false);
    }
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
      saveLoginResponse(res);
      router.replace(res.onboardingRequired ? "/onboarding" : "/painel");
    } catch (err) {
      setErro(mapAuthError(err instanceof ApiError ? err.message : "Falha no login", err instanceof ApiError ? err.status : undefined));
    } finally {
      setLoading(false);
    }
  }

  if (booting) {
    return (
      <div className="app-loading">Carregando…</div>
    );
  }

  return (
    <LoginShell subtitle="Acesse com CPF/CNPJ ou e-mail da propriedade / empresa">
      <div className="login-tabs">
        <button
          type="button"
          className={`login-tabs__btn ${modo === "cnpj" ? "login-tabs__btn--active" : ""}`}
          onClick={() => setModo("cnpj")}
        >
          CPF/CNPJ
        </button>
        <button
          type="button"
          className={`login-tabs__btn ${modo === "email" ? "login-tabs__btn--active" : ""}`}
          onClick={() => setModo("email")}
        >
          E-mail
        </button>
      </div>

      <form className="space-y-5" onSubmit={onSubmit}>
        {modo === "cnpj" ? (
          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
              CPF/CNPJ da empresa
            </label>
            <BaseInput
              id="cnpj"
              value={cnpj}
              onChange={(e) => setCnpj(e.target.value)}
              placeholder="Documento do emitente"
              autoComplete="username"
              icon={<Building2 className="h-4 w-4" />}
            />
          </div>
        ) : (
          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
              E-mail
            </label>
            <BaseInput
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="usuario@empresa.com.br"
              autoComplete="username"
              icon={<Mail className="h-4 w-4" />}
            />
          </div>
        )}

        <div>
          <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
            Senha
          </label>
          <div className="relative">
            <BaseInput
              id="senha"
              type={showPassword ? "text" : "password"}
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              icon={<Lock className="h-4 w-4" />}
              className="!pr-12"
            />
            <button
              type="button"
              className="absolute right-4 top-1/2 -translate-y-1/2 text-agro-muted hover:text-[var(--primary-600)]"
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
        </div>

        {erro && <p className="text-center text-sm font-medium text-rose-600">{erro}</p>}

        <BaseButton type="submit" loading={loading}>
          Entrar
        </BaseButton>

        <Link href="/auth/admin" className="link-agro block text-center text-sm">
          Acesso administrador da plataforma (cadastro global)
        </Link>

        <p className="text-center text-sm text-agro-muted">
          Novo por aqui?{" "}
          <Link href="/registrar" className="link-agro font-medium">
            Criar conta grátis
          </Link>
        </p>
      </form>
    </LoginShell>
  );
}
