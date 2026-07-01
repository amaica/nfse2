import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";

export default function GrupoTributarioPage() {
  return (
    <FiscalCrudWorkspace
      title="Tributação — Grupo Tributário"
      endpoint="/api/tribut-grupo-tributario"
      columns={[
        { key: "id", label: "Id" },
        { key: "descricao", label: "Descrição" },
        { key: "origemMercadoria", label: "Origem" },
      ]}
      fields={[
        { key: "descricao", label: "Descrição" },
        { key: "origemMercadoria", label: "Origem mercadoria (0-8)" },
        { key: "observacao", label: "Observação", type: "textarea" },
      ]}
    />
  );
}
