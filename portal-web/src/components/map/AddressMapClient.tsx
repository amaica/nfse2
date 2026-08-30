"use client";

import { useMemo } from "react";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

const TILE_LAYERS = {
  mapa: {
    url: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  },
  satelite: {
    url: "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
    attribution: "&copy; Esri",
  },
};

function createPinIcon() {
  return L.divIcon({
    className: "address-map-pin border-0 bg-transparent",
    html: `<svg width="28" height="40" viewBox="0 0 28 40" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M14 0C6.268 0 0 6.268 0 14c0 10.5 14 26 14 26s14-15.5 14-26C28 6.268 21.732 0 14 0z" fill="#dc2626"/>
      <circle cx="14" cy="14" r="6" fill="white"/>
    </svg>`,
    iconSize: [28, 40],
    iconAnchor: [14, 40],
  });
}

export interface AddressMapClientProps {
  viewMode: "mapa" | "satelite";
  center: [number, number];
  zoom: number;
  markerPosition?: [number, number];
  draggable?: boolean;
  onMarkerPositionChange?: (lat: number, lng: number) => void;
}

export function AddressMapClient({
  viewMode,
  center,
  zoom,
  markerPosition,
  draggable = false,
  onMarkerPositionChange,
}: AddressMapClientProps) {
  const layer = useMemo(() => TILE_LAYERS[viewMode], [viewMode]);
  const pinIcon = useMemo(createPinIcon, []);
  const position = markerPosition ?? center;

  return (
    <MapContainer center={center} zoom={zoom} className="h-full w-full z-0" scrollWheelZoom>
      <TileLayer url={layer.url} attribution={layer.attribution} />
      <Marker
        position={position}
        icon={pinIcon}
        draggable={draggable || !!onMarkerPositionChange}
        eventHandlers={
          onMarkerPositionChange
            ? {
                dragend: (e) => {
                  const latlng = e.target.getLatLng();
                  onMarkerPositionChange(latlng.lat, latlng.lng);
                },
              }
            : undefined
        }
      >
        <Popup>Arraste o marcador para ajustar as coordenadas.</Popup>
      </Marker>
    </MapContainer>
  );
}
