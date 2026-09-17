"use client";

import { useCallback, useEffect, useState } from "react";
import { getAppToken } from "@/lib/app-session";
import { apiBaseUrl } from "@/lib/api-base";

export type ReformaOpcao = { codigo: string; descricao: string; label: string };

async function fetchJson<T>(path: string): Promise<T> {
  const token = getAppToken();
  if (!token) return [] as T;
  const res = await fetch(`${apiBaseUrl()}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) return [] as T;
  return (await res.json()) as T;
}

export function useReformaCatalogo(cst?: string) {
  const [csts, setCsts] = useState<ReformaOpcao[]>([]);
  const [classTribs, setClassTribs] = useState<ReformaOpcao[]>([]);
  const [cstsIs, setCstsIs] = useState<ReformaOpcao[]>([]);

  const carregarCst = useCallback(async () => {
    setCsts(await fetchJson("/api/tributacao/reforma/cst-ibs-cbs"));
    setCstsIs(await fetchJson("/api/tributacao/reforma/cst-is"));
  }, []);

  const carregarClass = useCallback(async (cstFiltro?: string) => {
    const qs = cstFiltro ? `?cst=${encodeURIComponent(cstFiltro)}` : "";
    setClassTribs(await fetchJson(`/api/tributacao/reforma/class-trib${qs}`));
  }, []);

  useEffect(() => {
    void carregarCst();
  }, [carregarCst]);

  useEffect(() => {
    void carregarClass(cst);
  }, [carregarClass, cst]);

  return { csts, classTribs, cstsIs, recarregarClass: carregarClass };
}
