"use client";

import Image from "next/image";
import { useEffect, useState } from "react";

const WALLPAPERS = [
  "/images/login-rotator/wall1.jpeg",
  "/images/login-rotator/wall3.jpeg",
  "/images/login-rotator/wall4.jpeg",
  "/images/login-rotator/wall6.jpeg",
];

export function AuthImmersiveShell({ children }: { children: React.ReactNode }) {
  const [idx, setIdx] = useState(0);
  const [fade, setFade] = useState(true);

  useEffect(() => {
    const rotate = setInterval(() => {
      setFade(false);
      window.setTimeout(() => {
        setIdx((i) => (i + 1) % WALLPAPERS.length);
        setFade(true);
      }, 400);
    }, 12000);
    return () => clearInterval(rotate);
  }, []);

  return (
    <div className="relative min-h-screen w-full overflow-hidden font-sans antialiased">
      <div className="pointer-events-none fixed inset-0 z-0 bg-slate-900">
        <Image
          src={WALLPAPERS[idx]}
          alt=""
          fill
          className={`object-cover transition-opacity duration-700 ${fade ? "opacity-100" : "opacity-0"}`}
          priority
          sizes="100vw"
        />
        <div className="absolute inset-0 bg-black/60" aria-hidden />
      </div>
      <div className="relative z-10 flex min-h-screen items-center justify-center p-4 sm:p-6">
        <div
          className="w-full max-w-[25rem] rounded-2xl px-9 py-11 shadow-2xl shadow-black/50 backdrop-blur-xl sm:px-10 sm:py-12"
          style={{
            backgroundColor: "rgba(16, 18, 20, 0.74)",
            border: "1px solid rgba(255, 255, 255, 0.1)",
          }}
        >
          <div className="mb-6 flex justify-center">
            <Image src="/images/logo.png" alt="SyncNota" width={160} height={40} className="h-10 w-auto" />
          </div>
          {children}
        </div>
      </div>
    </div>
  );
}
