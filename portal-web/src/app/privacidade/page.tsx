import Link from "next/link";
import { MarketingShell } from "@/components/marketing/MarketingShell";

export default function PrivacidadePage() {
  return (
    <MarketingShell>
      <article className="mx-auto max-w-3xl px-6 py-16">
        <h1 className="text-3xl font-semibold text-agro-body">Política de privacidade</h1>
        <p className="mt-2 text-sm text-agro-muted">Última atualização: julho de 2026 · LGPD</p>

        <section className="mt-8 space-y-4 text-sm leading-relaxed text-agro-body">
          <p>
            O SyncNota trata dados pessoais e fiscais conforme a Lei Geral de Proteção de Dados (LGPD).
            Coletamos nome, e-mail, CPF/CNPJ, dados de emitentes e notas fiscais para prestar o serviço.
          </p>
          <h2 className="text-lg font-semibold">Seus direitos</h2>
          <p>
            Usuários autenticados podem exportar dados na área Conta → LGPD. Para exclusão ou correção,
            entre em contato pelo e-mail de suporte da sua conta.
          </p>
          <h2 className="text-lg font-semibold">Segurança</h2>
          <p>
            Certificados A1 e dados sensíveis são armazenados com acesso restrito por conta e emitente.
            Utilizamos HTTPS e isolamento multi-tenant na aplicação.
          </p>
          <h2 className="text-lg font-semibold">Compartilhamento</h2>
          <p>
            Dados fiscais são transmitidos apenas aos órgãos competentes (prefeituras, SEFAZ) para emissão
            de documentos. Não vendemos dados a terceiros.
          </p>
        </section>

        <p className="mt-10">
          <Link href="/" className="link-agro">
            ← Voltar ao início
          </Link>
        </p>
      </article>
    </MarketingShell>
  );
}
