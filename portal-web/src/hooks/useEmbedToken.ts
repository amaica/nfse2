"use client";

import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";

export function useEmbedToken() {
  const params = useSearchParams();
  const token = params.get("t") ?? "";
  const [valid, setValid] = useState<boolean | null>(null);

  useEffect(() => {
    if (!token) {
      setValid(false);
      return;
    }
    api
      .validateEmbed(token)
      .then(() => setValid(true))
      .catch(() => setValid(false));
  }, [token]);

  return { token, valid, loading: valid === null };
}
