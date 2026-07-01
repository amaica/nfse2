import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";

export default function ProdutosPage() {
  return (
    <FiscalCrudWorkspace
      title="Cadastros — Produtos"
      endpoint="/api/produto"
      columns={[
        { key: "id", label: "Id" },
        { key: "codigo", label: "Código" },
        { key: "nome", label: "Nome" },
        { key: "codigoNcm", label: "NCM" },
      ]}
      defaultForm={{ unidade: "UN", ativo: true }}
      fields={[
        { key: "codigo", label: "Código" },
        { key: "nome", label: "Descrição" },
        { key: "gtin", label: "GTIN/EAN" },
        { key: "codigoNcm", label: "NCM" },
        { key: "unidade", label: "Unidade" },
        { key: "valorUnitario", label: "Valor unitário", type: "number" },
        { key: "grupoTributarioId", label: "Grupo tributário (ID)", type: "number" },
      ]}
    />
  );
}
