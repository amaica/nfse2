"use client";

import { useParams, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { api, type LoginResponse } from "@/lib/api";
import { lerTokenEmbed, limparTokenEmbed, salvarTokenEmbed } from "@/lib/embed-session";
import { senhaDaUrl } from "@/lib/embed-url";
import { saveAppSession } from "@/lib/app-session";
import { PORTAL_EMPRESA_EVENT } from "@/lib/portal-empresa";

function persistirSessao(login: LoginResponse, cnpj?: string) {
  salvarTokenEmbed(login.token, cnpj || login.empresaCnpj);
  saveAppSession({
    token: login.token,
    empresaId: login.empresaId,
    empresaNome: login.empresaNome,
    empresaCnpj: login.empresaCnpj,
    nome: login.nome,
    email: login.email,
  });
}

export function useEmbedToken() {
  const params = useSearchParams();
  const routeParams = useParams();
  const [token, setToken] = useState("");
  const [valid, setValid] = useState<boolean | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const cnpj = useMemo(() => {
    const path = typeof routeParams?.cnpj === "string" ? routeParams.cnpj : "";
    const query = params.get("cnpj") ?? "";
    return (path || query).replace(/\D/g, "");
  }, [routeParams, params]);

  const resolver = useCallback(async () => {
    setValid(null);
    setErro(null);

    const tUrl = params.get("t")?.trim() ?? "";
    const email = (params.get("email") ?? params.get("user") ?? params.get("usuario") ?? "").trim();
    const senha =
      typeof window !== "undefined"
        ? senhaDaUrl()
        : (params.get("senha") ?? params.get("password") ?? "");

    const limparQuerySensivel = () => {
      if (typeof window === "undefined") return;
      if (!email && !senha && !tUrl) return;
      const path = cnpj ? `/embed/${cnpj}` : window.location.pathname;
      window.history.replaceState({}, "", path);
    };

    try {
      let t = tUrl;

      if (!t && senha && (cnpj || email)) {
        const login = cnpj
          ? await api.login({ cnpj, senha })
          : await api.login({ email, senha });
        t = login.token;
        persistirSessao(login, cnpj || undefined);
        limparQuerySensivel();
      }

      if (!t) {
        t = lerTokenEmbed(cnpj || undefined) ?? "";
      }

      if (!t) {
        setToken("");
        setValid(false);
        setErro("missing");
        return;
      }

      await api.validateEmbed(t);
      salvarTokenEmbed(t, cnpj || undefined);
      const me = await api.sessaoAtual(t);
      saveAppSession({
        token: t,
        empresaId: me.empresaId,
        empresaNome: me.empresaNome,
        empresaCnpj: me.empresaCnpj,
        nome: me.nome,
        email: me.email,
      });
      setToken(t);
      setValid(true);
    } catch (e) {
      limparTokenEmbed(cnpj || undefined);
      setToken("");
      setValid(false);
      setErro(e instanceof Error ? e.message : "Acesso negado");
    }
  }, [params, cnpj]);

  useEffect(() => {
    void resolver();
  }, [resolver]);

  useEffect(() => {
    const onTroca = (ev: Event) => {
      const detail = (ev as CustomEvent<LoginResponse>).detail;
      if (detail?.token) {
        setToken(detail.token);
        setValid(true);
        setErro(null);
      }
    };
    window.addEventListener(PORTAL_EMPRESA_EVENT, onTroca);
    return () => window.removeEventListener(PORTAL_EMPRESA_EVENT, onTroca);
  }, []);

  function sair() {
    limparTokenEmbed(cnpj || undefined);
    setToken("");
    setValid(false);
    setErro("missing");
    if (typeof window !== "undefined") {
      const path = cnpj ? `/embed/${cnpj}` : window.location.pathname;
      window.history.replaceState({}, "", path);
    }
  }

  return { token, valid, loading: valid === null, erro, sair, cnpj };
}
