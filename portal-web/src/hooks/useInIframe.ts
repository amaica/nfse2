"use client";

import { useEffect, useState } from "react";

/** True quando a pagina roda dentro de iframe (ex.: /nfse/emissao). */
export function useInIframe() {
  const [inIframe, setInIframe] = useState(false);
  useEffect(() => {
    setInIframe(window.self !== window.top);
  }, []);
  return inIframe;
}
