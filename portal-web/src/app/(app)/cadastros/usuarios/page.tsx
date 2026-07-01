import { FiscalCrudWorkspace } from "@/components/fiscal/FiscalCrudWorkspace";

export default function UsuariosPage() {
  return (
    <FiscalCrudWorkspace
      title="Cadastros — Usuários"
      endpoint="/api/usuario"
      columns={[
        { key: "id", label: "Id" },
        { key: "nome", label: "Nome" },
        { key: "email", label: "E-mail" },
        { key: "perfil", label: "Perfil" },
      ]}
      defaultForm={{ perfil: "OPERADOR", ativo: true }}
      fields={[
        { key: "nome", label: "Nome" },
        { key: "email", label: "E-mail" },
        { key: "senha", label: "Senha" },
        { key: "cpf", label: "CPF" },
        { key: "perfil", label: "Perfil" },
      ]}
    />
  );
}
