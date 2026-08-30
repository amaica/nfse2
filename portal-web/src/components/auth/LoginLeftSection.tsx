import Image from "next/image";
import { Building2, FileText, Sprout } from "lucide-react";

const features = [
  {
    icon: Building2,
    title: "Multiempresa",
    desc: "Suínos, grãos, propriedades — troque o emitente na hora",
  },
  {
    icon: FileText,
    title: "NFS-e e NF-e",
    desc: "Emissão, consulta e eventos fiscais no campo",
  },
  {
    icon: Sprout,
    title: "Agro + reforma",
    desc: "Cadastros, tributação e IBS/CBS integrados",
  },
];

export function LoginLeftSection() {
  return (
    <div
      id="left-section"
      className="relative hidden flex-1 flex-col justify-center px-12 py-16 xl:px-20 lg:flex"
    >
      <div className="mx-auto w-full max-w-lg xl:mx-0">
        <div className="mb-12">
          <Image src="/images/logo.png" alt="SyncNota" width={200} height={48} className="h-12 w-auto" />
        </div>

        <p className="login-eyebrow mb-5">Bem-vindo</p>

        <h1 className="login-headline mb-6 text-[2.75rem] font-semibold leading-[1.08] tracking-tight xl:text-[3.25rem]">
          <span className="login-headline__line">Gestão fiscal</span>
          <span className="login-headline__accent">para o agro</span>
        </h1>

        <p className="mb-12 max-w-md text-lg font-light leading-relaxed text-[#4a5c44] xl:text-xl">
          NFS-e, NF-e e cadastros em um painel pensado para produtores e cooperativas.
          Uma conta, várias propriedades e empresas.
        </p>

        <div className="space-y-3">
          {features.map((item) => (
            <div
              key={item.title}
              className="login-feature flex items-center gap-4 rounded-2xl px-5 py-4"
            >
              <div className="login-feature__icon flex h-10 w-10 shrink-0 items-center justify-center rounded-xl">
                <item.icon className="h-[15px] w-[15px]" />
              </div>
              <div className="min-w-0">
                <h3 className="text-[15px] font-semibold tracking-tight text-[#1a2e16]">
                  {item.title}
                </h3>
                <p className="mt-0.5 text-sm font-light text-[#5c6b55]">{item.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
