"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Calculator,
  ExternalLink,
  FileText,
  Loader2,
  Plus,
  Receipt,
} from "lucide-react";
import { formatarCnpjCpf } from "@/lib/api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { menuAllowsHref } from "@/lib/menu/tree";
import { useMenus } from "@/lib/menu/useMenus";
import { useAppSession } from "@/hooks/useAppSession";
import { AssinaturaBanner } from "@/components/conta/AssinaturaBanner";
import { SetupChecklist } from "@/components/conta/SetupChecklist";

type MesPonto = { mes: number; receitas: number; despesas: number };

type NotaResumo = {
  data?: string;
  createdAt?: string;
  origem?: string;
  numero?: string | number;
  chave?: string;
  contraparte?: string;
  valor?: number;
  statusProtocolo?: string;
  statusLabel?: string;
};

type MonitorItem = {
  id: number;
  nomeEmitente?: string;
  cnpjEmitente?: string;
  valor?: number;
  dataEmissao?: string;
  createdAt?: string;
  numero?: string;
};

type HomeData = {
  ano: number;
  receitas: number;
  despesas: number;
  resultado: number;
  mensal: MesPonto[];
  ultimasNotas: NotaResumo[];
  monitorFiscal: MonitorItem[];
  monitorTotal: number;
};

const MESES = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

