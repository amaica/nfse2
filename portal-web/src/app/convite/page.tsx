import { Suspense } from "react";
import ConviteClient from "./ConviteClient";

export default function ConvitePage() {
  return (
    <Suspense fallback={<div className="app-loading">Carregando convite…</div>}>
      <ConviteClient />
    </Suspense>
  );
}
