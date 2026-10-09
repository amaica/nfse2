import { Suspense } from "react";
import { NfeEmissaoWorkspace } from "@/components/nfe/NfeEmissaoWorkspace";

export default function Page() {
  return (
    <Suspense fallback={null}>
      <NfeEmissaoWorkspace />
    </Suspense>
  );
}
