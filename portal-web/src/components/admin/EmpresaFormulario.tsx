"use client";

import { useCallback, useState } from "react";
import { Building2, Loader2, Plus, Trash2 } from "lucide-react";
import { type EmpresaDetalhe } from "@/lib/api";
import { consultarCep, consultarCnpjPessoa } from "@/lib/consulta-externa";
import { AMBIENTE_OPCOES, CRT_OPCOES, MODELO_DFE_OPCOES } from "@/lib/empresa-opcoes";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";
import { IntegracaoErpPanel } from "@/components/conta/IntegracaoErpPanel";
import { CODIGO_UF_IBGE, codigoUfIbge } from "./uf-ibge";

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

export type EmbedInfo = {
  cnpj?: string;
  emailIntegracao?: string;
  embedUrlCnpj?: string;
  embedUrlCnpjComSenha?: string;
  embedUrlEmail?: string;
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

type TabId = "ident" | "endereco" | "docs" | "nfse" | "erp";

type Props = {
  titulo: string;
  form: EmpresaFormData;
  empresaId: number | null;
  carregando: boolean;
  certNome?: string;
  logoNome?: string;
  mensagem?: { tipo: "ok" | "erro"; texto: string } | null;
  embedInfo?: EmbedInfo | null;
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
  embedInfo,
  onChange,
  onSalvar,
  onVoltar,
  onNovo,
  onCertificado,
  onLogo,
  onBaixarXmls,
  baixandoXmls,
}: Props) {
  const [tab, setTab] = useState<TabId>("ident");
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
    setTab("endereco");
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
      <FiscalDetailToolbar
        title={titulo}
        icon={<Building2 className="h-5 w-5 text-slate-500" />}
        onVoltar={onVoltar}
        onNovo={onNovo}
        onCancelar={onVoltar}
        onSalvar={onSalvar}
        saveDisabled={carregando}
      />

      {mensagem && (
        <p className={`erp-alert ${mensagem.tipo === "ok" ? "erp-alert--ok" : "erp-alert--error"}`}>
          {mensagem.texto}
        </p>
      )}
      {hint && <p className="erp-alert erp-alert--error">{hint}</p>}
      {(loadingCnpj || loadingCep) && (
        <p className="mb-3 flex items-center gap-2 text-sm text-[var(--primary-700)]">
          <Loader2 className="h-4 w-4 animate-spin" />
          {loadingCnpj ? "Buscando dados do CNPJ na Receita Federal…" : "Buscando endereço pelo CEP…"}
        </p>
      )}

      <div className="erp-master">
        <FiscalField label="Razão social *" className="erp-span-2">
          <input
            className="fiscal-input"
            maxLength={255}
            value={form.nome}
            autoFocus={empresaId == null}
            onChange={(e) => onChange({ ...form, nome: e.target.value })}
          />
        </FiscalField>
        <FiscalField label="CPF / CNPJ *">
          <input
            className="fiscal-input fiscal-input--mono"
            value={form.cpfCnpj}
            readOnly={empresaId != null}
            placeholder="11 ou 14 dígitos"
            onChange={(e) => {
              const cpfCnpj = e.target.value;
              const doc = cpfCnpj.replace(/\D/g, "");
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
        </FiscalField>
      </div>

      <div className="erp-tabs" role="tablist">
        <button type="button" className={`erp-tab ${tab === "ident" ? "active" : ""}`} onClick={() => setTab("ident")}>
          Identificação
        </button>
        <button
          type="button"
          className={`erp-tab ${tab === "endereco" ? "active" : ""}`}
          onClick={() => setTab("endereco")}
        >
          Endereços
        </button>
        <button type="button" className={`erp-tab ${tab === "docs" ? "active" : ""}`} onClick={() => setTab("docs")}>
          Certificado e logo
        </button>
        <button type="button" className={`erp-tab ${tab === "nfse" ? "active" : ""}`} onClick={() => setTab("nfse")}>
          Numeração
        </button>
        {empresaId != null && embedInfo && (
          <button type="button" className={`erp-tab ${tab === "erp" ? "active" : ""}`} onClick={() => setTab("erp")}>
            Integração ERP
          </button>
        )}
      </div>

      {tab === "ident" && (
        <FiscalSection title="Dados do emitente">
          <FiscalRow>
            <FiscalField label="CRT — regime tributário">
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
            <FiscalField label="Ambiente SEFAZ">
              <select
                className="fiscal-input"
                value={form.ambiente}
                onChange={(e) => onChange({ ...form, ambiente: e.target.value })}
              >
                {AMBIENTE_OPCOES.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </FiscalField>
            <FiscalField label="Modelo padrão">
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
            <FiscalField label="E-mail">
              <input
                className="fiscal-input"
                type="email"
                value={form.email}
                onChange={(e) => onChange({ ...form, email: e.target.value })}
              />
            </FiscalField>
            {empresaId == null && (
              <FiscalField label="Senha de acesso ao portal">
                <input
                  className="fiscal-input"
                  type="password"
                  value={form.senhaIntegracao}
                  onChange={(e) => onChange({ ...form, senhaIntegracao: e.target.value })}
                  autoComplete="new-password"
                />
              </FiscalField>
            )}
          </FiscalRow>
        </FiscalSection>
      )}

      {tab === "endereco" && end && (
        <FiscalSection title="Estabelecimentos">
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
            <FiscalField label="Endereço principal">
              <select
                className="fiscal-input"
                value={end.principal ? "S" : "N"}
                onChange={(e) => atualizarEndereco(idxEnd, { principal: e.target.value === "S" })}
              >
                <option value="N">Não</option>
                <option value="S">Sim</option>
              </select>
            </FiscalField>
            <FiscalField label="Situação">
              <select
                className="fiscal-input"
                value={end.ativo ? "S" : "N"}
                onChange={(e) => atualizarEndereco(idxEnd, { ativo: e.target.value === "S" })}
              >
                <option value="S">Ativo</option>
                <option value="N">Inativo</option>
              </select>
            </FiscalField>
          </FiscalRow>
          <FiscalRow>
            <FiscalField label="CEP">
              <input
                className="fiscal-input fiscal-input--mono"
                inputMode="numeric"
                maxLength={9}
                value={end.cep}
                onChange={(e) => atualizarEndereco(idxEnd, { cep: e.target.value.replace(/\D/g, "").slice(0, 8) })}
                onBlur={() => void preencherCep(idxEnd, end.cep.replace(/\D/g, ""))}
              />
            </FiscalField>
            <FiscalField label="Logradouro" className="erp-span-2">
              <input
                className="fiscal-input"
                value={end.logradouro}
                onChange={(e) => atualizarEndereco(idxEnd, { logradouro: e.target.value })}
              />
            </FiscalField>
          </FiscalRow>
          <FiscalRow>
            <FiscalField label="Número">
              <input
                className="fiscal-input"
                value={end.numero}
                onChange={(e) => atualizarEndereco(idxEnd, { numero: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Bairro">
              <input
                className="fiscal-input"
                value={end.bairro}
                onChange={(e) => atualizarEndereco(idxEnd, { bairro: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Município">
              <input
                className="fiscal-input"
                value={end.municipio}
                onChange={(e) => atualizarEndereco(idxEnd, { municipio: e.target.value })}
              />
            </FiscalField>
          </FiscalRow>
          <FiscalRow>
            <FiscalField label="UF">
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
            </FiscalField>
            <FiscalField label="Código IBGE do município">
              <input
                className="fiscal-input fiscal-input--mono"
                inputMode="numeric"
                maxLength={7}
                value={end.codigoMunicipioIbge}
                onChange={(e) =>
                  atualizarEndereco(idxEnd, { codigoMunicipioIbge: e.target.value.replace(/\D/g, "").slice(0, 7) })
                }
              />
            </FiscalField>
            <FiscalField label="Código IBGE da UF">
              <input className="fiscal-input fiscal-input--mono" value={codigoUfIbge(end.uf)} readOnly />
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
            <FiscalField label="Série NF-e">
              <input
                className="fiscal-input fiscal-input--mono"
                value={end.serieNfe}
                onChange={(e) => atualizarEndereco(idxEnd, { serieNfe: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Último número NF-e">
              <input
                className="fiscal-input fiscal-input--mono"
                inputMode="numeric"
                value={end.ultimoNumeroNfe}
                onChange={(e) => atualizarEndereco(idxEnd, { ultimoNumeroNfe: e.target.value.replace(/\D/g, "") })}
              />
            </FiscalField>
          </FiscalRow>
          {form.enderecos.length > 1 && (
            <button type="button" className="fiscal-btn-secondary mt-2" onClick={() => removerEndereco(idxEnd)}>
              <Trash2 className="h-4 w-4" /> Remover este endereço
            </button>
          )}
        </FiscalSection>
      )}

      {tab === "docs" && (
        <FiscalSection title="Arquivos fiscais">
          <FiscalRow>
            <FiscalField label="Certificado A1 (.pfx / .p12)">
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
          <FiscalField label="Logotipo do DANFE (PNG ou JPG)">
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

      {tab === "nfse" && (
        <FiscalSection title="NFS-e e download de XML">
          <FiscalRow>
            <FiscalField label="Prefeitura">
              <input
                className="fiscal-input"
                value={form.prefeitura}
                onChange={(e) => onChange({ ...form, prefeitura: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Série RPS">
              <input
                className="fiscal-input fiscal-input--mono"
                value={form.serieRps}
                onChange={(e) => onChange({ ...form, serieRps: e.target.value })}
              />
            </FiscalField>
            <FiscalField label="Último número NFS-e">
              <input
                className="fiscal-input fiscal-input--mono"
                inputMode="numeric"
                value={form.ultimoNumeroNfse}
                onChange={(e) => onChange({ ...form, ultimoNumeroNfse: e.target.value.replace(/\D/g, "") })}
              />
            </FiscalField>
          </FiscalRow>
          <FiscalField label="Baixar XML de entrada automaticamente">
            <select
              className="fiscal-input"
              value={form.baixarXml ? "S" : "N"}
              onChange={(e) => onChange({ ...form, baixarXml: e.target.value === "S" })}
            >
              <option value="N">Não</option>
              <option value="S">Sim — notas de entrada (despesas / livro caixa)</option>
            </select>
          </FiscalField>
          {empresaId != null && form.baixarXml && onBaixarXmls && (
            <button type="button" className="fiscal-btn-secondary mt-3" disabled={baixandoXmls} onClick={onBaixarXmls}>
              {baixandoXmls ? "Baixando XMLs…" : "Baixar XMLs agora"}
            </button>
          )}
        </FiscalSection>
      )}

      {tab === "erp" && embedInfo && <IntegracaoErpPanel {...embedInfo} compact />}
    </div>
  );
}
