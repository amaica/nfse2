import { formatarCnpjCpf } from "@/lib/api";

export type DanfsePrintData = {
  chave: string;
  status?: string | null;
  numeroRps?: string | null;
  serieRps?: string | null;
  prestadorCnpj?: string | null;
  prestadorNome?: string | null;
  prestadorIm?: string | null;
  tomadorDocumento?: string | null;
  tomadorNome?: string | null;
  tomadorEmail?: string | null;
  tomadorTelefone?: string | null;
  descricaoServico?: string | null;
  itemListaServico?: string | null;
  codigoTributacaoMunicipio?: string | null;
  municipioIbge?: string | null;
  valorServicos?: number | null;
  valorIss?: number | null;
  aliquotaIss?: number | null;
  observacoes?: string | null;
  criadoEm?: string | null;
};

function esc(v?: string | number | null) {
  return String(v ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function money(v?: number | null) {
  if (v == null || !Number.isFinite(Number(v))) return "R$ 0,00";
  return Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function fmtData(iso?: string | null) {
  if (!iso) return new Date().toLocaleString("pt-BR");
  try {
    return new Date(iso).toLocaleString("pt-BR");
  } catch {
    return iso;
  }
}

/** Gera HTML da NFS-e (DANFSe simplificado) e abre diálogo de impressão/PDF. */
export function imprimirDanfseNfse(nota: DanfsePrintData): void {
  const rps = `${nota.numeroRps || "—"}${nota.serieRps ? `/${nota.serieRps}` : ""}`;
  const html = `<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8"/>
<title>NFS-e ${esc(rps)} · ${esc(nota.chave)}</title>
<style>
  @page { size: A4; margin: 12mm; }
  * { box-sizing: border-box; }
  body { margin: 0; font-family: "Segoe UI", Arial, sans-serif; color: #122018; background: #eef5f0; }
  .sheet { max-width: 210mm; margin: 0 auto; background: #fff; border: 1px solid #c5decc; box-shadow: 0 10px 36px rgba(22, 80, 40, 0.08); }
  .top { display: grid; grid-template-columns: 1.4fr 1fr; border-bottom: 3px solid #16c15e; }
  .brand { padding: 18px 22px; background: linear-gradient(135deg, #0f2918 0%, #1b5e2a 55%, #16c15e 140%); color: #fff; }
  .brand .doc { font-size: 11px; letter-spacing: 0.16em; text-transform: uppercase; opacity: 0.85; }
  .brand h1 { margin: 6px 0 0; font-size: 20px; font-weight: 700; }
  .brand .sub { margin-top: 4px; font-size: 12px; opacity: 0.9; }
  .rps { padding: 16px 20px; text-align: right; background: #f4fbf6; }
  .rps .lbl { font-size: 11px; color: #5f7a63; text-transform: uppercase; letter-spacing: 0.08em; }
  .rps .num { font-size: 28px; font-weight: 800; color: #0f2918; line-height: 1.1; margin-top: 4px; }
  .rps .st { display: inline-block; margin-top: 8px; padding: 3px 10px; border-radius: 999px; background: #dcfce7; color: #166534; font-size: 11px; font-weight: 700; }
  .body { padding: 18px 22px 24px; }
  .row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 14px; }
  .card { border: 1px solid #d7ebd9; border-radius: 12px; padding: 12px 14px; background: #fafdfb; }
  .card.full { grid-column: 1 / -1; }
  .card h2 { margin: 0 0 8px; font-size: 11px; letter-spacing: 0.1em; text-transform: uppercase; color: #2e7d32; }
  .card .name { font-size: 15px; font-weight: 700; color: #122018; }
  .card .meta { margin-top: 4px; font-size: 12px; color: #4a654f; }
  .kv { display: grid; grid-template-columns: 140px 1fr; gap: 4px 10px; font-size: 13px; }
  .kv dt { color: #6b8570; }
  .kv dd { margin: 0; font-weight: 600; color: #1a2e1a; }
  .desc { white-space: pre-wrap; font-size: 13px; line-height: 1.5; color: #243828; margin: 0; }
  .valores { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 4px; }
  .val-box { border-radius: 12px; border: 1px solid #c8e6c9; background: linear-gradient(180deg, #f0fdf4, #fff); padding: 12px; text-align: center; }
  .val-box .lbl { font-size: 11px; color: #5f7a63; text-transform: uppercase; }
  .val-box .v { margin-top: 4px; font-size: 18px; font-weight: 800; color: #0f2918; }
  .chave { margin-top: 16px; padding: 10px 12px; border-radius: 10px; background: #0f2918; color: #d9f5e3; font-family: ui-monospace, monospace; font-size: 11px; word-break: break-all; text-align: center; }
  .foot { margin-top: 18px; text-align: center; font-size: 11px; color: #7a9480; }
  @media print { body { background: #fff; } .sheet { box-shadow: none; border: none; } }
</style>
</head>
<body>
  <div class="sheet">
    <div class="top">
      <div class="brand">
        <div class="doc">Documento auxiliar · NFS-e</div>
        <h1>${esc(nota.prestadorNome || "Prestador")}</h1>
        <div class="sub">CNPJ ${esc(formatarCnpjCpf(nota.prestadorCnpj || ""))}${nota.prestadorIm ? ` · IM ${esc(nota.prestadorIm)}` : ""}</div>
      </div>
      <div class="rps">
        <div class="lbl">RPS / Série</div>
        <div class="num">${esc(rps)}</div>
        <div class="st">${esc(nota.status || "EMITIDA")}</div>
      </div>
    </div>
    <div class="body">
      <div class="row">
        <div class="card"><h2>Prestador</h2><div class="name">${esc(nota.prestadorNome || "—")}</div><div class="meta">${esc(formatarCnpjCpf(nota.prestadorCnpj || ""))}</div></div>
        <div class="card"><h2>Tomador</h2><div class="name">${esc(nota.tomadorNome || "—")}</div><div class="meta">${esc(formatarCnpjCpf(nota.tomadorDocumento || ""))}</div></div>
      </div>
      <div class="card full" style="margin-bottom:14px">
        <h2>Serviço prestado</h2>
        <dl class="kv">
          <dt>Item LC 116</dt><dd>${esc(nota.itemListaServico || "—")}</dd>
          <dt>Cód. município</dt><dd>${esc(nota.codigoTributacaoMunicipio || "—")}</dd>
          <dt>Município IBGE</dt><dd>${esc(nota.municipioIbge || "—")}</dd>
          <dt>Emissão</dt><dd>${esc(fmtData(nota.criadoEm))}</dd>
        </dl>
        <p class="desc" style="margin-top:10px">${esc(nota.descricaoServico || "—")}</p>
        ${nota.observacoes ? `<p class="desc" style="margin-top:8px;color:#5f7a63"><strong>Obs.:</strong> ${esc(nota.observacoes)}</p>` : ""}
      </div>
      <div class="valores">
        <div class="val-box"><div class="lbl">Valor dos serviços</div><div class="v">${esc(money(nota.valorServicos))}</div></div>
        <div class="val-box"><div class="lbl">Alíquota ISS</div><div class="v">${esc(nota.aliquotaIss != null ? Number(nota.aliquotaIss).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 4 }) + "%" : "—")}</div></div>
        <div class="val-box"><div class="lbl">Valor ISS</div><div class="v">${esc(money(nota.valorIss))}</div></div>
      </div>
      <div class="chave">Chave ${esc(nota.chave)}</div>
      <div class="foot">AgroNota · Imprima ou salve como PDF · ${esc(new Date().toLocaleString("pt-BR"))}</div>
    </div>
  </div>
  <script>window.onload = function () { window.print(); };</script>
</body>
</html>`;

  const blob = new Blob([html], { type: "text/html;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const w = window.open(url, "_blank", "width=920,height=1100");
  if (!w) {
    URL.revokeObjectURL(url);
    throw new Error("Permita pop-ups para gerar o PDF da NFS-e.");
  }
  w.focus();
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
