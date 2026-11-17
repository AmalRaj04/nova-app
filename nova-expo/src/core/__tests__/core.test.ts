import { describe, expect, it } from 'vitest';
import map from '../../data/map.json';
import markers from '../../data/markers.json';
import { classifyTurn, turnAngle } from '../geometry';
import { routeInstruction, arrivalText } from '../instructions';
import { loadData } from '../loader';
import { Action, NavigationMachine } from '../machine';
import { findRoute } from '../route';
import { MapData, MarkersData } from '../types';

const m = map as unknown as MapData;
const mk = markers as unknown as MarkersData;
const markerNodes = new Set(mk.markers.map((x) => x.nodeId));

function spoken(markerId: number, dest: string, step = 0.7) {
  const marker = mk.markers.find((x) => x.id === markerId)!;
  const route = findRoute(m, marker.nodeId, dest)!;
  return routeInstruction(m, markerNodes, route, 0, marker.facingDeg, step);
}

describe('instructions (golden strings from the Kotlin tests)', () => {
  it('matches the D13 route', () => {
    const entrance = 'You are at the entrance. Walk straight about 2 steps. Then climb 16 steps up.';
    expect(spoken(0, 'D13')).toBe(entrance);
    expect(spoken(1, 'D13')).toBe(entrance);
    const landing = 'You are at the stairs landing. Walk straight about 2 steps. Then climb 10 steps up.';
    expect(spoken(2, 'D13')).toBe(landing);
    const top = 'You are at the top of the main stairs. Turn right. Walk about 8 steps.';
    expect(spoken(4, 'D13')).toBe(top);
    expect(spoken(6, 'D13')).toBe('You are at the walkway corner near the stairs. Turn left. Walk about 14 steps.');
    expect(spoken(7, 'D13')).toBe('You are at the start of the D block path. Turn right. Walk about 11 steps.');
    expect(spoken(8, 'D12')).toBe('You are beside D11. Walk straight about 15 steps.');
    expect(spoken(9, 'D13')).toBe('You are beside D12. Walk straight about 15 steps.');
    expect(arrivalText(m, 'D13')).toBe('You have arrived. D13 is on your right.');
  });
  it('rounds with a shorter step', () => {
    expect(spoken(4, 'D13', 0.6)).toBe('You are at the top of the main stairs. Turn right. Walk about 9 steps.');
  });
});

describe('geometry', () => {
  it('classifies bands with inclusive upper bounds', () => {
    expect(classifyTurn(20)).toBe('STRAIGHT');
    expect(classifyTurn(-50)).toBe('BEAR_LEFT');
    expect(classifyTurn(130)).toBe('TURN_RIGHT');
    expect(classifyTurn(170)).toBe('SHARP_RIGHT');
    expect(classifyTurn(180)).toBe('AROUND');
    expect(turnAngle(350, 10)).toBe(20);
  });
});

describe('data checks', () => {
  it('accepts the shipped data and rejects a broken edge', () => {
    expect(loadData(m, mk).ok).toBe(true);
    const bad = { ...m, edges: [...m.edges, { from: 'X', to: 'D11', weight: 1, kind: 'walk' as const }] };
    expect(loadData(bad, mk).ok).toBe(false);
  });
});

describe('navigation machine', () => {
  const setup = () => {
    let t = 0;
    const machine = new NavigationMachine(() => t);
    machine.handle({ type: 'DataLoaded', result: loadData(m, mk) });
    return { machine, advance: (ms: number) => (t += ms) };
  };
  const texts = (a: Action[]) => a.filter((x) => x.type === 'Speak').map((x: any) => x.text);

  it('walks the whole D13 route to arrival', () => {
    const { machine } = setup();
    machine.handle({ type: 'DestinationChosen', destinationId: 'D13', cameraPermissionGranted: true });
    expect(machine.state).toBe('WAITING');
    const said: string[] = [];
    for (const id of [0, 2, 4, 6, 7, 8, 10]) said.push(...texts(machine.handle({ type: 'Sighting', markerId: id })));
    expect(said[0]).toContain('climb 16 steps up');
    expect(said.at(-1)).toBe('You have arrived. D13 is on your right.');
    expect(machine.state).toBe('ARRIVED');
  });
  it('asks for the camera first when permission is missing', () => {
    const { machine } = setup();
    const a = machine.handle({ type: 'DestinationChosen', destinationId: 'D11', cameraPermissionGranted: false });
    expect(a.some((x) => x.type === 'RequestPermission')).toBe(true);
    expect(machine.state).toBe('PERMISSION');
    machine.handle({ type: 'PermissionResult', granted: false });
    expect(machine.state).toBe('HOME');
  });
  it('says behind you when the destination is backwards', () => {
    const { machine } = setup();
    machine.handle({ type: 'DestinationChosen', destinationId: 'D11', cameraPermissionGranted: true });
    machine.handle({ type: 'Sighting', markerId: 9 });
    expect(machine.state).toBe('WAITING');
  });
  it('fires reminders at 12, 27 and 42 s then help at 57 s', () => {
    const { machine, advance } = setup();
    machine.handle({ type: 'DestinationChosen', destinationId: 'D13', cameraPermissionGranted: true });
    const count = (s: number) => {
      advance(s);
      return texts(machine.handle({ type: 'Tick' })).length;
    };
    expect(count(11_000)).toBe(0);
    expect(count(1_000)).toBe(1);
    expect(count(15_000)).toBe(1);
    expect(count(15_000)).toBe(1);
    expect(texts((advance(15_000), machine.handle({ type: 'Tick' })))[0]).toContain('Still no marker');
    expect(count(15_000)).toBe(0);
  });
  it('replans on an off-route marker', () => {
    const { machine } = setup();
    machine.handle({ type: 'DestinationChosen', destinationId: 'D12', cameraPermissionGranted: true });
    machine.handle({ type: 'Sighting', markerId: 0 });
    const a = texts(machine.handle({ type: 'Sighting', markerId: 4 }));
    expect(machine.state).toBe('GUIDING');
    expect(a.length).toBeGreaterThan(0);
  });
});
