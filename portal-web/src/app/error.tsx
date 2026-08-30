"use client";

import Link from "next/link";

export default function ErrorPage({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-[var(--surface-ground)] px-6 text-center">
      <h1 className="text-2xl font-semibold text-agro-body">Algo deu errado</h1>
      <p className="mt-2 max-w-md text-sm text-agro-muted">
        Ocorreu um erro inesperado. Tente novamente ou volte ao início.
      </p>
      <div className="mt-8 flex gap-3">
        <button type="button" className="fiscal-btn-primary" onClick={() => reset()}>
          Tentar de novo
        </button>
        <Link href="/" className="btn-ghost">
          Início
        </Link>
      </div>
    </div>
  );
}
