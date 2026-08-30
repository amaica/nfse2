import Link from "next/link";

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-[var(--surface-ground)] px-6 text-center">
      <p className="text-sm font-semibold uppercase tracking-wider text-agro-muted">404</p>
      <h1 className="mt-2 text-2xl font-semibold text-agro-body">Página não encontrada</h1>
      <p className="mt-2 max-w-md text-sm text-agro-muted">
        O endereço pode ter mudado ou você não tem permissão para acessar este recurso.
      </p>
      <div className="mt-8 flex gap-3">
        <Link href="/" className="fiscal-btn-primary">
          Início
        </Link>
        <Link href="/login" className="btn-ghost">
          Entrar
        </Link>
      </div>
    </div>
  );
}
