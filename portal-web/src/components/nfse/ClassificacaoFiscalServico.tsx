"use client";

import { useState } from "react";
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
  ativo = true,
}: {
  token: string;
  form: EmissaoFormState;
  patch: Patch;
  ativo?: boolean;
}) {
  const [lc116Busca, setLc116Busca] = useState("");
  const [lc116Aberto, setLc116Aberto] = useState(false);
  const [lc116Editando, setLc116Editando] = useState(!form.servico.itemListaServico);

  const [nbsBusca, setNbsBusca] = useState("");
  const [nbsAberto, setNbsAberto] = useState(false);
  const [nbsLabel, setNbsLabel] = useState("");
  const [nbsEditando, setNbsEditando] = useState(!form.servico.nbs);

  const [cnaeBusca, setCnaeBusca] = useState("");
  const [cnaeAberto, setCnaeAberto] = useState(false);
  const [cnaeLabel, setCnaeLabel] = useState("");
  const [cnaeEditando, setCnaeEditando] = useState(!form.servico.cnae);
  const [cnaeVisivel, setCnaeVisivel] = useState(Boolean(form.servico.cnae));

  const lc116Ok = Boolean(form.servico.itemListaServico);
  const lc116Codigo = form.servico.itemListaServico;

  const { itens: lc116Lista, carregando: buscandoLc116, erro: erroLc116 } = useDebouncedServicos(
    token,
    lc116Busca,
    "todos",
    ativo && lc116Editando && lc116Aberto,
    40,
  );

  const { itens: nbsLista, carregando: buscandoNbs, erro: erroNbs } = useDebouncedNbs(
    token,
    nbsBusca,
    lc116Codigo,
    ativo && lc116Ok && nbsEditando && nbsAberto && nbsBusca.trim().length >= 2,
    30,
  );

  const { itens: cnaeLista, carregando: buscandoCnae, erro: erroCnae } = useDebouncedCnae(
    token,
    cnaeBusca,
    ativo && lc116Ok && cnaeEditando && cnaeAberto,
    40,
  );

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

  return (
    <div className="space-y-5">
      <CampoBusca
        label="Serviço"
        selecionado={lc116Editando ? null : lc116Titulo}
        onTrocar={() => {
          patch("servico", "itemListaServico", "");
          patch("servico", "descricaoServico", "");
          patch("servico", "nbs", "");
          patch("servico", "cnae", "");
          patch("servico", "codigoTributacaoMunicipio", "");
          setLc116Busca("");
          setLc116Editando(true);
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
        itens={lc116Lista.map((s) => ({ key: s.codigo, titulo: s.descricao }))}
        onEscolher={(key) => {
          const s = lc116Lista.find((x) => x.codigo === key);
          if (s) escolherLc116(s);
        }}
      />

      {lc116Ok && (
        <>
          <CampoBusca
            label="NBS"
            selecionado={nbsEditando ? null : nbsLabel || null}
            onTrocar={() => {
              patch("servico", "nbs", "");
              setNbsBusca("");
              setNbsLabel("");
              setNbsEditando(true);
            }}
            placeholder="Buscar pelo nome"
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
              selecionado={cnaeEditando ? null : cnaeLabel || null}
              onTrocar={() => {
                patch("servico", "cnae", "");
                setCnaeBusca("");
                setCnaeLabel("");
                setCnaeEditando(true);
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
              onClick={() => setCnaeVisivel(true)}
            >
              Adicionar CNAE (opcional)
            </button>
          )}
        </>
      )}
    </div>
  );
}
