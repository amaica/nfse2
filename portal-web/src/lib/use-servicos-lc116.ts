"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api, ApiError, type ServicosLc116Response } from "@/lib/api";
import type { ServicoLc116 } from "@/lib/servicos-lc116";

export type GrupoServico = "todos" | "agro" | "mecanico";

export function useServicosLc116(token: string) {
  const [itens, setItens] = useState<ServicoLc116[]>([]);
  const [meta, setMeta] = useState<Pick<ServicosLc116Response, "total" | "totalAgro" | "totalMecanico" | "exibidos"> | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const seq = useRef(0);

  const buscar = useCallback(
    (termo: string, grupo: GrupoServico = "todos", limite = 400) => {
      const id = ++seq.current;
      setCarregando(true);
      setErro(null);
      api
        .buscarServicos(token, termo, limite, grupo)
        .then((r) => {
          if (id !== seq.current) return;
          setMeta({
            total: r.total,
            totalAgro: r.totalAgro,
            totalMecanico: r.totalMecanico,
            exibidos: r.exibidos,
          });
          setItens(
            r.itens.map((s) => ({
              ...s,
              descricaoPadrao: s.descricao,
            })),
          );
        })
        .catch((e) => {
          if (id !== seq.current) return;
          setItens([]);
          setMeta(null);
          const msg = e instanceof ApiError ? e.message : "Falha ao buscar serviços";
          setErro(msg.includes("static resource") ? "API desatualizada — reinicie o portal (./start-portal.sh)" : msg);
        })
        .finally(() => {
          if (id === seq.current) setCarregando(false);
        });
    },
    [token],
  );

  return { itens, meta, carregando, erro, buscar };
}

export function useDebouncedServicos(
  token: string,
  termo: string,
  grupo: GrupoServico,
  enabled: boolean,
  limite = 400,
) {
  const { itens, meta, carregando, erro, buscar } = useServicosLc116(token);

  useEffect(() => {
    if (!enabled) return;
    const t = window.setTimeout(() => buscar(termo, grupo, limite), 220);
    return () => window.clearTimeout(t);
  }, [termo, grupo, enabled, limite, buscar]);

  return { itens, meta, carregando, erro, buscar };
}
