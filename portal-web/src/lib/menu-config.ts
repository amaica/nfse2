export type MenuItem = {
  label: string;
  icon?: string;
  href?: string;
  external?: boolean;
  items?: MenuItem[];
};

export const PAPEIS_GESTAO = ["OWNER", "ADMIN"] as const;

export function isGestaoPapel(papel?: string | null): boolean {
  if (!papel) return false;
  return PAPEIS_GESTAO.includes(papel as (typeof PAPEIS_GESTAO)[number]);
}

/** NF-e — nota fiscal de produto / mercadoria */
const NFE_ITENS: MenuItem[] = [
  { label: "Emitir NF-e", icon: "pencil", href: "/nfe/emissao" },
  { label: "NF-e — XMLs / DANFE", icon: "list", href: "/nfe/notas-emitidas" },
  { label: "Eventos da NF-e", icon: "file-edit", href: "/nfe/eventos-fiscais" },
];

/** NFS-e — nota fiscal de serviço (admin) */
const NFSE_ITENS: MenuItem[] = [
  { label: "Emitir NFS-e", icon: "receipt", href: "/nfse/emissao" },
  { label: "NFS-e mensais", icon: "calendar", href: "/nfse/mensais" },
  { label: "NFS-e emitidas", icon: "list", href: "/nfse/emitidas" },
];

const TRIBUTACAO_NFE_ITENS: MenuItem[] = [
  { label: "Grupos tributários", icon: "sitemap", href: "/tributacao/grupo-tributario" },
  { label: "Operações fiscais", icon: "briefcase", href: "/tributacao/operacao-fiscal" },
  { label: "ICMS por operação × grupo", icon: "settings", href: "/tributacao/configura-of-gt" },
];

const CADASTROS_BASE: MenuItem[] = [
  { label: "Clientes", icon: "users", href: "/cadastros/pessoas" },
  { label: "Produtos", icon: "box", href: "/cadastros/produtos" },
  { label: "Serviços (NFS-e)", icon: "cog", href: "/tributacao/nfse-servico" },
  { label: "Veículos", icon: "car", href: "/cadastros/veiculos" },
];

/** Menu do operador / visualizador (tema verde). */
export const USER_MENU: MenuItem[] = [
  { label: "Início", icon: "home", href: "/painel" },
  {
    label: "NF-e",
    icon: "file",
    items: NFE_ITENS,
  },
  {
    label: "Cadastros",
    icon: "database",
    items: CADASTROS_BASE,
  },
  {
    label: "Tributação",
    icon: "percent",
    items: TRIBUTACAO_NFE_ITENS,
  },
  {
    label: "Emitente",
    icon: "building",
    items: [{ label: "Dados do emitente", icon: "settings", href: "/cadastros/empresa" }],
  },
];

/** Menu do administrador / owner (tema roxo). */
export const ADMIN_MENU: MenuItem[] = [
  { label: "Início", icon: "home", href: "/painel" },
  {
    label: "NF-e",
    icon: "file",
    items: NFE_ITENS,
  },
  {
    label: "NFS-e",
    icon: "receipt",
    items: NFSE_ITENS,
  },
  {
    label: "Cadastros",
    icon: "database",
    items: [...CADASTROS_BASE, { label: "Usuários", icon: "user-plus", href: "/cadastros/usuarios" }],
  },
  {
    label: "Tributação",
    icon: "scale",
    items: [
      ...TRIBUTACAO_NFE_ITENS,
      { label: "Tributação NFS-e", icon: "receipt", href: "/tributacao/nfse-servico" },
    ],
  },
  {
    label: "Conta",
    icon: "settings",
    items: [
      { label: "Emitentes", icon: "building", href: "/cadastros/empresa" },
      { label: "Integração ERP", icon: "plug", href: "/conta/integracao" },
      { label: "Livro Caixa + LCDPR", icon: "book", href: "/conta/contabilidade" },
      { label: "Assinatura", icon: "credit-card", href: "/conta/assinatura" },
      { label: "LGPD e segurança", icon: "shield", href: "/conta/lgpd" },
      { label: "Auditoria", icon: "clipboard-list", href: "/conta/auditoria" },
      { label: "Métricas de uso", icon: "chart-bar", href: "/conta/metricas" },
    ],
  },
];

export function getMenuForPapel(papel?: string | null): MenuItem[] {
  return isGestaoPapel(papel) ? ADMIN_MENU : USER_MENU;
}

/** Rotas exclusivas de gestão (admin / owner). */
export const GESTAO_ROUTES = [
  "/cadastros/usuarios",
  "/conta",
  "/nfse/emissao",
  "/nfse/mensais",
  "/nfse/emitidas",
  "/parametros",
] as const;

export function isGestaoRoute(path: string): boolean {
  return GESTAO_ROUTES.some((r) => path === r || path.startsWith(r + "/"));
}

/** @deprecated Use getMenuForPapel(session.papel) */
export const APP_MENU = ADMIN_MENU;
