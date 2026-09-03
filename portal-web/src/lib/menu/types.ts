export type SubmenuItem = {
  id?: number;
  label?: string;
  icon?: string;
  outcome?: string;
};

export type MenuReference = {
  id: number;
  label?: string;
};

export type MenuItemDto = {
  id?: number;
  label?: string;
  icon?: string;
  ordemMenu?: number;
  outcome?: string;
  operadorTemAcesso?: "SIM" | "NAO";
  ativo?: boolean;
  parent?: MenuReference | null;
  submenus?: SubmenuItem[];
};

export type MenuNode = {
  id: string;
  label: string;
  icon?: string;
  outcome?: string;
  children: MenuNode[];
};

export type ResolvedOutcome =
  | { kind: "internal"; href: string }
  | { kind: "external"; href: string }
  | null;
