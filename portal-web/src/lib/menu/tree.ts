import { MENU_LABEL_PATHS, MENU_OUTCOME_PATHS } from "@/lib/menu/paths";
import type { MenuItemDto, MenuNode, ResolvedOutcome, SubmenuItem } from "@/lib/menu/types";

/** Fallback idêntico ao menu estático antigo (ADMIN), se a API falhar ou estiver vazia. */
export const FALLBACK_MENU: MenuNode[] = [
  { id: "fb-inicio", label: "Início", icon: "home", outcome: "/painel", children: [] },
  {
    id: "fb-nfe",
    label: "NF-e",
    icon: "file",
    children: [
      { id: "fb-nfe-e", label: "Emitir NF-e", icon: "pencil", outcome: "/nfe/emissao", children: [] },
      { id: "fb-nfe-l", label: "NF-e — XMLs / DANFE", icon: "list", outcome: "/nfe/notas-emitidas", children: [] },
      { id: "fb-nfe-ev", label: "Eventos da NF-e", icon: "file-edit", outcome: "/nfe/eventos-fiscais", children: [] },
    ],
  },
  {
    id: "fb-nfse",
    label: "NFS-e",
    icon: "receipt",
    children: [
      { id: "fb-nfse-e", label: "Emitir NFS-e", icon: "receipt", outcome: "/nfse/emissao", children: [] },
      { id: "fb-nfse-m", label: "NFS-e mensais", icon: "calendar", outcome: "/nfse/mensais", children: [] },
      { id: "fb-nfse-l", label: "NFS-e emitidas", icon: "list", outcome: "/nfse/emitidas", children: [] },
    ],
  },
  {
    id: "fb-cad",
    label: "Cadastros",
    icon: "database",
    children: [
      { id: "fb-cli", label: "Clientes", icon: "users", outcome: "/cadastros/pessoas", children: [] },
      { id: "fb-prod", label: "Produtos", icon: "box", outcome: "/cadastros/produtos", children: [] },
      { id: "fb-serv", label: "Serviços (NFS-e)", icon: "cog", outcome: "/tributacao/nfse-servico", children: [] },
      { id: "fb-vei", label: "Veículos", icon: "car", outcome: "/cadastros/veiculos", children: [] },
      { id: "fb-usu", label: "Usuários", icon: "user-plus", outcome: "/cadastros/usuarios", children: [] },
    ],
  },
  {
    id: "fb-trib",
    label: "Tributação",
    icon: "scale",
    children: [
      { id: "fb-gt", label: "Grupos tributários", icon: "sitemap", outcome: "/tributacao/grupo-tributario", children: [] },
      { id: "fb-of", label: "Operações fiscais", icon: "briefcase", outcome: "/tributacao/operacao-fiscal", children: [] },
      { id: "fb-cfg", label: "ICMS por operação × grupo", icon: "settings", outcome: "/tributacao/configura-of-gt", children: [] },
      { id: "fb-tnfse", label: "Tributação NFS-e", icon: "receipt", outcome: "/tributacao/nfse-servico", children: [] },
    ],
  },
  {
    id: "fb-emitente",
    label: "Emitente",
    icon: "building",
    children: [
      { id: "fb-emp-op", label: "Dados do emitente", icon: "settings", outcome: "/cadastros/empresa", children: [] },
    ],
  },
  {
    id: "fb-conta",
    label: "Conta",
    icon: "settings",
    children: [
      { id: "fb-emp", label: "Emitentes", icon: "building", outcome: "/cadastros/empresa", children: [] },
      { id: "fb-erp", label: "Integração ERP", icon: "plug", outcome: "/conta/integracao", children: [] },
      { id: "fb-lc", label: "Livro Caixa + LCDPR", icon: "book", outcome: "/conta/contabilidade", children: [] },
      { id: "fb-ass", label: "Assinatura", icon: "credit-card", outcome: "/conta/assinatura", children: [] },
      { id: "fb-lgpd", label: "LGPD e segurança", icon: "shield", outcome: "/conta/lgpd", children: [] },
      { id: "fb-aud", label: "Auditoria", icon: "clipboard-list", outcome: "/conta/auditoria", children: [] },
      { id: "fb-met", label: "Métricas de uso", icon: "chart-bar", outcome: "/conta/metricas", children: [] },
      { id: "fb-menu", label: "Configurar Menu", icon: "list", outcome: "/parametros/configurar-menu", children: [] },
      { id: "fb-perm", label: "Permissões", icon: "shield", outcome: "/parametros/permissoes", children: [] },
    ],
  },
];

const compareItems = (
  left: Pick<MenuItemDto, "ordemMenu" | "label">,
  right: Pick<MenuItemDto, "ordemMenu" | "label">,
) => {
  const orderDifference =
    (left.ordemMenu ?? Number.MAX_SAFE_INTEGER) - (right.ordemMenu ?? Number.MAX_SAFE_INTEGER);
  return (
    orderDifference ||
    (left.label ?? "").localeCompare(right.label ?? "", "pt-BR", {
      sensitivity: "base",
      numeric: true,
    })
  );
};

