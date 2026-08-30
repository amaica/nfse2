/** Mensagens amigáveis para erros de login/cadastro. */
export function mapAuthError(message: string, status?: number): string {
  const m = message.toLowerCase();
  if (status === 401 || m.includes("credencial") || m.includes("senha incorreta") || m.includes("invalid")) {
    return "E-mail ou senha incorretos. Confira os dados e tente novamente.";
  }
  if (m.includes("inativo")) {
    return "Sua conta está inativa. Peça ao administrador para reativá-la.";
  }
  if (m.includes("email") && (m.includes("cadastrado") || m.includes("exist"))) {
    return "Este e-mail já está cadastrado. Use Entrar ou outro e-mail.";
  }
  if (m.includes("senha") && m.includes("6")) {
    return "A senha deve ter pelo menos 6 caracteres.";
  }
  if (m.includes("email") && m.includes("valid")) {
    return "Informe um e-mail válido.";
  }
  if (status === 0 || m.includes("failed to fetch") || m.includes("network")) {
    return "Não foi possível conectar ao servidor. Verifique se o portal está em execução.";
  }
  if (status === 500 || m.includes("interno")) {
    return "Algo deu errado no servidor. Tente novamente em instantes.";
  }
  return message || "Não foi possível concluir. Tente novamente.";
}

/** Erros de assinatura / emissão bloqueada. */
export function mapEmissaoError(message: string): string {
  const m = message.toLowerCase();
  if (m.includes("trial encerrado") || m.includes("assine")) {
    return "Seu período de teste acabou. Assine um plano em Conta → Assinatura para continuar.";
  }
  if (m.includes("assinatura") && m.includes("bloqueada")) {
    return "Emissão bloqueada pela assinatura. Acesse Conta → Assinatura para regularizar.";
  }
  if (m.includes("cota") && m.includes("nfse")) {
    return "Você atingiu o limite mensal de NFS-e. Aumente seu plano em Conta → Assinatura.";
  }
  if (m.includes("cota") && m.includes("nf-e")) {
    return "Você atingiu o limite mensal de NF-e. Aumente seu plano em Conta → Assinatura.";
  }
  return message;
}

export type AssinaturaStatus = {
  stripeHabilitado: boolean;
  status: string;
  pacotes: number;
  periodoFim: string | null;
  empresasQuota: number;
  empresasUsadas: number;
  usuariosQuota: number;
  usuariosUsados: number;
  nfseMesQuota: number;
  nfseMesUsadas: number;
  nfeMesQuota: number;
  nfeMesUsadas: number;
  podeEmitir?: boolean;
  mensagemStatus?: string;
  diasTrialRestantes?: number;
};
