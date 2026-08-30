import { BookOpen, Building2, Download, FileText, Mail, Plug, Receipt, Sprout } from "lucide-react";
import type { LucideIcon } from "lucide-react";

export type RecursoPlataforma = {
  id: string;
  titulo: string;
  desc: string;
  status: "disponivel" | "em_breve";
  cor: string;
  icon: string;
};

export const RECURSOS_PLATAFORMA: RecursoPlataforma[] = [
  {
    id: "nfse",
    titulo: "NFS-e nacional",
    desc: "Emissão, DANFSe, cancelamento e histórico completo.",
    status: "disponivel",
    cor: "emerald",
    icon: "receipt",
  },
  {
    id: "nfe",
    titulo: "NF-e produtor",
    desc: "Nota de produto, DANFE, eventos e XML autorizado.",
    status: "disponivel",
    cor: "sky",
    icon: "file",
  },
  {
    id: "xml",
    titulo: "XML p/ contabilidade",
    desc: "Envio automático por e-mail + download ZIP do período.",
    status: "disponivel",
    cor: "amber",
    icon: "mail",
  },
  {
    id: "livro",
    titulo: "Livro Caixa (CSV)",
    desc: "Gerado a partir dos XMLs das notas emitidas no período.",
    status: "disponivel",
    cor: "lime",
    icon: "book",
  },
  {
    id: "lcdpr",
    titulo: "LCDPR leiaute 1.3",
    desc: "Arquivo TXT para o PVA da Receita, montado dos XMLs das notas.",
    status: "disponivel",
    cor: "orange",
    icon: "download",
  },
  {
    id: "multi",
    titulo: "Multi-emitente",
    desc: "Várias propriedades, CNPJs e equipe na mesma conta.",
    status: "disponivel",
    cor: "violet",
    icon: "building",
  },
  {
    id: "nfpe",
    titulo: "NFP-e",
    desc: "Nota fiscal do produtor eletrônica integrada ao fluxo rural.",
    status: "em_breve",
    cor: "teal",
    icon: "sprout",
  },
  {
    id: "erp",
    titulo: "Integração ERP",
    desc: "Iframe e API para conectar seu sistema legado.",
    status: "disponivel",
    cor: "rose",
    icon: "plug",
  },
];

export type LucideIconMap = Record<string, LucideIcon>;

export const RECURSOS_ICONES: LucideIconMap = {
  receipt: Receipt,
  file: FileText,
  mail: Mail,
  book: BookOpen,
  download: Download,
  building: Building2,
  sprout: Sprout,
  plug: Plug,
};