export function buildMenuTree(items: MenuItemDto[]): MenuNode[] {
  const activeItems = items.filter(
    (item) =>
      item.ativo !== false &&
      String(item.operadorTemAcesso ?? "SIM").toUpperCase() !== "NAO",
  );
  const indexedItems = new Map<number, MenuItemDto>();
  const sourceIndexes = new Map<MenuItemDto, number>();

  activeItems.forEach((item, index) => {
    sourceIndexes.set(item, index);
    if (item.id != null) indexedItems.set(item.id, item);
  });

  const childrenByParent = new Map<number, MenuItemDto[]>();
  activeItems.forEach((item) => {
    const parentId = item.parent?.id;
    if (parentId == null || !indexedItems.has(parentId)) return;
    const children = childrenByParent.get(parentId) ?? [];
    children.push(item);
    childrenByParent.set(parentId, children);
  });

  const buildNode = (item: MenuItemDto, ancestors: Set<MenuItemDto>): MenuNode | null => {
    if (ancestors.has(item)) return null;
    const nextAncestors = new Set(ancestors);
    nextAncestors.add(item);
    const sourceIndex = sourceIndexes.get(item) ?? 0;
    const id = item.id != null ? `menu-${item.id}` : `menu-item-${sourceIndex}`;
    const relatedChildren = item.id == null ? [] : (childrenByParent.get(item.id) ?? []);
    const hierarchyChildren = relatedChildren
      .sort(compareItems)
      .map((child) => buildNode(child, nextAncestors))
      .filter((child): child is MenuNode => child != null);
    const legacyChildren = (item.submenus ?? [])
      .slice()
      .sort((left: SubmenuItem, right: SubmenuItem) =>
        (left.label ?? "").localeCompare(right.label ?? "", "pt-BR", {
          sensitivity: "base",
          numeric: true,
        }),
      )
      .map(
        (submenu, index): MenuNode => ({
          id: `${id}-legacy-${submenu.id ?? index}`,
          label: submenu.label?.trim() || "Item sem nome",
          icon: submenu.icon,
          outcome: submenu.outcome,
          children: [],
        }),
      );

    return {
      id,
      label: item.label?.trim() || "Item sem nome",
      icon: item.icon,
      outcome: item.outcome,
      children: [...hierarchyChildren, ...legacyChildren],
    };
  };

  return activeItems
    .filter((item) => {
      const parentId = item.parent?.id;
      return parentId == null || !indexedItems.has(parentId);
    })
    .sort(compareItems)
    .map((item) => buildNode(item, new Set()))
    .filter((node): node is MenuNode => node != null);
}

/** Monta árvore para gestão (ignora filtro operadorTemAcesso=NAO). */
export function buildMenuTreeAdmin(items: MenuItemDto[]): MenuNode[] {
  return buildMenuTree(items.map((i) => ({ ...i, operadorTemAcesso: "SIM" as const })));
}

export function resolveOutcome(outcome: string | undefined, label: string): ResolvedOutcome {
  const value = outcome?.trim();
  if (!value || value === "#") return null;
  if (/^https?:\/\//i.test(value)) return { kind: "external", href: value };

  const pathname = (value.split(/[?#]/, 1)[0] || "/").replace(/\/+$/, "") || "/";
  const pathKey = pathname.startsWith("/") ? pathname : `/${pathname}`;
  const labelPath = MENU_LABEL_PATHS[label.trim().toLowerCase()];

  const byPath = MENU_OUTCOME_PATHS[pathKey] ?? MENU_OUTCOME_PATHS[pathKey.toLowerCase()];
  if (byPath) return { kind: "internal", href: byPath };

  if (value.startsWith("/") && !/\.xhtml(?:[?#]|$)/i.test(value)) {
    return { kind: "internal", href: pathname || "/" };
  }

  return labelPath ? { kind: "internal", href: labelPath } : null;
}

export function isRouteActive(pathname: string, outcome: ResolvedOutcome): boolean {
  if (!outcome || outcome.kind !== "internal") return false;
  const path = outcome.href.split(/[?#]/, 1)[0] || "/";
  return path === "/"
    ? pathname === "/"
    : pathname === path || pathname.startsWith(`${path}/`);
}

export function nodeContainsActiveRoute(node: MenuNode, pathname: string): boolean {
  return (
    isRouteActive(pathname, resolveOutcome(node.outcome, node.label)) ||
    node.children.some((child) => nodeContainsActiveRoute(child, pathname))
  );
}

/** True se algum item do menu (incluindo filhos) aponta para o href informado. */
export function menuAllowsHref(tree: MenuNode[], href: string): boolean {
  const target = (href.split(/[?#]/, 1)[0] || "/").replace(/\/+$/, "") || "/";
  const walk = (nodes: MenuNode[]): boolean => {
    for (const node of nodes) {
      const resolved = resolveOutcome(node.outcome, node.label);
      if (resolved?.kind === "internal") {
        const path = (resolved.href.split(/[?#]/, 1)[0] || "/").replace(/\/+$/, "") || "/";
        if (path === target || target.startsWith(`${path}/`) || path.startsWith(`${target}/`)) {
          return true;
        }
      }
      if (walk(node.children)) return true;
    }
    return false;
  };
  return walk(tree);
}
