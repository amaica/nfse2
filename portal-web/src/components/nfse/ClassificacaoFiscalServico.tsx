"use client";

import { useEffect, useMemo, useState } from "react";
import type { EmissaoContexto } from "@/lib/api";
import { api } from "@/lib/api";
import type { EmissaoFormState } from "@/types/emissao-form";
import type { ServicoLc116 } from "@/lib/servicos-lc116";
import { useDebouncedCnae } from "@/lib/use-cnae";
import { useDebouncedNbs } from "@/lib/use-nbs";
import { useDebouncedServicos } from "@/lib/use-servicos-lc116";
import { cn } from "@/lib/utils";
import { Loader2, Search } from "lucide-react";

type Patch = <K extends keyof EmissaoFormState>(
  section: K,
  field: keyof EmissaoFormState[K],
  value: string | boolean,
) => void;

const inputClass =
  "w-full rounded-lg border border-[var(--border)] bg-white px-3 py-2.5 text-sm text-slate-900 focus:border-[var(--brand)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)]/30";

function CampoBusca({
  label,
  opcional,
  hint,
  selecionado,
  onTrocar,
  placeholder,
  valor,
  onValor,
  aberto,
  onAberto,
  carregando,
  erro,
  itens,
  onEscolher,
}: {
  label: string;
  opcional?: boolean;
  hint?: string;
  selecionado: string | null;
  onTrocar: () => void;
  placeholder: string;
  valor: string;
  onValor: (v: string) => void;
  aberto: boolean;
  onAberto: (v: boolean) => void;
  carregando: boolean;
  erro: string | null;
  itens: { key: string; titulo: string }[];
  onEscolher: (key: string) => void;
}) {
  const editando = !selecionado;

  return (
    <div className="space-y-2">
      <span className="text-sm font-medium text-slate-700">
        {label}
        {opcional && <span className="ml-1 font-normal text-slate-400">(opcional)</span>}
      </span>
      {hint && <p className="text-xs text-slate-500">{hint}</p>}

      {editando ? (
        <>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-3 h-4 w-4 text-slate-400" />
            <input
              className={cn(inputClass, "pl-9")}
              placeholder={placeholder}
              value={valor}
              onChange={(e) => {
                onValor(e.target.value);
                onAberto(true);
              }}
              onFocus={() => onAberto(true)}
            />
            {carregando && <Loader2 className="absolute right-3 top-3 h-4 w-4 animate-spin text-slate-400" />}
          </div>
          {erro && <p className="text-xs text-red-600">{erro}</p>}
          {aberto && itens.length > 0 && (
            <ul className="max-h-52 overflow-auto rounded-lg border border-[var(--border)] bg-white py-1 shadow-sm">
              {itens.map((item) => (
                <li key={item.key}>
                  <button
                    type="button"
                    className="w-full px-3 py-2.5 text-left text-sm text-slate-800 hover:bg-[var(--brand-soft)]"
                    onClick={() => onEscolher(item.key)}
                  >
                    {item.titulo}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </>
      ) : (
        <div className="flex items-center justify-between gap-3 rounded-lg border border-[var(--border)] bg-white px-3 py-2.5">
          <p className="min-w-0 flex-1 text-sm text-slate-800">{selecionado}</p>
          <button type="button" className="shrink-0 text-sm text-[var(--brand)] hover:underline" onClick={onTrocar}>
            Trocar
          </button>
        </div>
      )}
    </div>
  );
}

export function ClassificacaoFiscalServico({
  token,
  form,
  patch,
  ctx,
  ativo = true,
}: {
  token: string;
  form: EmissaoFormState;
  patch: Patch;
  ctx?: EmissaoContexto | null;
  ativo?: boolean;
}) {
  const [lc116Busca, setLc116Busca] = useState("");
  const [lc116Aberto, setLc116Aberto] = useState(false);
  const [lc116Editando, setLc116Editando] = useState(!form.servico.itemListaServico);
  const [verTodosServicos, setVerTodosServicos] = useState(false);

  const [nbsBusca, setNbsBusca] = useState("");
  const [nbsAberto, setNbsAberto] = useState(false);
  const [nbsLabel, setNbsLabel] = useState("");
  const [nbsEditando, setNbsEditando] = useState(!form.servico.nbs);

  const [cnaeBusca, setCnaeBusca] = useState("");
  const [cnaeAberto, setCnaeAberto] = useState(false);
  const [cnaeLabel, setCnaeLabel] = useState("");
  const [cnaeEditando, setCnaeEditando] = useState(!form.servico.cnae);
  const [cnaeVisivel, setCnaeVisivel] = useState(Boolean(form.servico.cnae));

  const [cadastrosNfse, setCadastrosNfse] = useState<NonNullable<EmissaoContexto["operacoesNfse"]>>([]);
  const [cadastroBusca, setCadastroBusca] = useState("");
  const [cadastroAberto, setCadastroAberto] = useState(false);
  const [cadastroSelecionado, setCadastroSelecionado] = useState<string | null>(null);
  const [carregandoCadastros, setCarregandoCadastros] = useState(false);

  const lc116Ok = Boolean(form.servico.itemListaServico);
  const lc116Codigo = form.servico.itemListaServico;

  const cnaesEmpresa = useMemo(
    () => (ctx?.empresaCnaes ?? []).map((c) => c.codigo),
    [ctx?.empresaCnaes],
  );

  const cnaesFiltro = verTodosServicos || cnaesEmpresa.length === 0 ? undefined : cnaesEmpresa;

  const hintCnae =
    !verTodosServicos && cnaesEmpresa.length > 0
      ? `Conforme CNAE da empresa: ${ctx?.empresaCnaes?.map((c) => c.descricao).slice(0, 2).join("; ")}${(ctx?.empresaCnaes?.length ?? 0) > 2 ? "…" : ""}`
      : undefined;

  const { itens: lc116Lista, carregando: buscandoLc116, erro: erroLc116 } = useDebouncedServicos(
    token,
    lc116Busca,
    "todos",
    ativo && lc116Editando && lc116Aberto,
    40,
    cnaesFiltro,
  );

  const lc116Itens =
    lc116Lista.length > 0
      ? lc116Lista
      : !verTodosServicos && lc116Busca.trim() === "" && (ctx?.servicosSugeridos?.length ?? 0) > 0
        ? (ctx?.servicosSugeridos ?? []).map((s) => ({ ...s, descricaoPadrao: s.descricao }))
        : lc116Lista;

  const { itens: nbsLista, carregando: buscandoNbs, erro: erroNbs } = useDebouncedNbs(
    token,
    nbsBusca,
    lc116Codigo,
    ativo && lc116Ok && nbsEditando && nbsAberto,
    30,
  );

  const { itens: cnaeLista, carregando: buscandoCnae, erro: erroCnae } = useDebouncedCnae(
    token,
    cnaeBusca,
    ativo && lc116Ok && cnaeEditando && cnaeAberto,
    40,
  );

  useEffect(() => {
    if (ativo && lc116Editando && cnaesEmpresa.length > 0 && !verTodosServicos) {
      setLc116Aberto(true);
    }
  }, [ativo, lc116Editando, cnaesEmpresa.length, verTodosServicos]);

  useEffect(() => {
    if (!ativo) return;
    const base = ctx?.operacoesNfse ?? [];
    if (base.length > 0 && !cadastroBusca.trim()) {
      setCadastrosNfse(base);
      return;
    }
    setCarregandoCadastros(true);
    api
      .listarTributNfseServicos(token, cadastroBusca)
      .then((lista) => setCadastrosNfse(Array.isArray(lista) ? lista : []))
      .catch(() => setCadastrosNfse(base))
      .finally(() => setCarregandoCadastros(false));
  }, [ativo, token, ctx?.operacoesNfse, cadastroBusca]);

  function aplicarCnaePrincipal() {
    const principal = ctx?.empresaCnaes?.find((c) => c.principal) ?? ctx?.empresaCnaes?.[0];
    if (!principal) return;
    patch("servico", "cnae", principal.codigo);
    setCnaeLabel(principal.descricao);
    setCnaeEditando(false);
    setCnaeVisivel(true);
  }

  function sugerirNbs(lc116: string) {
    api.buscarNbs(token, "", 15, lc116).then((r) => {
      if (r.itens.length > 0) {
        const n = r.itens[0];
        patch("servico", "nbs", n.codigoNacional);
        setNbsLabel(n.descricao);
        setNbsBusca("");
        setNbsAberto(false);
        setNbsEditando(false);
      } else {
        setNbsEditando(true);
        setNbsAberto(true);
      }
    });
  }

  function escolherLc116(s: ServicoLc116) {
    patch("servico", "itemListaServico", s.codigo);
    patch("servico", "descricaoServico", s.descricao);
    patch("servico", "codigoTributacaoMunicipio", "");
    patch("servico", "nbs", "");
    patch("servico", "cnae", "");
    setLc116Busca("");
    setLc116Aberto(false);
    setLc116Editando(false);
    setNbsLabel("");
    setNbsEditando(true);
    setCnaeLabel("");
    setCnaeEditando(true);
    setCnaeVisivel(false);
    aplicarCnaePrincipal();
    sugerirNbs(s.codigo);
  }

  function escolherNbs(codigoNacional: string, descricao: string) {
    patch("servico", "nbs", codigoNacional);
    setNbsLabel(descricao);
    setNbsBusca("");
    setNbsAberto(false);
    setNbsEditando(false);
  }

  function escolherCnae(codigo: string, descricao: string) {
    patch("servico", "cnae", codigo);
    setCnaeLabel(descricao);
    setCnaeBusca("");
    setCnaeAberto(false);
    setCnaeEditando(false);
  }

  const lc116Titulo = lc116Ok ? form.servico.descricaoServico : null;

  async function aplicarCadastroNfse(id: string) {
    if (!id) return;
    const cad = await api.obterTributNfseServico(token, Number(id));
    const item = cadastrosNfse.find((c) => c.id === Number(id));
    setCadastroSelecionado(
      item ? `${item.descricao}${item.principal ? " (principal)" : ""}` : cad.descricao,
    );
    setCadastroBusca("");
    setCadastroAberto(false);
    if (cad.itemListaServico) {
      patch("servico", "itemListaServico", cad.itemListaServico);
      setLc116Editando(false);
    }
    if (cad.descricaoServico) patch("servico", "descricaoServico", cad.descricaoServico);
    if (cad.codigoTributacaoMunicipio) patch("servico", "codigoTributacaoMunicipio", cad.codigoTributacaoMunicipio);
    if (cad.nbs) {
      patch("servico", "nbs", cad.nbs);
      setNbsLabel(cad.nbs);
      setNbsEditando(false);
    }
    if (cad.cnae) {
      patch("servico", "cnae", cad.cnae);
      setCnaeLabel(cad.cnae);
      setCnaeEditando(false);
      setCnaeVisivel(true);
    }
    if (cad.municipioPrestacaoIbge) {
      patch("servico", "municipioPrestacao", cad.municipioPrestacaoIbge);
      patch("servico", "localPrestacao", cad.municipioPrestacaoIbge);
    }
    if (cad.aliquotaIss != null) patch("valores", "aliquota", String(cad.aliquotaIss));
    if (cad.tributacaoIssqn) patch("regime", "tributacaoIssqn", cad.tributacaoIssqn);
    if (cad.issRetido) patch("regime", "issRetido", cad.issRetido);
    if (cad.simplesNacional) patch("regime", "simplesNacional", cad.simplesNacional);
    if (cad.regimeEspecial) patch("regime", "regimeEspecialTributacao", cad.regimeEspecial);
    if (cad.cstPisCofins) patch("tributacaoFederal", "cstPisCofins", cad.cstPisCofins);
    if (cad.aliquotaPis != null) patch("tributacaoFederal", "aliquotaPis", String(cad.aliquotaPis));
    if (cad.aliquotaCofins != null) patch("tributacaoFederal", "aliquotaCofins", String(cad.aliquotaCofins));
    if (cad.habilitarRetencoes != null) patch("tributacaoFederal", "habilitarRetencoes", cad.habilitarRetencoes);
    if (cad.retencaoInss != null) patch("tributacaoFederal", "retencaoInss", String(cad.retencaoInss));
    if (cad.retencaoIrrf != null) patch("tributacaoFederal", "retencaoIrrf", String(cad.retencaoIrrf));
    if (cad.retencaoCsll != null) patch("tributacaoFederal", "retencaoCsll", String(cad.retencaoCsll));
    if (cad.ibsCbsCst) patch("ibsCbs", "cst", cad.ibsCbsCst);
    if (cad.ibsCbsClassTrib) patch("ibsCbs", "classificacaoTributaria", cad.ibsCbsClassTrib);
    if (cad.aliquotaIbs != null) patch("ibsCbs", "aliquotaIbs", String(cad.aliquotaIbs));
    if (cad.aliquotaCbs != null) patch("ibsCbs", "aliquotaCbs", String(cad.aliquotaCbs));
    if (cad.habilitarIbsCbs != null) patch("ibsCbs", "habilitar", cad.habilitarIbsCbs);
  }

  return (
    <div className="space-y-5">
      <CampoBusca
        label="Cadastro tributário NFS-e"
        hint="Busque e aplique um cadastro salvo em Tributação → NFS-e (serviços)"
        selecionado={cadastroSelecionado}
        onTrocar={() => {
          setCadastroSelecionado(null);
          setCadastroBusca("");
          setCadastroAberto(true);
        }}
        placeholder="Buscar cadastro por nome ou código LC 116"
        valor={cadastroBusca}
        onValor={setCadastroBusca}
        aberto={cadastroAberto}
        onAberto={setCadastroAberto}
        carregando={carregandoCadastros}
        erro={cadastrosNfse.length === 0 && !carregandoCadastros ? "Nenhum cadastro NFS-e — cadastre em Tributação" : null}
        itens={cadastrosNfse.map((o) => ({
          key: String(o.id),
          titulo: `${o.descricao}${o.principal ? " (principal)" : ""} — ${o.itemListaServico}`,
        }))}
        onEscolher={(key) => void aplicarCadastroNfse(key)}
      />
      <CampoBusca
        label="Serviço"
        hint={hintCnae}
        selecionado={lc116Editando ? null : lc116Titulo}
        onTrocar={() => {
          patch("servico", "itemListaServico", "");
          patch("servico", "descricaoServico", "");
          patch("servico", "nbs", "");
          patch("servico", "cnae", "");
          patch("servico", "codigoTributacaoMunicipio", "");
          setLc116Busca("");
          setLc116Editando(true);
          setLc116Aberto(true);
          setVerTodosServicos(false);
          setNbsLabel("");
          setNbsEditando(true);
          setCnaeLabel("");
          setCnaeEditando(true);
          setCnaeVisivel(false);
        }}
        placeholder="Buscar pelo nome do serviço"
        valor={lc116Busca}
        onValor={setLc116Busca}
        aberto={lc116Aberto}
        onAberto={setLc116Aberto}
        carregando={buscandoLc116}
        erro={erroLc116}
        itens={lc116Itens.map((s) => ({ key: s.codigo, titulo: s.descricao }))}
        onEscolher={(key) => {
          const s = lc116Itens.find((x) => x.codigo === key);
          if (s) escolherLc116(s);
        }}
      />

      {lc116Editando && cnaesEmpresa.length > 0 && (
        <button
          type="button"
          className="text-sm text-[var(--brand)] hover:underline"
          onClick={() => {
            setVerTodosServicos((v) => !v);
            setLc116Aberto(true);
          }}
        >
          {verTodosServicos ? "Mostrar só serviços dos CNAEs da empresa" : "Ver todos os serviços LC 116"}
        </button>
      )}

      {lc116Ok && (
        <>
          <CampoBusca
            label="NBS"
            hint="Sugerido conforme o serviço LC 116 — você pode trocar"
            selecionado={nbsEditando ? null : nbsLabel || null}
            onTrocar={() => {
              patch("servico", "nbs", "");
              setNbsBusca("");
              setNbsLabel("");
              setNbsEditando(true);
              setNbsAberto(true);
            }}
            placeholder="Buscar pelo nome ou escolha na lista"
            valor={nbsBusca}
            onValor={setNbsBusca}
            aberto={nbsAberto}
            onAberto={setNbsAberto}
            carregando={buscandoNbs}
            erro={erroNbs}
            itens={nbsLista.map((n) => ({ key: n.codigoNacional, titulo: n.descricao }))}
            onEscolher={(key) => {
              const n = nbsLista.find((x) => x.codigoNacional === key);
              if (n) escolherNbs(n.codigoNacional, n.descricao);
            }}
          />

          {cnaeVisivel ? (
            <CampoBusca
              label="CNAE"
              opcional
              hint="Preenchido com o CNAE principal da empresa — você pode trocar"
              selecionado={cnaeEditando ? null : cnaeLabel || null}
              onTrocar={() => {
                patch("servico", "cnae", "");
                setCnaeBusca("");
                setCnaeLabel("");
                setCnaeEditando(true);
                setCnaeAberto(true);
              }}
              placeholder="Buscar pelo nome da atividade"
              valor={cnaeBusca}
              onValor={setCnaeBusca}
              aberto={cnaeAberto}
              onAberto={setCnaeAberto}
              carregando={buscandoCnae}
              erro={erroCnae}
              itens={cnaeLista.map((c) => ({ key: c.codigo, titulo: c.descricao }))}
              onEscolher={(key) => {
                const c = cnaeLista.find((x) => x.codigo === key);
                if (c) escolherCnae(c.codigo, c.descricao);
              }}
            />
          ) : (
            <button
              type="button"
              className="text-sm text-[var(--brand)] hover:underline"
              onClick={() => {
                setCnaeVisivel(true);
                aplicarCnaePrincipal();
              }}
            >
              Adicionar CNAE (opcional)
            </button>
          )}
        </>
      )}
    </div>
  );
}
