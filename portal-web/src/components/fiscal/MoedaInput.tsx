"use client";

import { useEffect, useState } from "react";

type Props = {
  value?: number | null;
  onChange: (value: number | undefined) => void;
  className?: string;
  placeholder?: string;
  disabled?: boolean;
  id?: string;
};

/** Campo monetário BRL — digitação livre, normaliza ao sair do foco. */
export function MoedaInput({
  value,
  onChange,
  className = "fiscal-input",
  placeholder = "0,00",
  disabled,
  id,
}: Props) {
  const [texto, setTexto] = useState("");

  useEffect(() => {
    if (value == null || Number.isNaN(value)) {
      setTexto("");
      return;
    }
    setTexto(
      value.toLocaleString("pt-BR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 4,
      }),
    );
  }, [value]);

  function parseMoeda(raw: string): number | undefined {
    const t = raw.trim();
    if (!t) return undefined;
    const norm = t.replace(/\./g, "").replace(",", ".");
    const n = Number(norm);
    return Number.isFinite(n) ? n : undefined;
  }

  return (
    <div className="money-field">
      <span className="money-field__prefix">R$</span>
      <input
        id={id}
        type="text"
        inputMode="decimal"
        disabled={disabled}
        className={`${className} money-field__input`}
        placeholder={placeholder}
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        onBlur={() => {
          const n = parseMoeda(texto);
          onChange(n);
          if (n == null) setTexto("");
          else
            setTexto(
              n.toLocaleString("pt-BR", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 4,
              }),
            );
        }}
      />
    </div>
  );
}

type PercentProps = {
  value?: number | null;
  onChange: (value: number | undefined) => void;
  className?: string;
  placeholder?: string;
  disabled?: boolean;
};

/** Percentual (markup, margem). */
export function PercentInput({
  value,
  onChange,
  className = "fiscal-input",
  placeholder = "0,00",
  disabled,
}: PercentProps) {
  const [texto, setTexto] = useState("");

  useEffect(() => {
    if (value == null || Number.isNaN(value)) {
      setTexto("");
      return;
    }
    setTexto(value.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
  }, [value]);

  return (
    <div className="money-field">
      <input
        type="text"
        inputMode="decimal"
        disabled={disabled}
        className={`${className} money-field__input`}
        placeholder={placeholder}
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        onBlur={() => {
          const t = texto.trim().replace(/\./g, "").replace(",", ".");
          if (!t) {
            onChange(undefined);
            setTexto("");
            return;
          }
          const n = Number(t);
          if (!Number.isFinite(n)) return;
          onChange(Math.round(n * 100) / 100);
          setTexto(n.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
        }}
      />
      <span className="money-field__suffix">%</span>
    </div>
  );
}

type QtyProps = {
  value?: number | null;
  onChange: (value: number | undefined) => void;
  className?: string;
  placeholder?: string;
  disabled?: boolean;
  decimals?: number;
};

/** Quantidade / peso — casa decimal livre, normaliza ao sair do foco. */
export function QtyInput({
  value,
  onChange,
  className = "fiscal-input",
  placeholder = "0,000",
  disabled,
  decimals = 4,
}: QtyProps) {
  const [texto, setTexto] = useState("");

  useEffect(() => {
    if (value == null || Number.isNaN(value)) {
      setTexto("");
      return;
    }
    setTexto(
      value.toLocaleString("pt-BR", {
        minimumFractionDigits: 0,
        maximumFractionDigits: decimals,
      }),
    );
  }, [value, decimals]);

  return (
    <input
      type="text"
      inputMode="decimal"
      disabled={disabled}
      className={className}
      placeholder={placeholder}
      value={texto}
      onChange={(e) => setTexto(e.target.value)}
      onBlur={() => {
        const t = texto.trim().replace(/\./g, "").replace(",", ".");
        if (!t) {
          onChange(undefined);
          setTexto("");
          return;
        }
        const n = Number(t);
        if (!Number.isFinite(n)) return;
        const f = Math.pow(10, decimals);
        const rounded = Math.round(n * f) / f;
        onChange(rounded);
        setTexto(
          rounded.toLocaleString("pt-BR", {
            minimumFractionDigits: 0,
            maximumFractionDigits: decimals,
          }),
        );
      }}
    />
  );
}

export function fmtMoeda(v?: number | null): string {
  if (v == null || Number.isNaN(v)) return "—";
  return v.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export function calcPrecoVenda(custo?: number | null, markup?: number | null): number | undefined {
  if (custo == null || custo <= 0) return undefined;
  const m = markup ?? 0;
  return Math.round(custo * (1 + m / 100) * 10000) / 10000;
}

export function calcMarkup(custo?: number | null, venda?: number | null): number | undefined {
  if (custo == null || custo <= 0 || venda == null) return undefined;
  return Math.round((venda / custo - 1) * 10000) / 100;
}
