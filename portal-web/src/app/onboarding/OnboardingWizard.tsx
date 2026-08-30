"use client";

import Link from "next/link";
import { useState } from "react";
import { Building2, CheckCircle2, FileText, Loader2, MapPin, Receipt, Search, Shield } from "lucide-react";
import { BaseButton } from "@/components/ui/BaseButton";
import { BaseInput } from "@/components/ui/BaseInput";
import { api, formatarCnpjCpf, type CnpjPublicoResponse, type OnboardingEmpresaPayload } from "@/lib/api";
import { saveLoginResponse } from "@/lib/app-session";

const PASSOS = ["CNPJ", "Confirmar", "Pronto"];

type Props = {
  token: string;
  contaNome?: string;
};

export function OnboardingWizard({ token, contaNome }: Props) {
  const [passo, setPasso] = useState(1);
  const [cnpjInput, setCnpjInput] = useState("");
  const [dados, setDados] = useState<CnpjPublicoResponse | null>(null);
  const [ambiente, setAmbiente] = useState("homologacao");
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);

  async function buscarCnpj() {
    const doc = cnpjInput.replace(/\D/g, "");
    if (doc.length !== 14) {
      setErro("Informe um CNPJ válido com 14 dígitos.");
      return;
    }
    setErro("");
    setLoading(true);
    try {
      const res = await api.consultarCnpjPublico(cnpjInput);
      setDados(res);
      setPasso(2);
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível consultar o CNPJ. Tente novamente.");
    } finally {
      setLoading(false);
    }
  }

  async function concluir() {
    if (!dados) return;
    setErro("");
    setLoading(true);
    try {
      const payload: OnboardingEmpresaPayload = {
        cnpj: dados.cnpj,
        nome: dados.razaoSocial,
        nomeFantasia: dados.nomeFantasia,
        email: dados.email,
        telefone: dados.telefone,
        cep: dados.endereco.cep,
        logradouro: dados.endereco.logradouro,
        numero: dados.endereco.numero,
        complemento: dados.endereco.complemento,
        bairro: dados.endereco.bairro,
        municipio: dados.endereco.municipio,
        uf: dados.endereco.uf,
        cnaePrincipal: dados.cnaePrincipal.codigo,
        cnaePrincipalDescricao: dados.cnaePrincipal.descricao,
        optanteSimples: dados.optanteSimples,
        situacaoCadastral: dados.situacaoCadastral,
        prefeitura: dados.prefeitura,
        codigoMunicipioIbge: dados.codigoMunicipioIbge,
        ambiente,
      };
      const res = await api.onboardingEmpresa(token, payload);
      saveLoginResponse({ ...res, onboardingRequired: false });
      setPasso(3);
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível cadastrar o emitente.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto w-full max-w-xl">
      <div className="mb-2 flex justify-between text-xs font-medium text-agro-muted">
        {PASSOS.map((label, i) => (
          <span key={label} className={passo >= i + 1 ? "text-[var(--primary-700)]" : ""}>
            {i + 1}. {label}
          </span>
        ))}
      </div>
      <div className="mb-8 flex items-center gap-2">
        {[1, 2, 3].map((n) => (
          <div
            key={n}
            className={`h-2 flex-1 rounded-full transition-colors ${
              passo >= n ? "bg-[var(--brand)]" : "bg-[var(--border)]"
            }`}
          />
        ))}
      </div>

      {contaNome && (
        <p className="mb-4 text-center text-sm text-agro-muted">
          Conta: <strong className="text-agro-body">{contaNome}</strong>
        </p>
      )}

      <p className="mb-6 text-center text-sm text-agro-muted">
        Trial de <strong>14 dias grátis</strong> — cadastre seu primeiro emitente para começar.
      </p>

      {passo === 1 && (
        <div className="login-card space-y-5 p-6">
          <div className="text-center">
            <div className="agro-icon-box mx-auto mb-3">
              <Search className="h-5 w-5" />
            </div>
            <h2 className="text-lg font-semibold text-agro-body">Qual é o CNPJ do emitente?</h2>
            <p className="mt-1 text-sm text-agro-muted">
              Buscamos razão social, endereço e município na Receita Federal
            </p>
          </div>

          <BaseInput
            id="cnpj"
            value={cnpjInput}
            onChange={(e) => setCnpjInput(e.target.value)}
            placeholder="00.000.000/0001-00"
            icon={<Building2 className="h-4 w-4" />}
          />

          {erro && <p className="text-center text-sm text-rose-600">{erro}</p>}

          <BaseButton type="button" loading={loading} onClick={() => void buscarCnpj()}>
            Buscar CNPJ
          </BaseButton>
        </div>
      )}

      {passo === 2 && dados && (
        <div className="login-card space-y-5 p-6">
          <div className="text-center">
            <div className="agro-icon-box mx-auto mb-3">
              <MapPin className="h-5 w-5" />
            </div>
            <h2 className="text-lg font-semibold text-agro-body">Confirme os dados</h2>
          </div>

          <dl className="space-y-2 rounded-lg border border-[var(--border)] bg-[#fafcf7] p-4 text-sm">
            <div>
              <dt className="text-xs uppercase text-agro-muted">Razão social</dt>
              <dd className="font-medium">{dados.razaoSocial}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-agro-muted">CNPJ</dt>
              <dd>{formatarCnpjCpf(dados.cnpj)}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-agro-muted">Município</dt>
              <dd>
                {dados.endereco.municipio}/{dados.endereco.uf} · IBGE {dados.codigoMunicipioIbge}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-agro-muted">Prefeitura NFS-e</dt>
              <dd>{dados.prefeitura}</dd>
            </div>
          </dl>

          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-agro-muted">
              Ambiente fiscal
            </label>
            <select
              value={ambiente}
              onChange={(e) => setAmbiente(e.target.value)}
              className="w-full rounded-lg border border-[var(--border)] bg-white px-3 py-2.5 text-sm"
            >
              <option value="homologacao">Homologação (recomendado para começar)</option>
              <option value="producao">Produção</option>
            </select>
          </div>

          {erro && <p className="text-center text-sm text-rose-600">{erro}</p>}

          <div className="flex gap-3">
            <BaseButton type="button" variant="ghost" onClick={() => setPasso(1)}>
              Voltar
            </BaseButton>
            <BaseButton type="button" loading={loading} className="flex-1" onClick={() => void concluir()}>
              Cadastrar emitente
            </BaseButton>
          </div>
        </div>
      )}

      {passo === 3 && (
        <div className="login-card space-y-6 p-6">
          <div className="text-center">
            <CheckCircle2 className="mx-auto h-12 w-12 text-[var(--brand)]" />
            <h2 className="mt-4 text-lg font-semibold text-agro-body">Emitente cadastrado!</h2>
            <p className="mt-2 text-sm text-agro-muted">
              Próximos passos para emitir sua primeira nota:
            </p>
          </div>

          <ol className="space-y-3 text-sm">
            <li className="flex gap-3 rounded-lg border border-[var(--border)] p-3">
              <Shield className="mt-0.5 h-5 w-5 shrink-0 text-[var(--primary-600)]" />
              <span>
                <strong>1.</strong> Envie o certificado A1 em{" "}
                <Link href="/cadastros/empresa" className="link-agro font-medium">
                  Cadastros → Emitentes
                </Link>
              </span>
            </li>
            <li className="flex gap-3 rounded-lg border border-[var(--border)] p-3">
              <Receipt className="mt-0.5 h-5 w-5 shrink-0 text-sky-600" />
              <span>
                <strong>2.</strong> Emita NFS-e (serviço) ou{" "}
                <Link href="/nfse/emissao" className="link-agro font-medium">
                  abrir emissão NFS-e
                </Link>
              </span>
            </li>
            <li className="flex gap-3 rounded-lg border border-[var(--border)] p-3">
              <FileText className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
              <span>
                <strong>3.</strong> Emita NF-e (produto) em{" "}
                <Link href="/nfe/emissao" className="link-agro font-medium">
                  Emissão NF-e
                </Link>
              </span>
            </li>
          </ol>

          <BaseButton type="button" onClick={() => window.location.assign("/painel")}>
            Ir para o painel
          </BaseButton>
        </div>
      )}

      {loading && passo === 1 && (
        <div className="flex justify-center py-4">
          <Loader2 className="h-6 w-6 animate-spin text-[var(--brand)]" />
        </div>
      )}
    </div>
  );
}
