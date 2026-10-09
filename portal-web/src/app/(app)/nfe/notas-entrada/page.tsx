import { redirect } from "next/navigation";

/** Rota antiga — redireciona para o nome correto. */
export default function Page() {
  redirect("/nfe/recebidas");
}
