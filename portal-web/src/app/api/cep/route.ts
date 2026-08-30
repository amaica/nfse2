import { NextRequest, NextResponse } from "next/server";

const VIACEP = "https://viacep.com.br/ws";
const NOMINATIM = "https://nominatim.openstreetmap.org/search";

async function geocodeBrasil(
  localidade: string,
  uf: string,
  logradouro?: string,
  bairro?: string,
): Promise<{ lat: number; lng: number } | null> {
  const parts = [logradouro, bairro, localidade, uf, "Brasil"].filter(Boolean);
  const q = parts.join(", ");
  const url = `${NOMINATIM}?q=${encodeURIComponent(q)}&format=json&limit=1`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
        "User-Agent": "SyncNotaPortal/1.0 (cadastro endereco)",
      },
      next: { revalidate: 86400 },
    });
    const arr = (await res.json()) as Array<{ lat: string; lon: string }>;
    if (!Array.isArray(arr) || arr.length === 0) return null;
    const lat = Number(arr[0].lat);
    const lng = Number(arr[0].lon);
    if (Number.isNaN(lat) || Number.isNaN(lng)) return null;
    return { lat, lng };
  } catch {
    return null;
  }
}

/** GET /api/cep?cep=12345678 — ViaCEP + geocode (lat/lng) quando possível. */
export async function GET(request: NextRequest) {
  const cep = request.nextUrl.searchParams.get("cep");
  const digits = (cep ?? "").replace(/\D/g, "");
  if (digits.length !== 8) {
    return NextResponse.json({ error: "CEP deve conter exatamente 8 dígitos." }, { status: 400 });
  }
  try {
    const res = await fetch(`${VIACEP}/${digits}/json/`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 86400 },
    });
    const data = await res.json();
    if (data.erro === true) {
      return NextResponse.json({ error: "CEP não encontrado no ViaCEP." }, { status: 404 });
    }
    const localidade = (data.localidade ?? "").trim();
    const uf = (data.uf ?? "").trim().toUpperCase();
    const logradouro = (data.logradouro ?? "").trim();
    const bairro = (data.bairro ?? "").trim();
    let latitude: string | undefined;
    let longitude: string | undefined;
    if (localidade && uf) {
      const coords = await geocodeBrasil(localidade, uf, logradouro || undefined, bairro || undefined);
      if (coords) {
        latitude = coords.lat.toFixed(6);
        longitude = coords.lng.toFixed(6);
      }
    }
    return NextResponse.json({
      cep: data.cep ?? `${digits.slice(0, 5)}-${digits.slice(5)}`,
      logradouro,
      bairro,
      localidade,
      uf,
      ibge: (data.ibge ?? "").toString().replace(/\D/g, ""),
      ...(latitude != null && { latitude }),
      ...(longitude != null && { longitude }),
    });
  } catch {
    return NextResponse.json({ error: "Falha ao consultar CEP." }, { status: 502 });
  }
}
