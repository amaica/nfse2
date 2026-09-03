import { fiscalApi } from "@/lib/fiscal-api";
import type { MenuItemDto } from "@/lib/menu/types";

const API = "/api/menu";
export const MENU_UPDATED_EVENT = "portal:menu-updated";

export async function listarMenus(): Promise<MenuItemDto[]> {
  const menus = await fiscalApi.list<MenuItemDto>(API);
  return menus.map(normalizarMenu);
}

export async function buscarMenu(id: number): Promise<MenuItemDto | null> {
  const menu = await fiscalApi.get<MenuItemDto>(API, id);
  return menu ? normalizarMenu(menu) : null;
}

export async function salvarMenu(item: MenuItemDto): Promise<MenuItemDto> {
  const payload: MenuItemDto = {
    ...item,
    parent: item.parent?.id ? { id: item.parent.id } : null,
    submenus: (item.submenus ?? []).map(normalizarSubmenu),
  };
  const saved =
    item.id != null
      ? await fiscalApi.update<MenuItemDto>(API, item.id, payload)
      : await fiscalApi.create<MenuItemDto>(API, payload);
  notifyMenuUpdated();
  return normalizarMenu(saved);
}

export async function excluirMenu(id: number): Promise<void> {
  await fiscalApi.remove(API, id);
  notifyMenuUpdated();
}

function notifyMenuUpdated(): void {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(MENU_UPDATED_EVENT));
  }
}

function normalizarSubmenu(item: NonNullable<MenuItemDto["submenus"]>[number]) {
  return {
    ...(item.id != null ? { id: item.id } : {}),
    label: item.label?.trim() ?? "",
    icon: item.icon?.trim() ?? "",
    outcome: item.outcome?.trim() ?? "",
  };
}

function normalizarMenu(item: MenuItemDto): MenuItemDto {
  return {
    ...item,
    submenus: (item.submenus ?? []).map(normalizarSubmenu),
  };
}
