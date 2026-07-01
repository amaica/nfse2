import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";

export default function PessoasPage() {
  return (
    <FiscalCrudWorkspace
      title="Cadastros — Pessoas"
      endpoint="/api/pessoas"
      columns={[
        { key: "id", label: "Id" },
        { key: "nome", label: "Nome" },
        { key: "cpfCnpj", label: "CPF/CNPJ" },
        { key: "uf", label: "UF" },
      ]}
      defaultForm={{ tipo: "J", ativo: true }}
      fields={[
        { key: "nome", label: "Nome / Razão social" },
        { key: "tipo", label: "Tipo (F/J)" },
        { key: "cpfCnpj", label: "CPF/CNPJ" },
        { key: "email", label: "E-mail" },
        { key: "inscricaoEstadual", label: "Inscrição estadual" },
        { key: "logradouro", label: "Logradouro" },
        { key: "numero", label: "Número" },
        { key: "bairro", label: "Bairro" },
        { key: "municipio", label: "Município" },
        { key: "uf", label: "UF" },
        { key: "cep", label: "CEP" },
        { key: "codigoMunicipioIbge", label: "Cód. IBGE município" },
      ]}
    />
  );
}
