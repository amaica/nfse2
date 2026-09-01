"use client";

import { ChevronDown } from "lucide-react";
import { useEffect, useState } from "react";

type Option = { value: string; label: string };

type Props = {
  options: Option[];
  placeholder?: string;
  onChange: (value: string) => void;
  className?: string;
  value?: string;
};

export function Select({
  options,
  placeholder = "Selecione…",
  onChange,
  className = "",
  value: controlledValue,
}: Props) {
  const [selected, setSelected] = useState(controlledValue ?? "");

  useEffect(() => {
    if (controlledValue !== undefined) setSelected(controlledValue);
  }, [controlledValue]);

  const display = controlledValue ?? selected;

  return (
    <div className="relative">
      <select
        className={`h-11 w-full appearance-none rounded-lg border border-slate-300 bg-white px-4 py-2.5 pr-11 text-sm shadow-sm focus:border-[#16c15e] focus:outline-none focus:ring-3 focus:ring-[#16c15e]/15 ${
          display ? "text-slate-800" : "text-slate-400"
        } ${className}`}
        value={display}
        onChange={(e) => {
          setSelected(e.target.value);
          onChange(e.target.value);
        }}
      >
        <option value="" disabled>
          {placeholder}
        </option>
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
    </div>
  );
}
