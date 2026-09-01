"use client";

type Props = {
  rows?: number;
  value?: string;
  onChange?: (value: string) => void;
  className?: string;
  placeholder?: string;
  disabled?: boolean;
};

export function TextArea({
  rows = 3,
  value = "",
  onChange,
  className = "",
  placeholder,
  disabled,
}: Props) {
  return (
    <textarea
      rows={rows}
      value={value}
      placeholder={placeholder}
      disabled={disabled}
      onChange={(e) => onChange?.(e.target.value)}
      className={`w-full rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm text-slate-800 shadow-sm placeholder:text-slate-400 focus:border-[#16c15e] focus:outline-none focus:ring-3 focus:ring-[#16c15e]/15 disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
    />
  );
}
