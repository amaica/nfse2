import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";
import { REFORMA_FIELDS } from "@/lib/fiscal-api";

export default function OperacaoFiscalPage() {
  return (
    <FiscalCrudWorkspace
      title="Tributação — Operação Fiscal"
      endpoint="/api/tribut-operacao-fiscal"
      reformaSection
      columns={[
        { key: "id", label: "Id" },
        { key: "descricao", label: "Descrição" },
        { key: "cfop", label: "CFOP" },
        { key: "habilitarIbsCbs", label: "IBS/CBS" },
      ]}
      defaultForm={{
        geraFinanceiro: "S",
        movimentaEstoque: "S",
        principal: "N",
        indIntermed: "0",
        ibsCbsCst: "000",
        ibsCbsClassTrib: "000001",
        aliquotaIbsUf: 0.009,
        aliquotaIbsMun: 0.001,
        aliquotaCbs: 0.01,
        habilitarIbsCbs: true,
      }}
      fields={[
        { key: "descricao", label: "Descrição" },
        { key: "tipoOperacao", label: "Tipo operação (E/S)" },
        { key: "cfop", label: "CFOP padrão", type: "number" },
        { key: "descricaoNaNf", label: "Descrição na NF" },
        { key: "geraFinanceiro", label: "Gera financeiro (S/N)" },
        { key: "movimentaEstoque", label: "Movimenta estoque (S/N)" },
        { key: "observacao", label: "Observação", type: "textarea" },
        ...REFORMA_FIELDS,
      ]}
    />
  );
}
