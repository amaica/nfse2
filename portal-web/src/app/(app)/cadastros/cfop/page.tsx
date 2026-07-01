import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";

export default function CfopPage() {
  return (
    <FiscalCrudWorkspace
      title="Cadastros — CFOP"
      endpoint="/api/cfop"
      columns={[
        { key: "id", label: "Id" },
        { key: "cfop", label: "CFOP" },
        { key: "descricao", label: "Descrição" },
      ]}
      fields={[
        { key: "cfop", label: "CFOP (4 dígitos)" },
        { key: "aplicacao", label: "Aplicação" },
        { key: "descricao", label: "Descrição" },
      ]}
    />
  );
}
