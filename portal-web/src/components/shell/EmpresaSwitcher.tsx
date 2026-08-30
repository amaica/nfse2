"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Building2, Check, ChevronDown, Loader2, Search } from "lucide-react";
import { api, formatarCnpjCpf, type EmpresaResumo, type LoginResponse } from "@/lib/api";
import { getAppSession, getAppToken, saveAppSession } from "@/lib/app-session";
import { useAppSession } from "@/hooks/useAppSession";
import { salvarTokenEmbed } from "@/lib/embed-session";
import { dispatchEmpresaAlterada } from "@/lib/portal-empresa";

type Props = {
  token?: string | null;
  compact?: boolean;
  /** Rótulo do botão quando usado como ação explícita de troca. */
  labelTrocar?: boolean;
};

const PANEL_ALTURA = 360;

function aplicarTroca(res: LoginResponse) {
  saveAppSession({
    token: res.token,
    refreshToken: res.refreshToken,
    empresaId: res.empresaId,
    empresaNome: res.empresaNome,
    empresaCnpj: res.empresaCnpj,
    nome: res.nome,
    email: res.email,
    papel: res.papel,
    contaId: res.contaId,
  });
  salvarTokenEmbed(res.token, res.empresaCnpj);
  dispatchEmpresaAlterada(res);
}

