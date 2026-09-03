import { fiscalApi } from "@/lib/fiscal-api";

export type PortalPerfilDto = {
  id?: number;
  nome: string;
  descricao?: string;
  ativo?: boolean;
  menuIds?: number[];
  totalMenus?: number;
};

const API = "/api/permissoes";

export async function listarPerfis(): Promise<PortalPerfilDto[]> {
  return fiscalApi.list<PortalPerfilDto>(API);
}

export async function buscarPerfil(id: number): Promise<PortalPerfilDto> {
  return fiscalApi.get<PortalPerfilDto>(API, id);
}

export async function salvarPerfil(item: PortalPerfilDto): Promise<PortalPerfilDto> {
  if (item.id != null) {
    return fiscalApi.update<PortalPerfilDto>(API, item.id, item);
  }
  return fiscalApi.create<PortalPerfilDto>(API, item);
}

export async function excluirPerfil(id: number): Promise<void> {
  await fiscalApi.remove(API, id);
}
