import { useEffect, useMemo, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { CircleMarker, MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import { MIN_BAY_ZOOM, UK_CENTER, UK_ZOOM } from '../../config';
import { FEE_LABEL } from '../../components/BayChips';
import { ensureBaysIn } from '../../data/bayStore';
import type { BBox, Bay } from '../../data/types';
import { readStorage, writeStorage } from '../../utils/storage';

export interface MapView {
  bbox: BBox;
  zoom: number;
  center: { lat: number; lon: number };
}

/** Imperative commands from the page, e.g. after a search or "my location". */
export type MapCommand =
  | { kind: 'fly'; lat: number; lon: number; zoom?: number }
  | { kind: 'fit'; bbox: BBox };

interface Props {
  bays: Bay[];
  selectedId: string | null;
  userPosition: { lat: number; lon: number } | null;
  command: MapCommand | null;
  onSelect: (bay: Bay) => void;
  onViewChange: (view: MapView) => void;
}

/** Remembered across remounts and app launches, so the map reopens where the rider left it. */
const VIEW_KEY = 'motobay.mapView';
let lastView = readStorage<{ center: [number, number]; zoom: number }>(VIEW_KEY, { center: UK_CENTER, zoom: UK_ZOOM });

const PIN_TEXT = { free: 'P', paid: '£', unknown: '?' } as const;

function pinIcon(bay: Bay, selected: boolean) {
  return L.divIcon({
    className: '',
    html: `<div class="bay-pin bay-pin--${bay.fee}${selected ? ' bay-pin--selected' : ''}">${PIN_TEXT[bay.fee]}</div>`,
    iconSize: selected ? [40, 40] : [30, 30],
    iconAnchor: selected ? [20, 20] : [15, 15],
  });
}

function ViewTracker({ onViewChange }: Pick<Props, 'onViewChange'>) {
  const map = useMap();
  const report = () => {
    const b = map.getBounds();
    const bbox = { south: b.getSouth(), west: b.getWest(), north: b.getNorth(), east: b.getEast() };
    const zoom = map.getZoom();
    const c = map.getCenter();
    lastView = { center: [c.lat, c.lng], zoom };
    writeStorage(VIEW_KEY, lastView);
    onViewChange({ bbox, zoom, center: { lat: c.lat, lon: c.lng } });
    if (zoom >= MIN_BAY_ZOOM) void ensureBaysIn(bbox);
  };
  useMapEvents({ moveend: report });
  useEffect(() => {
    report();
    // Leaflet measures its container on mount; re-measure when the layout changes size.
    const ro = new ResizeObserver(() => map.invalidateSize());
    ro.observe(map.getContainer());
    return () => ro.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return null;
}

function CommandRunner({ command }: Pick<Props, 'command'>) {
  const map = useMap();
  const last = useRef<MapCommand | null>(null);
  useEffect(() => {
    if (!command || command === last.current) return;
    last.current = command;
    if (command.kind === 'fly') {
      map.flyTo([command.lat, command.lon], command.zoom ?? Math.max(map.getZoom(), 16), { duration: 1 });
    } else {
      const bounds = L.latLngBounds([command.bbox.south, command.bbox.west], [command.bbox.north, command.bbox.east]);
      map.flyToBounds(bounds, { maxZoom: 16, duration: 1 });
    }
  }, [command, map]);
  return null;
}

export function BayMap({ bays, selectedId, userPosition, command, onSelect, onViewChange }: Props) {
  const markers = useMemo(
    () =>
      bays.map((bay) => (
        <Marker
          key={bay.id}
          position={[bay.lat, bay.lon]}
          icon={pinIcon(bay, bay.id === selectedId)}
          zIndexOffset={bay.id === selectedId ? 1000 : 0}
          title={`${bay.name}, ${FEE_LABEL[bay.fee]}`}
          alt={`${bay.name}, ${FEE_LABEL[bay.fee]}`}
          keyboard
          eventHandlers={{ click: () => onSelect(bay) }}
        />
      )),
    [bays, selectedId, onSelect],
  );

  return (
    <MapContainer center={lastView.center} zoom={lastView.zoom} zoomControl={false} style={{ height: '100%', width: '100%' }}>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        maxZoom={19}
      />
      <ViewTracker onViewChange={onViewChange} />
      <CommandRunner command={command} />
      {markers}
      {userPosition && (
        <CircleMarker
          center={[userPosition.lat, userPosition.lon]}
          radius={8}
          pathOptions={{ className: 'user-location', weight: 3, fillOpacity: 1 }}
        />
      )}
    </MapContainer>
  );
}