export function EmpresaSwitcher({ token: tokenProp, compact, labelTrocar }: Props) {
  const [aberto, setAberto] = useState(false);
  const [busca, setBusca] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [trocando, setTrocando] = useState<number | null>(null);
  const [itens, setItens] = useState<EmpresaResumo[]>([]);
  const [token, setToken] = useState<string | null>(null);
  const { session: sessao, ready: sessaoReady } = useAppSession();
  const [panelRect, setPanelRect] = useState<{ top: number; left: number; width: number } | null>(
    null,
  );
  const rootRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setToken(tokenProp ?? getAppToken());
  }, [tokenProp]);

  const atualizarSessao = useCallback(async () => {
    if (getAppSession()) return;
    if (!token) return;
    try {
      const me = await api.sessaoAtual(token);
      saveAppSession({
        token,
        empresaId: me.empresaId,
        empresaNome: me.empresaNome,
        empresaCnpj: me.empresaCnpj,
        nome: me.nome,
        email: me.email,
      });
    } catch {
      /* sessão indisponível */
    }
  }, [token]);

  useEffect(() => {
    void atualizarSessao();
    const onTroca = () => void atualizarSessao();
    window.addEventListener("portal-empresa-alterada", onTroca);
    return () => window.removeEventListener("portal-empresa-alterada", onTroca);
  }, [atualizarSessao]);

  const carregar = useCallback(
    async (termo: string) => {
      if (!token) return;
      setCarregando(true);
      try {
        const res = await api.listarEmpresas(token, termo, 100);
        setItens(res.itens);
      } finally {
        setCarregando(false);
      }
    },
    [token],
  );

  useEffect(() => {
    if (!aberto || !token) return;
    const termo = busca.trim();
    if (termo.length < 2) {
      void carregar("");
      return;
    }
    const t = window.setTimeout(() => void carregar(termo), 200);
    return () => window.clearTimeout(t);
  }, [aberto, busca, carregar, token]);

  const reposicionarPainel = useCallback(() => {
    const el = rootRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const width = Math.min(384, window.innerWidth - 16);
    let left = Math.min(rect.left, window.innerWidth - width - 8);
    left = Math.max(8, left);
    const espacoAbaixo = window.innerHeight - rect.bottom;
    const top =
      espacoAbaixo < PANEL_ALTURA && rect.top > PANEL_ALTURA
        ? rect.top - PANEL_ALTURA - 8
        : rect.bottom + 8;
    setPanelRect({ top, left, width });
  }, []);

  useEffect(() => {
    if (!aberto) {
      setPanelRect(null);
      return;
    }
    reposicionarPainel();
    window.addEventListener("resize", reposicionarPainel);
    window.addEventListener("scroll", reposicionarPainel, true);
    return () => {
      window.removeEventListener("resize", reposicionarPainel);
      window.removeEventListener("scroll", reposicionarPainel, true);
    };
  }, [aberto, reposicionarPainel]);

  useEffect(() => {
    if (!aberto) return;
    function fora(e: MouseEvent) {
      const alvo = e.target as Node;
      if (rootRef.current?.contains(alvo) || panelRef.current?.contains(alvo)) return;
      setAberto(false);
    }
    document.addEventListener("mousedown", fora);
    return () => document.removeEventListener("mousedown", fora);
  }, [aberto]);

  async function selecionar(empresaId: number) {
    if (!token || empresaId === sessao?.empresaId) {
      setAberto(false);
      return;
    }
    setTrocando(empresaId);
    try {
      const res = await api.trocarEmpresa(token, empresaId);
      aplicarTroca(res);
      setToken(res.token);
      setAberto(false);
      setBusca("");
    } finally {
      setTrocando(null);
    }
  }

  if (!sessaoReady || !token || !sessao) return null;

  const label = labelTrocar
    ? "Trocar emitente"
    : compact
      ? sessao.empresaCnpj
        ? formatarCnpjCpf(sessao.empresaCnpj)
        : "Empresa"
      : sessao.empresaNome ?? "Selecionar empresa";
  const termoBusca = busca.trim();

  const painel =
    aberto && panelRect
      ? createPortal(
          <div
            ref={panelRef}
            className="rounded-xl border border-[var(--border)] bg-white shadow-2xl"
            style={{
              position: "fixed",
              top: panelRect.top,
              left: panelRect.left,
              width: panelRect.width,
              zIndex: 99999,
              maxHeight: PANEL_ALTURA,
              display: "flex",
              flexDirection: "column",
            }}
          >
            <div className="shrink-0 border-b border-[var(--border)] p-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-agro-muted">
                Trocar empresa emitente
              </p>
              <div className="relative">
                <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-agro-muted" />
                <input
                  type="search"
                  value={busca}
                  onChange={(e) => setBusca(e.target.value)}
                  placeholder="Filtrar por nome ou CNPJ..."
                  className="w-full rounded-lg border border-[var(--border)] bg-[#fafcf7] py-2 pl-9 pr-3 text-sm outline-none focus:border-[var(--brand)]"
                  autoFocus
                />
              </div>
            </div>
            <ul className="min-h-0 flex-1 overflow-y-auto p-1">
              {carregando && itens.length === 0 && (
                <li className="flex items-center justify-center gap-2 px-3 py-6 text-sm text-agro-muted">
                  <Loader2 className="h-4 w-4 animate-spin" /> Carregando emitentes…
                </li>
              )}
              {!carregando && itens.length === 0 && termoBusca.length >= 2 && (
                <li className="px-3 py-6 text-center text-sm text-agro-muted">
                  Nenhuma empresa encontrada
                </li>
              )}
              {!carregando && itens.length === 0 && termoBusca.length < 2 && (
                <li className="px-3 py-6 text-center text-sm text-agro-muted">
                  Nenhum emitente vinculado à sua conta
                </li>
              )}
              {itens.map((e) => {
                const atual = e.id === sessao.empresaId;
                const ambiente = e.ambiente === "producao" ? "Produção" : "Homologação";
                return (
                  <li key={e.id}>
                    <button
                      type="button"
                      disabled={trocando !== null}
                      onClick={() => void selecionar(e.id)}
                      className={`flex w-full items-start gap-2 rounded-lg px-3 py-2.5 text-left text-sm hover:bg-[var(--primary-50)] ${
                        atual ? "empresa-item-atual" : ""
                      }`}
                    >
                      <span className="mt-0.5 w-4 shrink-0">
                        {trocando === e.id ? (
                          <Loader2 className="h-4 w-4 animate-spin text-agro-muted" />
                        ) : atual ? (
                          <Check className="h-4 w-4 text-[var(--primary-600)]" />
                        ) : null}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block font-medium leading-snug text-agro-body">{e.nome}</span>
                        <span className="mt-0.5 block text-xs text-agro-muted">
                          {formatarCnpjCpf(e.cnpj)} · {ambiente}
                          {!e.certificadoCadastrado && " · sem certificado"}
                        </span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>,
          document.body,
        )
      : null;

  return (
    <div className="relative" ref={rootRef}>
      <button
        type="button"
        onClick={() => {
          setAberto((v) => !v);
          if (!aberto) setBusca("");
        }}
        className={labelTrocar ? "fiscal-btn-primary text-sm !py-2" : "empresa-switcher-btn"}
        title="Trocar empresa emitente"
      >
        <Building2 className="h-4 w-4 shrink-0 text-[var(--brand)]" />
        <span className="min-w-0 truncate font-medium text-[#1a2e16]">{label}</span>
        <ChevronDown className="h-4 w-4 shrink-0 text-agro-muted" />
      </button>
      {painel}
    </div>
  );
}
