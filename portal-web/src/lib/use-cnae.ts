"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api, ApiError, type CnaeItem } from "@/lib/api";

export function useDebouncedCnae(token: string, termo: string, enabled: boolean, limite = 40) {
  const [itens, setItens] = useState<CnaeItem[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const seq = useRef(0);

  const buscar = useCallback(
    (t: string, lim: number) => {
      const id = ++seq.current;
      setCarregando(true);
      setErro(null);
      api
        .buscarCnae(token, t, lim)
        .then((r) => {
          if (id !== seq.current) return;
          setItens(r.itens);
        })
        .catch((e) => {
          if (id !== seq.current) return;
          setItens([]);
          const msg = e instanceof ApiError ? e.message : "Falha ao buscar CNAE";
          setErro(msg.includes("static resource") ? "API desatualizada — reinicie o portal" : msg);
        })
        .finally(() => {
          if (id === seq.current) setCarregando(false);
        });
    },
    [token],
  );

  useEffect(() => {
    if (!enabled) return;
    const t = termo.trim();
    if (t.length > 0 && t.length < 2 && t.replace(/\D/g, "").length < 3) {
      setItens([]);
      return;
    }
    const timer = window.setTimeout(() => buscar(termo, limite), 250);
    return () => window.clearTimeout(timer);
  }, [termo, enabled, limite, buscar]);

  return { itens, carregando, erro, buscar };
}
