"use client";

import dynamic from "next/dynamic";
import { useMemo } from "react";

const DEFAULT_CENTER: [number, number] = [-14.235, -51.925];
const DEFAULT_ZOOM = 4;

const MapContent = dynamic(
  () => import("./AddressMapClient").then((m) => m.AddressMapClient),
  {
    ssr: false,
    loading: () => (
      <div className="flex h-full w-full items-center justify-center rounded-xl bg-slate-100 text-sm text-slate-500">
        Carregando mapa...
      </div>
    ),
  },
);

export interface AddressMapProps {
  viewMode?: "mapa" | "satelite";
  center?: [number, number];
  zoom?: number;
  markerPosition?: [number, number];
  draggable?: boolean;
  onMarkerPositionChange?: (lat: number, lng: number) => void;
  height?: string;
  className?: string;
}

export function AddressMap({
  viewMode = "satelite",
  center = DEFAULT_CENTER,
  zoom = DEFAULT_ZOOM,
  markerPosition,
  draggable = false,
  onMarkerPositionChange,
  height = "20rem",
  className = "",
}: AddressMapProps) {
  const style = useMemo(() => ({ height }), [height]);
  return (
    <div
      style={style}
      className={`w-full overflow-hidden rounded-xl border border-slate-200 ${className}`}
    >
      <MapContent
        viewMode={viewMode}
        center={center}
        zoom={zoom}
        markerPosition={markerPosition}
        draggable={draggable}
        onMarkerPositionChange={onMarkerPositionChange}
      />
    </div>
  );
}
