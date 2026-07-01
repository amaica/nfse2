"use client";

import { useParams, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { lerTokenEmbed, limparTokenEmbed, salvarTokenEmbed } from "@/lib/embed-session";
import { senhaDaUrl } from "@/lib/embed-url";

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

  useEffect(() => {
    let cancelado = false;

    async function resolver() {
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
          salvarTokenEmbed(t, cnpj || undefined);
          limparQuerySensivel();
        }

        if (!t) {
          t = lerTokenEmbed(cnpj || undefined) ?? "";
        }

        if (!t) {
          if (!cancelado) {
            setToken("");
            setValid(false);
            setErro("missing");
          }
          return;
        }

        await api.validateEmbed(t);
        if (!cancelado) {
          salvarTokenEmbed(t, cnpj || undefined);
          setToken(t);
          setValid(true);
        }
      } catch (e) {
        if (!cancelado) {
          limparTokenEmbed(cnpj || undefined);
          setToken("");
          setValid(false);
          setErro(e instanceof Error ? e.message : "Acesso negado");
        }
      }
    }

    resolver();
    return () => {
      cancelado = true;
    };
  }, [params, cnpj]);

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
