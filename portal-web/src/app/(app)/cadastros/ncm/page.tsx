import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";

export default function NcmPage() {
  return (
    <FiscalCrudWorkspace
      title="Cadastros — NCM"
      endpoint="/api/ncm"
      columns={[
        { key: "id", label: "Id" },
        { key: "codigo", label: "Código" },
        { key: "descricao", label: "Descrição" },
      ]}
      fields={[
        { key: "codigo", label: "NCM (8 dígitos)" },
        { key: "descricao", label: "Descrição" },
        { key: "observacao", label: "Observação", type: "textarea" },
      ]}
    />
  );
}
