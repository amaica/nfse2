export type NfseEmbedEvent =
  | { type: "NFSE_EMITIDA"; chave: string }
  | { type: "NFSE_CANCELADA"; chave: string }
  | { type: "ERRO_EMISSAO"; mensagem: string };

export function notifyParent(event: NfseEmbedEvent) {
  if (typeof window === "undefined") return;
  window.parent.postMessage({ source: "synki-nfse", ...event }, "*");
}
