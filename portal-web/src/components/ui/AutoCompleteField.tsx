"use client";

import { useRef } from "react";
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
  completeMethod: (event: AutoCompleteCompleteEvent) => void | Promise<void>;
  forceSelection?: boolean;
  dropdown?: boolean;
  disabled?: boolean;
  loading?: boolean;
  className?: string;
  invalid?: boolean;
  error?: string;
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

type AcHandle = {
  search: (event: unknown, query: string, source: string) => void;
};

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
  invalid,
  error,
  itemTemplate = defaultItem,
  selectedItemTemplate,
}: Props) {
  const acRef = useRef<AutoComplete>(null);

  return (
    <div className={classNames("nfse-ac-field", className, invalid && "nfse-ac-field--invalid")}>
      {label ? (
        <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-slate-700">
          {label}
        </label>
      ) : null}
      {hint ? <p className="mb-1.5 text-xs text-slate-500">{hint}</p> : null}
      <div className={classNames("relative", loading && "opacity-80")}>
        <AutoComplete
          ref={acRef}
          inputId={id}
          value={value ?? undefined}
          suggestions={suggestions}
          completeMethod={(e) => {
            void Promise.resolve(completeMethod(e));
          }}
          onChange={(e) => onChange(e.value as AcOption | string | null)}
          onFocus={(e) => {
            // Lista ao focar o campo (sem precisar digitar).
            const handle = acRef.current as unknown as AcHandle | null;
            handle?.search(e, "", "dropdown");
          }}
          field="label"
          forceSelection={forceSelection}
          dropdown={dropdown}
          dropdownMode="blank"
          dropdownAutoFocus={false}
          disabled={disabled}
          placeholder={placeholder}
          className="w-full"
          inputClassName="w-full"
          panelClassName="nfse-ac-panel"
          itemTemplate={itemTemplate}
          selectedItemTemplate={selectedItemTemplate}
          emptyMessage="Nenhum resultado"
          showEmptyMessage
          delay={0}
          minLength={0}
        />
      </div>
      {error ? <p className="mt-1 text-xs font-medium text-red-600">{error}</p> : null}
    </div>
  );
}
