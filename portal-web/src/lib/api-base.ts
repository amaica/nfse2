/** Base URL da API. Em produção (mesmo domínio) usa path relativo; em dev, localhost:8080. */
export function apiBaseUrl(): string {
  const env = process.env.NEXT_PUBLIC_API_URL;
  if (env && env.trim()) {
    return env.replace(/\/$/, "");
  }
  if (process.env.NODE_ENV === "production") {
    return "";
  }
  return "http://localhost:8080";
}
