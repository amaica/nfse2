export type MenuItem = {
  label: string;
  icon?: string;
  href?: string;
  external?: boolean;
  items?: MenuItem[];
};

export const APP_MENU: MenuItem[] = [
  { label: "Início", icon: "home", href: "/" },
  {
    label: "Cadastros",
    icon: "database",
    items: [
      { label: "Empresa", icon: "building", href: "/cadastros/empresa" },
      { label: "Usuários", icon: "user-plus", href: "/cadastros/usuarios" },
      { label: "Pessoas", icon: "users", href: "/cadastros/pessoas" },
      { label: "Produtos", icon: "box", href: "/cadastros/produtos" },
      { label: "CFOP", icon: "arrow-right-left", href: "/cadastros/cfop" },
      { label: "NCM", icon: "tag", href: "/cadastros/ncm" },
      { label: "Veículos", icon: "car", href: "/cadastros/veiculos" },
    ],
  },
  {
    label: "Tributação",
    icon: "percent",
    items: [
      { label: "Grupo Tributário (NF-e)", icon: "sitemap", href: "/tributacao/grupo-tributario" },
      { label: "Operação Fiscal (NF-e)", icon: "briefcase", href: "/tributacao/operacao-fiscal" },
      { label: "Configurações OF/GT (NF-e)", icon: "settings", href: "/tributacao/configura-of-gt" },
      { label: "Tributação NFSe", icon: "receipt", href: "/tributacao/nfse-servico" },
    ],
  },
  {
    label: "NF-e",
    icon: "file",
    items: [
      { label: "Emissão", icon: "pencil", href: "/nfe/emissao" },
      { label: "Notas emitidas", icon: "list", href: "/nfe/notas-emitidas" },
      { label: "Eventos fiscais", icon: "file-edit", href: "/nfe/eventos-fiscais" },
    ],
  },
  {
    label: "NFS-e",
    icon: "receipt",
    items: [
      { label: "Emissão (portal)", icon: "external-link", href: "/nfse/emissao" },
      { label: "Embed ERP", icon: "frame", href: "/embed", external: true },
    ],
  },
];
