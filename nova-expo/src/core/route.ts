import { bearingBetween } from './geometry';
import { Edge, MapData, edgesFrom, nodeOf } from './types';

/** Shortest forward route over the one-way graph (Dijkstra), or null when none exists. */
export function findRoute(map: MapData, from: string, to: string): string[] | null {
  if (!nodeOf(map, from) || !nodeOf(map, to)) return null;
  const dist = new Map<string, number>([[from, 0]]);
  const prev = new Map<string, string>();
  const done = new Set<string>();
  for (;;) {
    let current: string | null = null;
    for (const [id, d] of dist) {
      if (!done.has(id) && (current === null || d < dist.get(current)!)) current = id;
    }
    if (current === null) return null;
    if (current === to) break;
    done.add(current);
    for (const e of edgesFrom(map, current)) {
      const cand = dist.get(current)! + e.weight;
      if (cand < (dist.get(e.to) ?? Infinity)) {
        dist.set(e.to, cand);
        prev.set(e.to, current);
      }
    }
  }
  const route: string[] = [];
  for (let s: string | undefined = to; s !== undefined; s = prev.get(s)) route.unshift(s);
  return route;
}

export interface Leg {
  startNodeId: string;
  endNodeId: string;
  edges: Edge[];
  firstHeadingDeg: number;
}

/** The leg that starts at route[startIndex] and runs to the next marker node (or the end). */
export function buildLeg(map: MapData, markerNodeIds: Set<string>, route: string[], startIndex: number): Leg | null {
  if (startIndex < 0 || startIndex >= route.length - 1) return null;
  let end = startIndex + 1;
  while (end < route.length - 1 && !markerNodeIds.has(route[end])) end++;
  const edges: Edge[] = [];
  for (let i = startIndex; i < end; i++) {
    edges.push(map.edges.find((e) => e.from === route[i] && e.to === route[i + 1])!);
  }
  const a = nodeOf(map, route[startIndex])!;
  const b = nodeOf(map, route[startIndex + 1])!;
  return { startNodeId: route[startIndex], endNodeId: route[end], edges, firstHeadingDeg: bearingBetween(a.x, a.y, b.x, b.y) };
}
