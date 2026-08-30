"use client";

import { AutoComplete, type AutoCompleteCompleteEvent } from "primereact/autocomplete";
import { classNames } from "primereact/utils";
import type { ReactNode } from "react";

export type AcOption = {
  label: string;
  value: string;
  meta?: string;
  raw?: unknown;
};

type Props = {
  id?: string;
  label: string;
  hint?: string;
  placeholder?: string;
  value: AcOption | string | null;
  suggestions: AcOption[];
  onChange: (value: AcOption | string | null) => void;
  completeMethod: (event: AutoCompleteCompleteEvent) => void;
  forceSelection?: boolean;
  dropdown?: boolean;
  disabled?: boolean;
  loading?: boolean;
  className?: string;
  itemTemplate?: (item: AcOption) => ReactNode;
  selectedItemTemplate?: (item: AcOption) => ReactNode;
};

function defaultItem(item: AcOption) {
  return (
    <div className="flex flex-col gap-0.5 py-0.5">
      <span className="text-sm font-medium text-slate-800">{item.label}</span>
      {item.meta ? <span className="text-xs text-slate-500">{item.meta}</span> : null}
    </div>
  );
}

export function AutoCompleteField({
  id,
  label,
  hint,
  placeholder,
  value,
  suggestions,
  onChange,
  completeMethod,
  forceSelection = false,
  dropdown = true,
  disabled,
  loading,
  className,
  itemTemplate = defaultItem,
  selectedItemTemplate,
}: Props) {
  return (
    <div className={classNames("nfse-ac-field", className)}>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
      </label>
      {hint ? <p className="mb-1.5 text-xs text-slate-500">{hint}</p> : null}
      <div className={classNames("relative", loading && "opacity-80")}>
        <AutoComplete
          inputId={id}
          value={value ?? undefined}
          suggestions={suggestions}
          completeMethod={completeMethod}
          onChange={(e) => onChange(e.value as AcOption | string | null)}
          field="label"
          forceSelection={forceSelection}
          dropdown={dropdown}
          disabled={disabled}
          placeholder={placeholder}
          className="w-full"
          inputClassName="w-full"
          panelClassName="nfse-ac-panel"
          itemTemplate={itemTemplate}
          selectedItemTemplate={selectedItemTemplate}
          emptyMessage="Nenhum resultado"
          delay={250}
          minLength={0}
        />
      </div>
    </div>
  );
}
