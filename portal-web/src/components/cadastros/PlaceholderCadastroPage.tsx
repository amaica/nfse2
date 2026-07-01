"use client";

export function PlaceholderCadastroPage({ title }: { title: string }) {
  return (
    <div className="fiscal-card">
      <h1 className="text-2xl font-semibold text-slate-800">{title}</h1>
      <p className="mt-2 text-sm text-slate-600">
        Tela espelhada do FISCALBACKEND — integração com API em desenvolvimento.
      </p>
    </div>
  );
}
