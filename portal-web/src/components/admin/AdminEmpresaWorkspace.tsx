"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { adminApi, ApiError, empresaPortalApi, type EmpresaResumo } from "@/lib/api";
import {
  detalheParaForm,
  EmpresaFormulario,
  formInicialVazio,
  type EmpresaFormData,
  type EnderecoLinha,
} from "@/components/admin/EmpresaFormulario";
import { EmpresaListagem } from "@/components/admin/EmpresaListagem";
import { getAdminKey, getAppToken, saveAdminKey } from "@/lib/app-session";

type ModoAuth = "admin" | "portal";

function enderecoParaApi(e: EnderecoLinha) {
  return {
    id: e.id,
    apelido: e.apelido.trim() || "Endereço",
    cep: e.cep || undefined,
    logradouro: e.logradouro || undefined,
    numero: e.numero || undefined,
    bairro: e.bairro || undefined,
    municipio: e.municipio || undefined,
    uf: e.uf || undefined,
    codigoMunicipioIbge: e.codigoMunicipioIbge.replace(/\D/g, "") || undefined,
    inscricaoEstadual: e.inscricaoEstadual || undefined,
    serieNfe: e.serieNfe || "1",
    ultimoNumeroNfe: Number(e.ultimoNumeroNfe) || 0,
    principal: e.principal,
    ativo: e.ativo,
  };
}

function formParaCriarBody(form: EmpresaFormData) {
  const principal = form.enderecos.find((e) => e.principal) ?? form.enderecos[0];
  const doc = form.cpfCnpj.replace(/\D/g, "");
  const emailInt = form.email.trim().toLowerCase() || `nfe.${doc}@synki.demo`;
  return {
    cnpj: doc,
    nome: form.nome.trim(),
    email: form.email || undefined,
    optanteSimples: form.crt === "1",
    prefeitura: form.prefeitura || principal?.municipio || "Municipio",
    codigoMunicipioIbge: (principal?.codigoMunicipioIbge || form.codigoMunicipioIbge).replace(/\D/g, ""),
    ambiente: form.ambiente,
    serieRps: form.serieRps || "1",
    ultimoNumeroNfse: Number(form.ultimoNumeroNfse) || 0,
    emailIntegracao: emailInt,
    senhaIntegracao: form.senhaIntegracao,
    serieNfe: principal?.serieNfe || "1",
    ultimoNumeroNfe: Number(principal?.ultimoNumeroNfe) || 0,
    baixarXml: form.baixarXml,
    enderecos: form.enderecos.map(enderecoParaApi),
    cep: principal?.cep,
    logradouro: principal?.logradouro,
    bairro: principal?.bairro,
    municipio: principal?.municipio,
    uf: principal?.uf,
    inscricaoEstadual: principal?.inscricaoEstadual,
  };
}

