"use client";

import { useEffect, useState } from "react";
import { getAppToken } from "@/lib/app-session";

export default function NfseEmissaoPage() {
  const [src, setSrc] = useState("/embed");

  useEffect(() => {
    const token = getAppToken();
    setSrc(token ? `/embed?t=${encodeURIComponent(token)}` : "/embed");
  }, []);

  return (
    <div className="fiscal-card p-0 overflow-hidden">
      <div className="border-b border-slate-200 px-4 py-2 text-sm text-slate-600">
        NFS-e integrada — mesma rota usada no iframe ERP (<code>/embed</code>)
      </div>
      <iframe title="NFS-e" src={src} className="h-[calc(100vh-12rem)] w-full border-0 bg-white" />
    </div>
  );
}
