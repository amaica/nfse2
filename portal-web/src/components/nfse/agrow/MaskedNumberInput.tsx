"use client";

import { useEffect, useState } from "react";
import { formatBrNumber, parseBrNumber } from "@/lib/number-mask";

type Props = {
  id?: string;
  value: number | null;
  onChange: (value: number | null) => void;
  decimalPlaces?: number;
  prefix?: string;
  suffix?: string;
  disabled?: boolean;
  placeholder?: string;
  className?: string;
};

const inputClass =
  "h-11 w-full rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm placeholder:text-slate-400 focus:border-[#16c15e] focus:outline-none focus:ring-3 focus:ring-[#16c15e]/15 disabled:cursor-not-allowed disabled:opacity-50";

export function MaskedNumberInput({
  id,
  value,
  onChange,
  decimalPlaces = 4,
  prefix,
  suffix,
  disabled,
  placeholder,
  className = "",
}: Props) {
  const [focused, setFocused] = useState(false);
  const [text, setText] = useState(() => formatBrNumber(value, decimalPlaces));

  useEffect(() => {
    if (!focused) setText(formatBrNumber(value, decimalPlaces));
  }, [value, decimalPlaces, focused]);

  return (
    <div className={`relative flex items-center ${className}`.trim()}>
      {prefix ? (
        <span className="pointer-events-none absolute left-3 text-sm font-semibold text-slate-500">{prefix}</span>
      ) : null}
      <input
        id={id}
        type="text"
        inputMode="decimal"
        disabled={disabled}
        placeholder={placeholder}
        value={text}
        onFocus={() => setFocused(true)}
        onBlur={() => {
          setFocused(false);
          const parsed = parseBrNumber(text);
          onChange(parsed);
          setText(formatBrNumber(parsed, decimalPlaces));
        }}
        onChange={(e) => {
          setText(e.target.value);
          onChange(parseBrNumber(e.target.value));
        }}
        className={`${inputClass} ${prefix ? "pl-10" : ""} ${suffix ? "pr-12" : ""}`}
      />
      {suffix ? (
        <span className="pointer-events-none absolute right-3 text-sm font-semibold text-slate-500">{suffix}</span>
      ) : null}
    </div>
  );
}
