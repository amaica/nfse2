import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";

export default function ConfiguraOfGtPage() {
  return (
    <FiscalCrudWorkspace
      title="Tributação — Configura OF × GT"
      endpoint="/api/tribut-configura-of-gt"
      columns={[
        { key: "id", label: "Id" },
        { key: "tributOperacaoFiscalId", label: "Op. Fiscal" },
        { key: "tributGrupoTributarioId", label: "Grupo Trib." },
      ]}
      fields={[
        { key: "tributOperacaoFiscalId", label: "ID Operação Fiscal", type: "number" },
        { key: "tributGrupoTributarioId", label: "ID Grupo Tributário", type: "number" },
      ]}
    />
  );
}