export function AdminEmpresaWorkspace() {
  const router = useRouter();
  const [modo, setModo] = useState<ModoAuth | null>(null);
  const [adminKey, setAdminKey] = useState("");
  const [appToken, setAppToken] = useState("");
  const [autenticado, setAutenticado] = useState(false);
  const [inicializando, setInicializando] = useState(true);
  const [empresas, setEmpresas] = useState<EmpresaResumo[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [tela, setTela] = useState<"lista" | "formulario">("lista");
  const [empresaId, setEmpresaId] = useState<number | null>(null);
  const [form, setForm] = useState<EmpresaFormData>(formInicialVazio());
  const [mensagem, setMensagem] = useState<{ tipo: "ok" | "erro"; texto: string } | null>(null);
  const [certArquivo, setCertArquivo] = useState<File | null>(null);
  const [certSenha, setCertSenha] = useState("");
  const [logoArquivo, setLogoArquivo] = useState<File | null>(null);
  const [logoNome, setLogoNome] = useState<string | undefined>();
  const [certNome, setCertNome] = useState<string | undefined>();
  const [baixandoXmls, setBaixandoXmls] = useState(false);
  const [embedInfo, setEmbedInfo] = useState<{
    cnpj?: string;
    emailIntegracao?: string;
    embedUrlCnpj?: string;
    embedUrlCnpjComSenha?: string;
    embedUrlEmail?: string;
  } | null>(null);

  useEffect(() => {
    const token = getAppToken();
    const key = getAdminKey();
    if (token) {
      setModo("portal");
      setAppToken(token);
      void carregarPortal(token).finally(() => setInicializando(false));
    } else if (key) {
      setModo("admin");
      setAdminKey(key);
      void carregarAdmin(key).finally(() => setInicializando(false));
    } else {
      router.replace("/login");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- checagem única na montagem
  }, []);

  async function carregarAdmin(key: string) {
    setCarregando(true);
    setErro("");
    try {
      const res = await adminApi.listarEmpresas(key);
      setEmpresas(res.itens);
      setAutenticado(true);
      saveAdminKey(key);
    } catch (e) {
      setAutenticado(false);
      setErro(e instanceof ApiError ? e.message : "Falha ao conectar");
    } finally {
      setCarregando(false);
    }
  }

  async function carregarPortal(token: string) {
    setCarregando(true);
    setErro("");
    try {
      const res = await empresaPortalApi.listarEmpresas(token);
      setEmpresas(res.itens);
      setAutenticado(true);
    } catch (e) {
      setAutenticado(false);
      setErro(e instanceof ApiError ? e.message : "Falha ao conectar");
    } finally {
      setCarregando(false);
    }
  }

  async function carregar() {
    if (modo === "admin" && adminKey) return carregarAdmin(adminKey);
    if (modo === "portal" && appToken) return carregarPortal(appToken);
  }

  async function abrirEditar(id: number) {
    setCarregando(true);
    setMensagem(null);
    try {
      const det =
        modo === "admin"
          ? await adminApi.obterEmpresa(adminKey, id)
          : await empresaPortalApi.obterEmpresa(appToken, id);
      setForm(detalheParaForm(det));
      setEmbedInfo({
        cnpj: det.cnpj,
        emailIntegracao: det.emailIntegracao,
        embedUrlCnpj: det.embedUrlCnpj,
        embedUrlCnpjComSenha: det.embedUrlCnpjComSenha,
        embedUrlEmail: det.embedUrlEmail,
      });
      setEmpresaId(id);
      setCertNome(det.certificadoCadastrado ? "certificado cadastrado" : undefined);
      setLogoNome(det.logoCadastrado ? "logo cadastrado" : undefined);
      setCertArquivo(null);
      setCertSenha("");
      setLogoArquivo(null);
      setTela("formulario");
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : "Erro ao carregar empresa");
    } finally {
      setCarregando(false);
    }
  }

  function abrirNovo() {
    setForm(formInicialVazio());
    setEmpresaId(null);
    setCertNome(undefined);
    setLogoNome(undefined);
    setCertArquivo(null);
    setCertSenha("");
    setLogoArquivo(null);
    setEmbedInfo(null);
    setMensagem(null);
    setTela("formulario");
  }

  function voltarLista() {
    setTela("lista");
    setEmpresaId(null);
    setEmbedInfo(null);
    setMensagem(null);
    setBaixandoXmls(false);
  }

  async function baixarXmlsAgora() {
    if (empresaId == null) return;
    setBaixandoXmls(true);
    setMensagem(null);
    try {
      const res =
        modo === "admin"
          ? await adminApi.baixarXmlsDestinatario(adminKey, empresaId)
          : await empresaPortalApi.baixarXmlsDestinatario(appToken, empresaId);
      const nsu = res.ultimoNsu ? ` NSU ${res.ultimoNsu}.` : "";
      setMensagem({
        tipo: "ok",
        texto: `${res.novas} XML(s) de entrada gravados para o livro caixa (despesas).${nsu}`,
      });
    } catch (err) {
      setMensagem({
        tipo: "erro",
        texto: err instanceof ApiError ? err.message : err instanceof Error ? err.message : "Falha ao baixar XMLs",
      });
    } finally {
      setBaixandoXmls(false);
    }
  }

  async function salvar() {
    setCarregando(true);
    setMensagem(null);
    try {
      const doc = form.cpfCnpj.replace(/\D/g, "");
      if (!form.nome.trim()) {
        throw new Error("Informe a razão social do emitente.");
      }
      if (doc.length !== 11 && doc.length !== 14) {
        throw new Error("Informe CPF (11 dígitos) ou CNPJ (14 dígitos)");
      }

      let id = empresaId;
      if (empresaId == null) {
        const body = formParaCriarBody(form);
        const res =
          modo === "admin"
            ? await adminApi.criarEmpresa(adminKey, body)
            : await empresaPortalApi.criarEmpresa(appToken, body);
        id = res.empresa.id;
      } else {
        const principal = form.enderecos.find((x) => x.principal) ?? form.enderecos[0];
        const payload = {
          nome: form.nome.trim(),
          email: form.email,
          optanteSimples: form.crt === "1",
          ambiente: form.ambiente,
          prefeitura: form.prefeitura || principal?.municipio,
          codigoMunicipioIbge: (principal?.codigoMunicipioIbge || form.codigoMunicipioIbge).replace(/\D/g, ""),
          serieRps: form.serieRps,
          ultimoNumeroNfse: Number(form.ultimoNumeroNfse) || 0,
          baixarXml: form.baixarXml,
          enderecos: form.enderecos.map(enderecoParaApi),
          ...(form.senhaIntegracao.trim() ? { senhaIntegracao: form.senhaIntegracao } : {}),
        };
        if (modo === "admin") {
          await adminApi.atualizarEmpresa(adminKey, empresaId, payload);
        } else {
          await empresaPortalApi.atualizarEmpresa(appToken, empresaId, payload);
        }
      }

      if (certArquivo && id != null && certSenha) {
        if (modo === "admin") {
          await adminApi.uploadCertificado(adminKey, id, certArquivo, certSenha);
        } else {
          await empresaPortalApi.uploadCertificado(appToken, id, certArquivo, certSenha);
        }
        setCertNome(certArquivo.name);
      }
      if (logoArquivo && id != null) {
        if (modo === "admin") {
          await adminApi.uploadLogo(adminKey, id, logoArquivo);
        } else {
          await empresaPortalApi.uploadLogo(appToken, id, logoArquivo);
        }
        setLogoNome(logoArquivo.name);
      }

      setMensagem({ tipo: "ok", texto: "Empresa salva com sucesso." });
      if (id != null) {
        const det =
          modo === "admin"
            ? await adminApi.obterEmpresa(adminKey, id)
            : await empresaPortalApi.obterEmpresa(appToken, id);
        setEmbedInfo({
          cnpj: det.cnpj,
          emailIntegracao: det.emailIntegracao,
          embedUrlCnpj: det.embedUrlCnpj,
          embedUrlCnpjComSenha: det.embedUrlCnpjComSenha,
          embedUrlEmail: det.embedUrlEmail,
        });
      }
      await carregar();
      if (empresaId == null && id != null) {
        setEmpresaId(id);
      }
    } catch (err) {
      setMensagem({
        tipo: "erro",
        texto: err instanceof ApiError ? err.message : err instanceof Error ? err.message : "Erro ao salvar",
      });
    } finally {
      setCarregando(false);
    }
  }

  async function excluirEmpresa(id: number) {
    setCarregando(true);
    try {
      if (modo === "admin") {
        await adminApi.excluirEmpresa(adminKey, id);
      } else {
        await empresaPortalApi.excluirEmpresa(appToken, id);
      }
      if (empresaId === id) voltarLista();
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : "Erro ao excluir empresa");
    } finally {
      setCarregando(false);
    }
  }

  if (inicializando || (carregando && !autenticado)) {
    return <div className="fiscal-card text-slate-500">Carregando emitentes…</div>;
  }

  if (!autenticado) {
    return (
      <div className="fiscal-card space-y-3">
        <p className="erp-alert erp-alert--error">{erro || "Não foi possível carregar as empresas."}</p>
        <button
          type="button"
          className="fiscal-btn-primary"
          onClick={() => {
            setInicializando(true);
            setErro("");
            const token = getAppToken();
            const key = getAdminKey();
            if (token) {
              void carregarPortal(token).finally(() => setInicializando(false));
            } else if (key) {
              void carregarAdmin(key).finally(() => setInicializando(false));
            } else {
              router.replace("/login");
            }
          }}
        >
          Tentar novamente
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {erro && <p className="erp-alert erp-alert--error">{erro}</p>}
      {tela === "lista" ? (
        <EmpresaListagem
          empresas={empresas}
          carregando={carregando}
          onNovo={abrirNovo}
          onEditar={abrirEditar}
          onExcluir={excluirEmpresa}
          onAtualizar={() => carregar()}
        />
      ) : (
        <EmpresaFormulario
          titulo={empresaId == null ? "Novo emitente" : `Emitente ${form.nome || empresaId}`}
          form={form}
          empresaId={empresaId}
          carregando={carregando}
          certNome={certNome}
          logoNome={logoNome}
          mensagem={mensagem}
          embedInfo={embedInfo}
          onChange={setForm}
          onSalvar={() => void salvar()}
          onVoltar={voltarLista}
          onNovo={abrirNovo}
          onCertificado={(arq, senha) => {
            setCertArquivo(arq);
            setCertSenha(senha);
          }}
          onLogo={setLogoArquivo}
          onBaixarXmls={() => void baixarXmlsAgora()}
          baixandoXmls={baixandoXmls}
        />
      )}
      {modo === "admin" && (
        <p className="text-xs text-slate-500">
          Sem chave admin?{" "}
          <Link href="/auth/admin" className="text-blue-600 hover:underline">
            Autenticar novamente
          </Link>
        </p>
      )}
    </div>
  );
}
