import type { FC, ReactNode } from "react";
import { twMerge } from "tailwind-merge";

type Props = {
  htmlFor?: string;
  children: ReactNode;
  className?: string;
};

export const Label: FC<Props> = ({ htmlFor, children, className }) => (
  <label
    htmlFor={htmlFor}
    className={twMerge("mb-1.5 block text-sm font-medium text-slate-700", className)}
  >
    {children}
  </label>
);
