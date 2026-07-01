import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";
import { NFSE_TRIBUT_FIELDS } from "@/lib/fiscal-api";

export default function TributacaoNfseServicoPage() {
  return (
    <FiscalCrudWorkspace
      title="Tributação — NFS-e (serviços)"
      endpoint="/api/tribut-nfse-servico"
      reformaSection
      columns={[
        { key: "id", label: "Id" },
        { key: "descricao", label: "Descrição" },
        { key: "itemListaServico", label: "LC 116" },
        { key: "aliquotaIss", label: "ISS %" },
        { key: "principal", label: "Principal" },
      ]}
      defaultForm={{
        tributacaoIssqn: "1",
        issRetido: "1",
        simplesNacional: "1",
        regimeEspecial: "0",
        cstPisCofins: "08",
        ibsCbsCst: "000",
        ibsCbsClassTrib: "000001",
        aliquotaIbs: 0.01,
        aliquotaCbs: 0.01,
        habilitarIbsCbs: true,
        habilitarRetencoes: false,
        principal: false,
        ativo: true,
      }}
      fields={NFSE_TRIBUT_FIELDS}
    />
  );
}