function moeda(v: number | undefined): string {
  return (Number(v) || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function statusBadge(nota: NotaResumo): { label: string; cls: string } {
  const s = String(nota.statusProtocolo ?? "").trim();
  if (s === "100" || nota.statusLabel === "Autorizada") {
    return { label: "Autorizada", cls: "home-badge--ok" };
  }
  if (!s || s === "0") {
    return { label: "Não enviada", cls: "home-badge--muted" };
  }
  return { label: "Revisar", cls: "home-badge--warn" };
}

function relativo(iso?: string): string {
  if (!iso) return "—";
  const d = new Date(iso.length === 10 ? `${iso}T12:00:00` : iso);
  if (Number.isNaN(d.getTime())) return iso;
  const diff = Date.now() - d.getTime();
  const h = Math.floor(diff / 3_600_000);
  if (h < 1) return "Agora";
  if (h < 24) return `${h} hora${h > 1 ? "s" : ""}`;
  const dias = Math.floor(h / 24);
  if (dias < 30) return `${dias} dia${dias > 1 ? "s" : ""}`;
  return d.toLocaleDateString("pt-BR");
}

function AreaChart({ mensal }: { mensal: MesPonto[] }) {
  const w = 640;
  const h = 168;
  const padX = 8;
  const padY = 16;
  const max = Math.max(
    1,
    ...mensal.map((m) => Math.max(Number(m.receitas) || 0, Number(m.despesas) || 0)),
  );

  const xs = mensal.map((_, i) => padX + (i * (w - padX * 2)) / Math.max(mensal.length - 1, 1));
  const yOf = (v: number) => padY + (1 - v / max) * (h - padY * 2);

  const linePath = (key: "receitas" | "despesas") =>
    mensal
      .map((m, i) => `${i === 0 ? "M" : "L"}${xs[i].toFixed(1)},${yOf(Number(m[key]) || 0).toFixed(1)}`)
      .join(" ");

  const areaReceitas = `${linePath("receitas")} L${xs[xs.length - 1].toFixed(1)},${h - padY} L${xs[0].toFixed(1)},${h - padY} Z`;

  return (
    <div className="home-chart" aria-hidden>
      <svg viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none">
        <defs>
          <linearGradient id="homeRecFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="rgba(61,107,47,0.28)" />
            <stop offset="100%" stopColor="rgba(61,107,47,0.02)" />
          </linearGradient>
        </defs>
        {[0.25, 0.5, 0.75].map((t) => (
          <line
            key={t}
            x1={padX}
            x2={w - padX}
            y1={padY + t * (h - padY * 2)}
            y2={padY + t * (h - padY * 2)}
            stroke="#d7e2d0"
            strokeDasharray="3 4"
          />
        ))}
        <path d={areaReceitas} fill="url(#homeRecFill)" />
        <path d={linePath("receitas")} fill="none" stroke="#3d6b2f" strokeWidth="2.25" strokeLinecap="round" />
        <path d={linePath("despesas")} fill="none" stroke="#c2410c" strokeWidth="2" strokeLinecap="round" strokeOpacity="0.85" />
        {mensal.map((m, i) => {
          const r = Number(m.receitas) || 0;
          if (r <= 0) return null;
          return <circle key={`r-${i}`} cx={xs[i]} cy={yOf(r)} r="2.8" fill="#3d6b2f" />;
        })}
      </svg>
      <div className="home-chart__months">
        {MESES.map((m) => (
          <span key={m}>{m}</span>
        ))}
      </div>
    </div>
  );
}

export function HomeDashboard() {
  const { session, ready } = useAppSession();
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const { menuTree, loading: menuLoading } = useMenus();
  const [ano, setAno] = useState(new Date().getFullYear());
  const [data, setData] = useState<HomeData | null>(null);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");

  const anos = useMemo(() => {
    const y = new Date().getFullYear();
    return [y, y - 1, y - 2];
  }, []);

  const podeNfe = menuAllowsHref(menuTree, "/nfe/emissao");
  const podeNfse = menuAllowsHref(menuTree, "/nfse/emissao");
  const podeIr = menuAllowsHref(menuTree, "/simulado-ir");
  const podeEntradas = menuAllowsHref(menuTree, "/nfe/notas-entrada");
  const podeEmitidas = menuAllowsHref(menuTree, "/nfe/notas-emitidas");

  const carregar = useCallback(async () => {
    const token = getAppToken();
    if (!token || !empresaId) {
      setData(null);
      return;
    }
    setLoading(true);
    setErro("");
    try {
      const res = await fetch(`${apiBaseUrl()}/api/painel/home?ano=${ano}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        throw new Error((await res.json().catch(() => ({})) as { erro?: string }).erro ?? res.statusText);
      }
      setData((await res.json()) as HomeData);
    } catch (e) {
      setData(null);
      setErro(e instanceof Error ? e.message : "Falha ao carregar painel");
    } finally {
      setLoading(false);
    }
  }, [ano, empresaId]);

  useEffect(() => {
    if (!ready) return;
    void carregar();
  }, [ready, carregar]);

  const acoes = useMemo(() => {
    if (menuLoading && menuTree.length === 0) return [];
    return [
      podeNfe
        ? {
            href: "/nfe/emissao",
            label: "Emitir NF-e",
            desc: "Produto — venda, remessa, devolução",
            icon: FileText,
          }
        : null,
      podeNfse
        ? {
            href: "/nfse/emissao",
            label: "Emitir NFS-e",
            desc: "Serviço — prefeitura / SEFIN",
            icon: Receipt,
          }
        : null,
    ].filter(Boolean) as { href: string; label: string; desc: string; icon: typeof FileText }[];
  }, [menuLoading, menuTree, podeNfe, podeNfse]);

  const primeiroNome = session?.nome?.split(" ")[0] ?? "olá";

  return (
    <div className="home-dash animate-in">
      <div className="home-dash__intro">
        <div>
          <p className="home-dash__eyebrow">Visão geral</p>
          <h1 className="home-dash__title">Olá, {primeiroNome}</h1>
          <p className="home-dash__subtitle">
            {empresaNome ? (
              <>
                Emitente <strong>{empresaNome}</strong>
                {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
              </>
            ) : (
              "Selecione um emitente na barra superior para começar."
            )}
          </p>
        </div>
      </div>

      <AssinaturaBanner />

      <SetupChecklist />

      {acoes.length > 0 ? (
        <section className="home-actions" aria-label="Ações rápidas">
          {acoes.map((a) => (
            <Link key={a.href} href={a.href} className="home-action">
              <span className="home-action__icon">
                <a.icon className="h-4 w-4" />
              </span>
              <span>
                <p className="home-action__label">{a.label}</p>
                <p className="home-action__desc">{a.desc}</p>
              </span>
            </Link>
          ))}
        </section>
      ) : null}

      <div className="home-grid">
        <section className="home-card home-card__pad">
          <div className="home-card__head">
            <div>
              <h2 className="home-card__title">Resumo do ano</h2>
              <p className="home-card__meta">Receitas e despesas a partir das notas do emitente</p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <select
                className="home-select"
                value={ano}
                onChange={(e) => setAno(Number(e.target.value))}
                aria-label="Ano"
              >
                {anos.map((y) => (
                  <option key={y} value={y}>
                    {y}
                  </option>
                ))}
              </select>
              {podeNfe ? (
                <Link href="/nfe/emissao" className="home-btn home-btn--primary">
                  <Plus className="h-3.5 w-3.5" />
                  Emitir nota
                </Link>
              ) : null}
            </div>
          </div>

          {loading && !data ? (
            <div className="home-empty flex items-center justify-center gap-2">
              <Loader2 className="h-4 w-4 animate-spin" />
              Carregando resumo…
            </div>
          ) : erro && !data ? (
            <div className="home-empty text-red-600">{erro}</div>
          ) : (
            <>
              <div className="home-resumo__body">
                <div className="home-kpi">
                  <div className="home-kpi__item">
                    <span className="home-kpi__label">Receitas no Livro Caixa</span>
                    <span className="home-kpi__value home-kpi__value--in">{moeda(data?.receitas)}</span>
                  </div>
                  <div className="home-kpi__item">
                    <span className="home-kpi__label">Despesas no Livro Caixa</span>
                    <span className="home-kpi__value home-kpi__value--out">{moeda(data?.despesas)}</span>
                  </div>
                  <div className="home-kpi__item">
                    <span className="home-kpi__label">Resultado Livro Caixa</span>
                    <span className="home-kpi__value home-kpi__value--net">{moeda(data?.resultado)}</span>
                  </div>
                </div>
                <AreaChart mensal={data?.mensal ?? Array.from({ length: 12 }, (_, i) => ({ mes: i + 1, receitas: 0, despesas: 0 }))} />
              </div>

              {podeIr ? (
                <Link href="/simulado-ir" className="home-ir-link">
                  <span className="home-ir-link__icon">
                    <Calculator className="h-3.5 w-3.5" />
                  </span>
                  Simule o Imposto de Renda
                </Link>
              ) : null}
            </>
          )}
        </section>

        <aside className="home-card home-card__pad">
          <div className="home-card__head">
            <div>
              <h2 className="home-card__title">Monitor Fiscal</h2>
              <p className="home-card__meta">
                {Number(data?.monitorTotal ?? 0).toLocaleString("pt-BR")} documentos DF-e no emitente
              </p>
            </div>
          </div>
          {!data?.monitorFiscal?.length ? (
            <p className="home-empty">Nenhuma nota recebida (DF-e) ainda.</p>
          ) : (
            <ul className="home-monitor__list">
              {data.monitorFiscal.map((item) => (
                <li key={item.id} className="home-monitor__row">
                  <span className="home-monitor__dot" />
                  <div className="min-w-0">
                    <p className="home-monitor__name" title={item.nomeEmitente}>
                      {item.nomeEmitente || "Emitente não identificado"}
                    </p>
                    <p className="home-monitor__when">
                      {relativo(item.dataEmissao || item.createdAt)}
                      {item.numero ? ` · nº ${item.numero}` : ""}
                    </p>
                  </div>
                  <span className="home-monitor__valor">{moeda(item.valor)}</span>
                </li>
              ))}
            </ul>
          )}
          {podeEntradas ? (
            <div className="home-monitor__footer">
              <Link href="/nfe/notas-entrada">
                Gerenciar Monitor Fiscal
                <ExternalLink className="h-3.5 w-3.5" />
              </Link>
            </div>
          ) : null}
        </aside>
      </div>

      <section className="home-card home-card__pad">
        <div className="home-card__head">
          <div>
            <h2 className="home-card__title">Últimas notas geradas</h2>
            <p className="home-card__meta">Emitidas pelo emitente ativo no período</p>
          </div>
          {podeNfe ? (
            <Link href="/nfe/emissao" className="home-btn home-btn--primary">
              <Plus className="h-3.5 w-3.5" />
              Emitir Nota Fiscal
            </Link>
          ) : null}
        </div>

        {!data?.ultimasNotas?.length ? (
          <p className="home-empty">Nenhuma nota emitida neste ano ainda.</p>
        ) : (
          <div className="home-table-wrap">
            <table className="home-table">
              <thead>
                <tr>
                  <th>Quando</th>
                  <th>Número</th>
                  <th>Destinatário</th>
                  <th>Valor</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.ultimasNotas.map((n, i) => {
                  const badge = statusBadge(n);
                  return (
                    <tr key={`${n.chave ?? n.numero ?? i}`}>
                      <td className="home-table__muted">{relativo(n.data || n.createdAt)}</td>
                      <td className="home-table__muted">{n.numero ?? "—"}</td>
                      <td>{n.contraparte || "—"}</td>
                      <td className="home-table__valor">{n.valor != null ? `+ ${moeda(n.valor)}` : "—"}</td>
                      <td>
                        <span className={`home-badge ${badge.cls}`}>{badge.label}</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {podeEmitidas ? (
          <div className="home-monitor__footer">
            <Link href="/nfe/notas-emitidas">
              Ver todas as NF-e
              <ExternalLink className="h-3.5 w-3.5" />
            </Link>
          </div>
        ) : null}
      </section>
    </div>
  );
}
