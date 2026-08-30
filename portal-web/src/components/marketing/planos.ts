import { Building2, BookOpen, Download, Headphones, Mail, Receipt, Shield, Users, Zap } from "lucide-react";
import type { LucideIcon } from "lucide-react";

export type PlanoMarketing = {
  id: string;
  nome: string;
  preco: string;
  periodo: string;
  descricao: string;
  destaque?: boolean;
  recursos: string[];
  cta: string;
  href: string;
};

export const PLANOS: PlanoMarketing[] = [
  {
    id: "starter",
    nome: "Starter",
    preco: "R$ 97",
    periodo: "/mês",
    descricao: "Produtor ou pequena propriedade com um emitente.",
    recursos: [
      "1 emitente (CNPJ/CPF)",
      "Até 100 NFS-e/mês",
      "NF-e e cadastros básicos",
      "Livro Caixa + LCDPR dos XMLs",
      "Download de XMLs em ZIP",
      "2 usuários",
      "14 dias grátis",
    ],
    cta: "Começar trial",
    href: "/registrar",
  },
  {
    id: "pro",
    nome: "Pro",
    preco: "R$ 197",
    periodo: "/mês",
    descricao: "Grupo familiar ou operação com vários emitentes.",
    destaque: true,
    recursos: [
      "Até 5 emitentes",
      "500 NFS-e/mês",
      "NF-e completa + eventos",
      "Envio automático de XML para contabilidade",
      "Livro Caixa + LCDPR leiaute 1.3",
      "10 usuários com papéis",
      "Integração ERP (iframe)",
      "Suporte prioritário",
    ],
    cta: "Testar 14 dias",
    href: "/registrar",
  },
  {
    id: "contador",
    nome: "Contador",
    preco: "Sob consulta",
    periodo: "",
    descricao: "Escritórios que gerenciam dezenas de clientes agro.",
    recursos: [
      "Emitentes ilimitados",
      "Volume negociável",
      "Multiusuário OWNER/ADMIN",
      "Livro Caixa + LCDPR por cliente",
      "XML automático + ZIP",
      "Auditoria e LGPD",
      "Onboarding assistido",
    ],
    cta: "Falar com vendas",
    href: "mailto:contato@synki.com.br?subject=SyncNota%20Plano%20Contador",
  },
];

export const COMO_FUNCIONA = [
  {
    passo: "1",
    titulo: "Crie sua conta",
    desc: "Cadastro em 2 minutos. Trial de 14 dias, sem cartão.",
  },
  {
    passo: "2",
    titulo: "Cadastre o emitente",
    desc: "CNPJ automático, certificado A1 e dados fiscais.",
  },
  {
    passo: "3",
    titulo: "Emita notas",
    desc: "NFS-e ou NF-e — XML enviado ao contador ou baixado em ZIP.",
  },
  {
    passo: "4",
    titulo: "Gere Livro Caixa e LCDPR",
    desc: "Conta → Contabilidade: apuração e arquivos gerados dos XMLs das notas emitidas.",
  },
];

export const DESTAQUES: { icon: LucideIcon; titulo: string; desc: string }[] = [
  {
    icon: BookOpen,
    titulo: "Livro Caixa dos XMLs",
    desc: "Receitas extraídas automaticamente dos XMLs de NFS-e e NF-e emitidas no período.",
  },
  {
    icon: Download,
    titulo: "LCDPR leiaute 1.3",
    desc: "Arquivo TXT para validação no PVA da Receita Federal, montado a partir das notas.",
  },
  {
    icon: Mail,
    titulo: "Envio automático para contabilidade",
    desc: "A cada emissão, o XML autorizado vai direto para o e-mail do seu contador.",
  },
  {
    icon: Receipt,
    titulo: "NFS-e + NF-e completo",
    desc: "Emissão, consulta, cancelamento, DANFE, histórico e XML autorizado.",
  },
  {
    icon: Building2,
    titulo: "Multi-emitente",
    desc: "Cooperativa, grupo familiar ou várias propriedades na mesma conta.",
  },
  {
    icon: Shield,
    titulo: "Contabilidade organizada",
    desc: "Tranquilidade nos períodos de prestar contas com a Receita Federal.",
  },
  {
    icon: Users,
    titulo: "Equipe com acesso",
    desc: "Operador, admin ou visualizador — cada um vê só seus emitentes.",
  },
  {
    icon: Zap,
    titulo: "Pronto para ERP",
    desc: "Iframe e API para integrar seu sistema legado.",
  },
  {
    icon: Headphones,
    titulo: "Suporte humano",
    desc: "Time que entende agro e obrigação fiscal municipal/estadual.",
  },
];

export const FAQ = [
  {
    q: "Como funciona o Livro Caixa e o LCDPR?",
    a: "Em Conta → Contabilidade, escolha o período. O SyncNota lê os XMLs das NFS-e e NF-e emitidas, monta o Livro Caixa (CSV) e gera o arquivo LCDPR leiaute 1.3 para importar no PVA da Receita. LCDPR exige emitente com CPF (11 dígitos).",
  },
  {
    q: "Como envio os XMLs para meu contador?",
    a: "Cadastre o e-mail do escritório e ative o envio automático, ou baixe o ZIP com todos os XMLs do período.",
  },
  {
    q: "Precisa de cartão para começar?",
    a: "Não. O trial de 14 dias é gratuito.",
  },
  {
    q: "Funciona para produtor rural (CPF)?",
    a: "Sim. Emitentes CPF e CNPJ, certificado A1 por emitente. LCDPR é gerado para titular PF (CPF).",
  },
  {
    q: "Integra com meu ERP?",
    a: "Sim. URLs de embed por emitente na área Conta → Integração ERP.",
  },
];
