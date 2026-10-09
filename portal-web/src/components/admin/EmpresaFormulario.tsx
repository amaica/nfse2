"use client";

import { useCallback, useState } from "react";
import { Loader2, Plus, Trash2 } from "lucide-react";
import { type EmpresaDetalhe } from "@/lib/api";
import { consultarCep, consultarCnpjPessoa } from "@/lib/consulta-externa";
import { CRT_OPCOES, MODELO_DFE_OPCOES } from "@/lib/empresa-opcoes";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";
import { CODIGO_UF_IBGE } from "./uf-ibge";

export type EnderecoLinha = {
  id?: number;
  municipio: string;
  bairro: string;
  cep: string;
  inscricaoEstadual: string;
  codigoMunicipioIbge: string;
  uf: string;
  logradouro: string;
  numero: string;
  apelido: string;
  serieNfe: string;
  ultimoNumeroNfe: string;
  principal: boolean;
  ativo: boolean;
};

export type EmpresaFormData = {
  nome: string;
  cpfCnpj: string;
  crt: string;
  ambiente: string;
  tipoEmitente: string;
  baixarXml: boolean;
  email: string;
  modelo: string;
  senhaIntegracao: string;
  prefeitura: string;
  codigoMunicipioIbge: string;
  serieRps: string;
  ultimoNumeroNfse: string;
  enderecos: EnderecoLinha[];
};

export function serieNfePadrao(cpfCnpj: string): string {
  const doc = cpfCnpj.replace(/\D/g, "");
  return doc.length === 11 ? "921" : "1";
}

export function enderecoVazio(partial?: Partial<EnderecoLinha>): EnderecoLinha {
  return {
    municipio: "",
    bairro: "",
    cep: "",
    inscricaoEstadual: "",
    codigoMunicipioIbge: "",
    uf: "RS",
    logradouro: "",
    numero: "",
    apelido: "Matriz",
    serieNfe: partial?.serieNfe ?? "1",
    ultimoNumeroNfe: "0",
    principal: true,
    ativo: true,
    ...partial,
  };
}

export function detalheParaForm(det: EmpresaDetalhe): EmpresaFormData {
  const ends: EnderecoLinha[] =
    det.enderecos?.map((e) => ({
      id: e.id,
      municipio: e.municipio ?? "",
      bairro: e.bairro ?? "",
      cep: e.cep ?? "",
      inscricaoEstadual: e.inscricaoEstadual ?? "",
      codigoMunicipioIbge: e.codigoMunicipioIbge ?? "",
      uf: e.uf ?? "RS",
      logradouro: e.logradouro ?? "",
      numero: e.numero ?? "",
      apelido: e.apelido ?? "Matriz",
      serieNfe: e.serieNfe ?? "1",
      ultimoNumeroNfe: String(e.ultimoNumeroNfe ?? 0),
      principal: e.principal ?? false,
      ativo: e.ativo ?? true,
    })) ?? [];

  if (ends.length === 0) {
    ends.push(
      enderecoVazio({
        cep: det.endereco?.cep ?? "",
        bairro: det.endereco?.bairro ?? "",
        logradouro: det.endereco?.logradouro ?? "",
        numero: det.endereco?.numero ?? "",
        municipio: det.municipio ?? "",
        uf: det.uf ?? "RS",
        inscricaoEstadual: det.inscricaoEstadual ?? "",
        codigoMunicipioIbge: det.codigoMunicipioIbge ?? "",
        principal: true,
      }),
    );
  }

  return {
    nome: det.nome ?? "",
    cpfCnpj: det.cnpj ?? "",
    crt: det.optanteSimples ? "1" : "3",
    ambiente: det.ambiente ?? "homologacao",
    tipoEmitente: "normal",
    baixarXml: det.baixarXml === true,
    email: det.email ?? det.emailIntegracao ?? "",
    modelo: "NFE",
    senhaIntegracao: "",
    prefeitura: det.prefeitura ?? det.municipio ?? "",
    codigoMunicipioIbge: det.codigoMunicipioIbge ?? ends[0]?.codigoMunicipioIbge ?? "",
    serieRps: det.serieRps ?? "1",
    ultimoNumeroNfse: String(det.ultimoNumeroNfse ?? 0),
    enderecos: ends,
  };
}

