export type EdgeKind = 'walk' | 'stairs';

export interface MapNode {
  id: string;
  name: string;
  type: string;
  floor?: number;
  x: number;
  y: number;
  doorSide?: string | null;
  spoken?: string | null;
}

export interface Edge {
  from: string;
  to: string;
  weight: number;
  kind: EdgeKind;
  steps?: number | null;
  hint?: string;
}

export interface MapData {
  nodes: MapNode[];
  edges: Edge[];
  destinations: string[];
}

export interface Marker {
  id: number;
  nodeId: string;
  facingDeg: number;
  where?: string;
}

export interface MarkersData {
  markers: Marker[];
}

export interface LoadedData {
  map: MapData;
  markers: MarkersData;
}

export type LoadResult = { ok: true; data: LoadedData } | { ok: false; errors: string[] };

export const nodeOf = (map: MapData, id: string) => map.nodes.find((n) => n.id === id);
export const speechName = (n: MapNode) => n.spoken ?? n.name;
export const isRoom = (n: MapNode) => n.type === 'room';
export const edgesFrom = (map: MapData, id: string) => map.edges.filter((e) => e.from === id);
export const markerById = (m: MarkersData, id: number) => m.markers.find((x) => x.id === id);
