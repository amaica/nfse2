import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";

export default function VeiculosPage() {
  return (
    <FiscalCrudWorkspace
      title="Cadastros — Veículos"
      endpoint="/api/veiculo"
      columns={[
        { key: "id", label: "Id" },
        { key: "placa", label: "Placa" },
        { key: "modelo", label: "Modelo" },
        { key: "marca", label: "Marca" },
      ]}
      defaultForm={{ ativo: true }}
      fields={[
        { key: "placa", label: "Placa" },
        { key: "modelo", label: "Modelo" },
        { key: "marca", label: "Marca" },
        { key: "renavam", label: "RENAVAM" },
        { key: "tipoRodado", label: "Tipo rodado" },
        { key: "tipoCarroceria", label: "Tipo carroceria" },
      ]}
    />
  );
}
