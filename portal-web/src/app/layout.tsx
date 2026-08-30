import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "SyncNota — Gestão fiscal para o agro",
  description:
    "Plataforma SaaS de NFS-e, NF-e e cadastros multiempresa para produtores e cooperativas.",
  openGraph: {
    title: "SyncNota",
    description: "Gestão fiscal multiempresa para o agronegócio",
    locale: "pt_BR",
    type: "website",
  },
  icons: {
    icon: "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'%3E%3Crect fill='%233d6b2f' width='32' height='32' rx='6'/%3E%3Ctext x='16' y='22' text-anchor='middle' fill='white' font-size='14' font-family='sans-serif'%3ES%3C/text%3E%3C/svg%3E",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR">
      <body
        className={`${geistSans.variable} ${geistMono.variable} min-h-screen w-full antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
