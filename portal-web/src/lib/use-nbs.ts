"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api, ApiError, type NbsItem } from "@/lib/api";

export function useDebouncedNbs(
  token: string,
  termo: string,
  lc116: string,
  enabled: boolean,
  limite = 30,
) {
  const [itens, setItens] = useState<NbsItem[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const seq = useRef(0);

  const buscar = useCallback(
    (t: string, lc: string, lim: number) => {
      const id = ++seq.current;
      setCarregando(true);
      setErro(null);
      const termoBusca = t.trim();
      const lcParaApi = termoBusca.length >= 2 ? "" : lc;
      api
        .buscarNbs(token, termoBusca, lim, lcParaApi || undefined)
        .then((r) => {
          if (id !== seq.current) return;
          setItens(r.itens);
        })
        .catch((e) => {
          if (id !== seq.current) return;
          setItens([]);
          const msg = e instanceof ApiError ? e.message : "Falha ao buscar NBS";
          setErro(msg.includes("static resource") ? "API desatualizada — reinicie o portal (./start-portal.sh)" : msg);
        })
        .finally(() => {
          if (id === seq.current) setCarregando(false);
        });
    },
    [token],
  );

  useEffect(() => {
    if (!enabled) return;
    const timer = window.setTimeout(() => buscar(termo, lc116, limite), 250);
    return () => window.clearTimeout(timer);
  }, [termo, lc116, enabled, limite, buscar]);

  return { itens, carregando, erro, buscar };
}