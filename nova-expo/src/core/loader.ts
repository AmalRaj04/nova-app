import { LoadResult, MapData, MarkersData, edgesFrom, isRoom, nodeOf, markerById } from './types';

const START_MARKER_ID = 0;

/** The same five data checks as the Kotlin MapLoader. Guidance must not start on failure. */
export function checkData(map: MapData, markers: MarkersData): string[] {
  const errors: string[] = [];
  const ids = new Set(map.nodes.map((n) => n.id));
  for (const e of map.edges) {
    if (!ids.has(e.from)) errors.push(`Edge ${e.from} to ${e.to} starts at unknown node ${e.from}.`);
    if (!ids.has(e.to)) errors.push(`Edge ${e.from} to ${e.to} ends at unknown node ${e.to}.`);
  }
  for (const m of markers.markers) {
    if (!ids.has(m.nodeId)) errors.push(`Marker ${m.id} points at unknown node ${m.nodeId}.`);
  }
  const seen = new Set<number>();
  for (const m of markers.markers) {
    if (seen.has(m.id)) errors.push(`Marker ID ${m.id} is used more than once.`);
    seen.add(m.id);
  }
  for (const d of map.destinations) {
    const n = nodeOf(map, d);
    if (!n) errors.push(`Destination ${d} is not a known node.`);
    else if (!isRoom(n)) errors.push(`Destination ${d} is not a room.`);
  }
  for (const e of map.edges) {
    if (e.weight <= 0) errors.push(`Edge ${e.from} to ${e.to} must have a weight above 0.`);
    if (e.kind === 'stairs' && (e.steps === null || e.steps === undefined)) {
      errors.push(`Stairs edge ${e.from} to ${e.to} has no steps value.`);
    }
  }
  const start = markerById(markers, START_MARKER_ID)?.nodeId;
  if (!start) {
    errors.push(`Marker ${START_MARKER_ID} is missing, so the route start is unknown.`);
  } else if (ids.has(start)) {
    const reach = new Set([start]);
    const queue = [start];
    while (queue.length) {
      for (const e of edgesFrom(map, queue.shift()!)) {
        if (!reach.has(e.to)) {
          reach.add(e.to);
          queue.push(e.to);
        }
      }
    }
    for (const d of map.destinations) {
      if (nodeOf(map, d) && !reach.has(d)) errors.push(`Destination ${d} cannot be reached from the entrance.`);
    }
  }
  return errors;
}

export function loadData(map: MapData, markers: MarkersData): LoadResult {
  const errors = checkData(map, markers);
  return errors.length === 0 ? { ok: true, data: { map, markers } } : { ok: false, errors };
}