export function formInicialVazio(): EmpresaFormData {
  return {
    nome: "",
    cpfCnpj: "",
    crt: "3",
    ambiente: "homologacao",
    tipoEmitente: "normal",
    baixarXml: false,
    email: "",
    modelo: "NFE",
    senhaIntegracao: "demo123",
    prefeitura: "",
    codigoMunicipioIbge: "",
    serieRps: "1",
    ultimoNumeroNfse: "0",
    enderecos: [enderecoVazio()],
  };
}

type Props = {
  titulo: string;
  form: EmpresaFormData;
  empresaId: number | null;
  carregando: boolean;
  certNome?: string;
  logoNome?: string;
  mensagem?: { tipo: "ok" | "erro"; texto: string } | null;
  onChange: (form: EmpresaFormData) => void;
  onSalvar: () => void;
  onVoltar: () => void;
  onNovo: () => void;
  onCertificado: (arquivo: File | null, senha: string) => void;
  onLogo: (arquivo: File | null) => void;
  onBaixarXmls?: () => void;
  baixandoXmls?: boolean;
};

const UFS = Object.keys(CODIGO_UF_IBGE).sort();

export function EmpresaFormulario({
  titulo,
  form,
  empresaId,
  carregando,
  certNome,
  logoNome,
  mensagem,
  onChange,
  onSalvar,
  onVoltar,
  onCertificado,
  onLogo,
  onBaixarXmls,
  baixandoXmls,
}: Props) {
  const [certArquivo, setCertArquivo] = useState<File | null>(null);
  const [certSenha, setCertSenha] = useState("");
  const [endAtivo, setEndAtivo] = useState(0);
  const [loadingCep, setLoadingCep] = useState(false);
  const [loadingCnpj, setLoadingCnpj] = useState(false);
  const [hint, setHint] = useState("");

  const idxEnd = Math.min(endAtivo, Math.max(0, form.enderecos.length - 1));
  const end = form.enderecos[idxEnd] ?? form.enderecos[0];

  function atualizarEndereco(idx: number, patch: Partial<EnderecoLinha>) {
    let next = form.enderecos.map((e, i) => (i === idx ? { ...e, ...patch } : e));
    if (patch.principal === true) {
      next = next.map((e, i) => ({ ...e, principal: i === idx }));
    }
    onChange({ ...form, enderecos: next });
  }

  function adicionarEndereco() {
    const novo = enderecoVazio({
      apelido: `Filial ${form.enderecos.length + 1}`,
      principal: false,
      serieNfe: serieNfePadrao(form.cpfCnpj),
    });
    onChange({ ...form, enderecos: [...form.enderecos, novo] });
    setEndAtivo(form.enderecos.length);
  }

  function removerEndereco(idx: number) {
    if (form.enderecos.length <= 1) return;
    const next = form.enderecos.filter((_, i) => i !== idx);
    if (!next.some((e) => e.principal) && next[0]) next[0].principal = true;
    onChange({ ...form, enderecos: next });
    setEndAtivo(Math.max(0, idx - 1));
  }

  const preencherCep = useCallback(
    async (idx: number, cepDigits: string) => {
      if (cepDigits.length !== 8) return;
      setLoadingCep(true);
      setHint("");
      try {
        const c = await consultarCep(cepDigits);
        onChange({
          ...form,
          prefeitura: form.prefeitura || c.localidade || form.prefeitura,
          enderecos: form.enderecos.map((e, i) =>
            i === idx
              ? {
                  ...e,
                  logradouro: c.logradouro || e.logradouro,
                  bairro: c.bairro || e.bairro,
                  municipio: c.localidade || e.municipio,
                  uf: c.uf || e.uf,
                  codigoMunicipioIbge: c.ibge || e.codigoMunicipioIbge,
                }
              : e,
          ),
        });
      } catch (e) {
        setHint(e instanceof Error ? e.message : "Falha ao consultar CEP");
      } finally {
        setLoadingCep(false);
      }
    },
    [form, onChange],
  );

  async function handleCnpjBlur() {
    if (empresaId != null) return;
    const doc = form.cpfCnpj.replace(/\D/g, "");
    if (doc.length !== 14) return;
    setLoadingCnpj(true);
    setHint("");
    try {
      const d = await consultarCnpjPessoa(doc);
      const ende = d.endereco ?? {};
      const primeiro = {
        ...form.enderecos[0],
        cep: (ende.cep ?? form.enderecos[0]?.cep ?? "").replace(/\D/g, ""),
        logradouro: ende.logradouro || form.enderecos[0]?.logradouro || "",
        numero: ende.numero || form.enderecos[0]?.numero || "",
        bairro: ende.bairro || form.enderecos[0]?.bairro || "",
        municipio: ende.municipio || form.enderecos[0]?.municipio || "",
        uf: (ende.uf || form.enderecos[0]?.uf || "RS").toUpperCase(),
        codigoMunicipioIbge:
          ende.codigoMunicipioIbge?.replace(/\D/g, "") ||
          d.codigoMunicipioIbge?.replace(/\D/g, "") ||
          form.enderecos[0]?.codigoMunicipioIbge ||
          "",
      };
      onChange({
        ...form,
        cpfCnpj: doc,
        nome: d.razaoSocial?.trim() || form.nome,
        email: d.email?.trim() || form.email,
        crt: d.optanteSimples ? "1" : form.crt,
        prefeitura: d.prefeitura || ende.municipio || form.prefeitura,
        codigoMunicipioIbge: primeiro.codigoMunicipioIbge,
        enderecos: [primeiro, ...form.enderecos.slice(1)],
      });
      const cepDigits = (ende.cep ?? "").replace(/\D/g, "");
      if (cepDigits.length === 8) await preencherCep(0, cepDigits);
    } catch (e) {
      setHint(e instanceof Error ? e.message : "Falha ao consultar CNPJ");
    } finally {
      setLoadingCnpj(false);
    }
  }

  return (
    <div className="fiscal-card">
      <div className="mb-8 flex items-center justify-between">
        <button type="button" className="text-sm text-stone-500 hover:text-stone-800" onClick={onVoltar}>
          Voltar
        </button>
        <button
          type="button"
          className="rounded-full bg-stone-900 px-4 py-2 text-sm text-white disabled:opacity-40"
          disabled={carregando}
          onClick={onSalvar}
        >
          Salvar
        </button>
      </div>
      <h1 className="mb-1 text-2xl font-semibold tracking-tight text-stone-900">
        {empresaId == null ? "Novo emitente" : form.nome || titulo}
      </h1>
      <p className="mb-8 text-sm text-stone-500">Nome, documento e endereço. O restante fica em ajustes.</p>

      {mensagem && (
        <p className={`erp-alert ${mensagem.tipo === "ok" ? "erp-alert--ok" : "erp-alert--error"}`}>
          {mensagem.texto}
        </p>
      )}
      {hint && <p className="erp-alert erp-alert--error">{hint}</p>}
      {(loadingCnpj || loadingCep) && (
        <p className="mb-3 flex items-center gap-2 text-sm text-stone-500">
          <Loader2 className="h-4 w-4 animate-spin" />
          {loadingCnpj ? "Buscando dados do CNPJ na Receita Federal…" : "Buscando endereço pelo CEP…"}
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm text-stone-500">Nome</span>
          <input
            className="fiscal-input"
            maxLength={255}
            value={form.nome}
            autoFocus={empresaId == null}
            onChange={(e) => onChange({ ...form, nome: e.target.value })}
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm text-stone-500">CPF ou CNPJ</span>
          <div className="flex gap-2">
            <input
              className="fiscal-input"
              value={form.cpfCnpj}
              readOnly={empresaId != null}
              placeholder="00.000.000/0000-00"
              onChange={(e) => {
                const doc = e.target.value.replace(/\D/g, "").slice(0, 14);
                const cpfCnpj =
                  doc.length <= 11
                    ? doc
                        .replace(/(\d{3})(\d)/, "$1.$2")
                        .replace(/(\d{3})(\d)/, "$1.$2")
                        .replace(/(\d{3})(\d{1,2})$/, "$1-$2")
                    : doc
                        .replace(/^(\d{2})(\d)/, "$1.$2")
                        .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
                        .replace(/\.(\d{3})(\d)/, ".$1/$2")
                        .replace(/(\d{4})(\d)/, "$1-$2");
                const seriePf = doc.length === 11 ? "921" : null;
                onChange({
                  ...form,
                  cpfCnpj,
                  enderecos: seriePf
                    ? form.enderecos.map((row) =>
                        row.serieNfe === "1" || !row.serieNfe ? { ...row, serieNfe: seriePf } : row,
                      )
                    : form.enderecos,
                });
              }}
              onBlur={() => void handleCnpjBlur()}
            />
            {empresaId == null && form.cpfCnpj.replace(/\D/g, "").length === 14 && (
              <button type="button" className="shrink-0 text-sm text-stone-500 underline" onClick={() => void handleCnpjBlur()}>
                Buscar
              </button>
            )}
          </div>
        </label>
        <label className="block">
          <span className="mb-1 block text-sm text-stone-500">E-mail</span>
          <input
            className="fiscal-input"
            type="email"
            placeholder="contato@empresa.com.br"
            value={form.email}
            onChange={(e) => onChange({ ...form, email: e.target.value })}
          />
        </label>
        <label className="block max-w-xs">
          <span className="mb-1 block text-sm text-stone-500">Ambiente</span>
          <select
            className="fiscal-input"
            value={form.ambiente}
            onChange={(e) => onChange({ ...form, ambiente: e.target.value })}
          >
            <option value="homologacao">Homologação</option>
            <option value="producao">Produção</option>
          </select>
        </label>
      </div>

      {end && (
        <div className="mt-10 space-y-4">
          <p className="text-sm text-stone-500">Endereço</p>
          <div className="grid grid-cols-3 gap-4">
            <label className="block">
              <span className="mb-1 block text-sm text-stone-500">CEP</span>
              <input
                className="fiscal-input"
                inputMode="numeric"
                maxLength={9}
                value={end.cep}
                onChange={(e) => atualizarEndereco(idxEnd, { cep: e.target.value.replace(/\D/g, "").slice(0, 8) })}
                onBlur={() => void preencherCep(idxEnd, end.cep.replace(/\D/g, ""))}
              />
            </label>
            <label className="col-span-2 block">
              <span className="mb-1 block text-sm text-stone-500">Rua</span>
              <input
                className="fiscal-input"
                value={end.logradouro}
                onChange={(e) => atualizarEndereco(idxEnd, { logradouro: e.target.value })}
              />
            </label>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <label className="block">
              <span className="mb-1 block text-sm text-stone-500">Número</span>
              <input
                className="fiscal-input"
                value={end.numero}
                onChange={(e) => atualizarEndereco(idxEnd, { numero: e.target.value })}
              />
            </label>
            <label className="col-span-2 block">
              <span className="mb-1 block text-sm text-stone-500">Bairro</span>
              <input
                className="fiscal-input"
                value={end.bairro}
                onChange={(e) => atualizarEndereco(idxEnd, { bairro: e.target.value })}
              />
            </label>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <label className="col-span-2 block">
              <span className="mb-1 block text-sm text-stone-500">Cidade</span>
              <input
                className="fiscal-input"
                value={end.municipio}
                onChange={(e) => atualizarEndereco(idxEnd, { municipio: e.target.value })}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-sm text-stone-500">UF</span>
              <select
                className="fiscal-input"
                value={end.uf}
                onChange={(e) => atualizarEndereco(idxEnd, { uf: e.target.value })}
              >
                {UFS.map((uf) => (
                  <option key={uf} value={uf}>
                    {uf}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>
      )}

      <details className="mt-10">
        <summary className="cursor-pointer text-sm text-stone-400">Ajustes da nota</summary>
      <div className="mt-4 space-y-4">
      {(
        <FiscalSection title="Ajustes">
          <FiscalRow>
            <FiscalField label="Regime">
              <select
                className="fiscal-input"
                value={form.crt}
                onChange={(e) => onChange({ ...form, crt: e.target.value })}
              >
                {CRT_OPCOES.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </FiscalField>
            <FiscalField label="Modelo">
              <select
                className="fiscal-input"
                value={form.modelo}
                onChange={(e) => onChange({ ...form, modelo: e.target.value })}
              >
                {MODELO_DFE_OPCOES.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </FiscalField>
          </FiscalRow>
          <FiscalRow>
            {empresaId == null && (
              <FiscalField label="Senha do primeiro acesso">
                <input
                  className="fiscal-input"
                  type="password"
                  value={form.senhaIntegracao}
                  onChange={(e) => onChange({ ...form, senhaIntegracao: e.target.value })}
                  autoComplete="new-password"
                />
                <span className="erp-hint">Quem for entrar no portal usa o e-mail acima e esta senha.</span>
              </FiscalField>
            )}
          </FiscalRow>
        </FiscalSection>
      )}

      {end && (
        <FiscalSection title="Numeração e inscrição">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            {form.enderecos.map((item, i) => (
              <button
                key={`${item.id ?? "n"}-${i}`}
                type="button"
                className={`erp-tab ${i === idxEnd ? "active" : ""}`}
                onClick={() => setEndAtivo(i)}
              >
                {item.apelido || `Endereço ${i + 1}`}
                {item.principal ? " · principal" : ""}
              </button>
            ))}
            <button type="button" className="fiscal-btn-secondary" onClick={adicionarEndereco}>
              <Plus className="h-4 w-4" /> Novo endereço
            </button>
          </div>
          <FiscalRow>
            <FiscalField label="Apelido">
              <input
                className="fiscal-input"
                value={end.apelido}
                onChange={(e) => atualizarEndereco(idxEnd, { apelido: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Uso deste endereço">
              <label className="flex items-center gap-2 py-2 text-sm text-slate-700">
                <input
                  type="checkbox"
                  checked={end.principal}
                  onChange={(e) => atualizarEndereco(idxEnd, { principal: e.target.checked })}
                />
                Usar este endereço nas notas
              </label>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input
                  type="checkbox"
                  checked={end.ativo}
                  onChange={(e) => atualizarEndereco(idxEnd, { ativo: e.target.checked })}
                />
                Endereço ativo
              </label>
            </FiscalField>
          </FiscalRow>
          <FiscalRow>
            <FiscalField label="Código do município">
              <input
                className="fiscal-input fiscal-input--mono"
                inputMode="numeric"
                maxLength={7}
                value={end.codigoMunicipioIbge}
                onChange={(e) =>
                  atualizarEndereco(idxEnd, { codigoMunicipioIbge: e.target.value.replace(/\D/g, "").slice(0, 7) })
                }
              />
              <span className="erp-hint">O CEP preenche sozinho. Só altere se a busca não achar.</span>
            </FiscalField>
          </FiscalRow>
          <FiscalRow>
            <FiscalField label="Inscrição estadual">
              <input
                className="fiscal-input fiscal-input--mono"
                value={end.inscricaoEstadual}
                onChange={(e) => atualizarEndereco(idxEnd, { inscricaoEstadual: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Série da NF-e">
              <input
                className="fiscal-input fiscal-input--mono"
                value={end.serieNfe}
                onChange={(e) => atualizarEndereco(idxEnd, { serieNfe: e.target.value })}
              />
              <span className="erp-hint">Pessoa física costuma usar 921. Empresa, em geral, 1.</span>
            </FiscalField>
            <FiscalField label="Última NF-e já emitida">
              <input
                className="fiscal-input fiscal-input--mono"
                inputMode="numeric"
                value={end.ultimoNumeroNfe}
                onChange={(e) => atualizarEndereco(idxEnd, { ultimoNumeroNfe: e.target.value.replace(/\D/g, "") })}
              />
              <span className="erp-hint">A próxima nota sai com o número seguinte.</span>
            </FiscalField>
          </FiscalRow>
          {form.enderecos.length > 1 && (
            <button type="button" className="fiscal-btn-secondary mt-2" onClick={() => removerEndereco(idxEnd)}>
              <Trash2 className="h-4 w-4" /> Remover este endereço
            </button>
          )}
        </FiscalSection>
      )}

      {(
        <FiscalSection title="Certificado">
          <FiscalRow>
            <FiscalField label="Arquivo .pfx">
              <input
                className="fiscal-input"
                type="file"
                accept=".pfx,.p12"
                onChange={(e) => {
                  const f = e.target.files?.[0] ?? null;
                  setCertArquivo(f);
                  onCertificado(f, certSenha);
                }}
              />
              {certNome && !certArquivo && <span className="erp-hint">{certNome}</span>}
            </FiscalField>
            <FiscalField label="Senha do certificado">
              <input
                className="fiscal-input"
                type="password"
                value={certSenha}
                placeholder="Senha do arquivo PFX"
                onChange={(e) => {
                  setCertSenha(e.target.value);
                  onCertificado(certArquivo, e.target.value);
                }}
              />
            </FiscalField>
          </FiscalRow>
          <FiscalField label="Logo da nota (PNG ou JPG)">
            <input
              className="fiscal-input"
              type="file"
              accept="image/png,image/jpeg,image/gif"
              onChange={(e) => onLogo(e.target.files?.[0] ?? null)}
            />
            {logoNome && <span className="erp-hint">{logoNome}</span>}
          </FiscalField>
        </FiscalSection>
      )}

      {(
        <FiscalSection title="Notas de serviço e notas recebidas">
          <p className="mb-3 text-sm text-slate-600">
            A série e o último número da NF-e de produto ficam no endereço. Aqui é a nota de serviço e a busca das notas que outros emitiram contra este CNPJ.
          </p>
          <FiscalRow>
            <FiscalField label="Prefeitura do serviço">
              <input
                className="fiscal-input"
                value={form.prefeitura}
                onChange={(e) => onChange({ ...form, prefeitura: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Série da nota de serviço">
              <input
                className="fiscal-input fiscal-input--mono"
                value={form.serieRps}
                onChange={(e) => onChange({ ...form, serieRps: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Última nota de serviço emitida">
              <input
                className="fiscal-input fiscal-input--mono"
                inputMode="numeric"
                value={form.ultimoNumeroNfse}
                onChange={(e) => onChange({ ...form, ultimoNumeroNfse: e.target.value.replace(/\D/g, "") })}
              />
            </FiscalField>
          </FiscalRow>
          <label className="flex items-start gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              className="mt-1"
              checked={form.baixarXml}
              onChange={(e) => onChange({ ...form, baixarXml: e.target.checked })}
            />
            <span>
              Buscar sozinho as notas que outras empresas emitiram contra este CNPJ.
              <span className="mt-1 block text-slate-500">Elas aparecem em NF-e recebidas e no livro caixa.</span>
            </span>
          </label>
          {empresaId != null && form.baixarXml && onBaixarXmls && (
            <button type="button" className="fiscal-btn-secondary mt-3" disabled={baixandoXmls} onClick={onBaixarXmls}>
              {baixandoXmls ? "Baixando XMLs…" : "Baixar XMLs agora"}
            </button>
          )}
        </FiscalSection>
      )}

      </div>
      </details>
    </div>
  );
}
