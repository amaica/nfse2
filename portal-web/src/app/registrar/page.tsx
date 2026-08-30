"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Building2, Eye, EyeOff, Lock, Mail, User } from "lucide-react";
import { LoginShell } from "@/components/auth/LoginShell";
import { BaseInput } from "@/components/ui/BaseInput";
import { BaseButton } from "@/components/ui/BaseButton";
import { api, ApiError } from "@/lib/api";
import { mapAuthError } from "@/lib/assinatura";
import { hasPortalAccess, getAdminKey, saveLoginResponse, isOnboardingSession } from "@/lib/app-session";

export default function RegistrarPage() {
  const router = useRouter();
  const [nome, setNome] = useState("");
  const [nomeConta, setNomeConta] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro("");
    setLoading(true);
    try {
      const res = await api.register({
        nome: nome.trim(),
        nomeConta: (nomeConta || nome).trim(),
        email: email.trim(),
        senha,
      });
      saveLoginResponse(res);
      router.replace("/onboarding");
    } catch (err) {
      setErro(mapAuthError(err instanceof ApiError ? err.message : "Falha no cadastro", err instanceof ApiError ? err.status : undefined));
    } finally {
      setLoading(false);
    }
  }

  if (booting) {
    return <div className="app-loading">Carregando…</div>;
  }

  return (
    <LoginShell subtitle="Crie sua conta — 14 dias de trial gratuito">
      <form className="space-y-5" onSubmit={onSubmit}>
        <div>
          <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
            Seu nome
          </label>
          <BaseInput
            id="nome"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            placeholder="João Silva"
            autoComplete="name"
            icon={<User className="h-4 w-4" />}
            required
          />
        </div>

        <div>
          <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
            Nome da conta / propriedade
          </label>
          <BaseInput
            id="nomeConta"
            value={nomeConta}
            onChange={(e) => setNomeConta(e.target.value)}
            placeholder="Fazenda Boa Vista (opcional)"
            icon={<Building2 className="h-4 w-4" />}
          />
        </div>

        <div>
          <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
            E-mail
          </label>
          <BaseInput
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="voce@empresa.com.br"
            autoComplete="email"
            icon={<Mail className="h-4 w-4" />}
            required
          />
        </div>

        <div>
          <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
            Senha (mín. 6 caracteres)
          </label>
          <div className="relative">
            <BaseInput
              id="senha"
              type={showPassword ? "text" : "password"}
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              placeholder="••••••••"
              autoComplete="new-password"
              icon={<Lock className="h-4 w-4" />}
              className="!pr-12"
              minLength={6}
              required
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
          Criar conta grátis
        </BaseButton>

        <p className="text-center text-sm text-agro-muted">
          Já tem conta?{" "}
          <Link href="/login" className="link-agro font-medium">
            Entrar
          </Link>
        </p>
      </form>
    </LoginShell>
  );
}
