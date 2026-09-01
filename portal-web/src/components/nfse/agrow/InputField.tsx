"use client";

import type { FC, InputHTMLAttributes } from "react";

const base =
  "h-11 w-full rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm text-slate-800 shadow-sm placeholder:text-slate-400 focus:border-[#16c15e] focus:outline-none focus:ring-3 focus:ring-[#16c15e]/15 disabled:cursor-not-allowed disabled:opacity-50";

export const InputField: FC<InputHTMLAttributes<HTMLInputElement>> = ({ className = "", ...props }) => (
  <input className={`${base} ${className}`.trim()} {...props} />
);
