"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { isGestaoPapel } from "@/lib/menu-config";
import { listarMenus, MENU_UPDATED_EVENT } from "@/lib/menu/repository";
import { buildMenuTree, FALLBACK_MENU } from "@/lib/menu/tree";
import type { MenuItemDto, MenuNode } from "@/lib/menu/types";
import { useAppSession } from "@/hooks/useAppSession";

type UseMenusResult = {
  menus: MenuItemDto[];
  menuTree: MenuNode[];
  loading: boolean;
  menuError: boolean;
  reload: () => void;
};

export function useMenus(): UseMenusResult {
  const { session, ready } = useAppSession();
  const gestao = ready && isGestaoPapel(session?.papel);
  const [menus, setMenus] = useState<MenuItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [menuError, setMenuError] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    listarMenus()
      .then((items) => {
        setMenus(Array.isArray(items) ? items : []);
        setMenuError(false);
      })
      .catch(() => {
        setMenuError(true);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    reload();
    const onUpdated = () => reload();
    window.addEventListener(MENU_UPDATED_EVENT, onUpdated);
    return () => window.removeEventListener(MENU_UPDATED_EVENT, onUpdated);
  }, [reload]);

  const menuTree = useMemo(() => {
    if (menuError) return filterFallback(FALLBACK_MENU, gestao);
    const visible = gestao
      ? menus.map((m) => ({ ...m, operadorTemAcesso: "SIM" as const }))
      : menus;
    let tree = buildMenuTree(visible);
    if (gestao) {
      tree = tree.filter((n) => n.label !== "Emitente");
    }
    if (!loading && tree.length === 0) return filterFallback(FALLBACK_MENU, gestao);
    return tree;
  }, [gestao, loading, menuError, menus]);

  return {
    menus,
    menuTree,
    loading,
    menuError,
    reload,
  };
}

function filterFallback(tree: MenuNode[], gestao: boolean): MenuNode[] {
  if (gestao) {
    // gestão: Conta cobre emitente; evita duplicar grupo Emitente do operador
    return tree.filter((n) => n.label !== "Emitente");
  }
  return tree
    .filter((n) => n.label !== "NFS-e" && n.label !== "Conta")
    .map((n) => {
      if (n.label !== "Cadastros" && n.label !== "Tributação") return n;
      return {
        ...n,
        children: n.children.filter(
          (c) => c.label !== "Usuários" && c.label !== "Tributação NFS-e",
        ),
      };
    });
}
