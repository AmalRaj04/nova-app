import { useCallback, useEffect, useRef, useState } from 'react';
import { AccessibilityInfo, AppState } from 'react-native';
import { useCameraPermissions } from 'expo-camera';
import map from './data/map.json';
import markers from './data/markers.json';
import { loadData } from './core/loader';
import { Action, ErrorKind, NavEvent, NavState, NavigationMachine } from './core/machine';
import { MapData, MarkersData, nodeOf, speechName } from './core/types';
import { speakOwn, stopSpeaking } from './speech';

export const mapData = map as unknown as MapData;
export const markersData = markers as unknown as MarkersData;

export interface View {
  state: NavState;
  destinationId: string | null;
  error: { message: string; kind: ErrorKind } | null;
  cameraOn: boolean;
  cameraPaused: boolean;
  showSettings: boolean;
  lastInstruction: string | null;
  lines: string[]; // newest first: what was spoken and what happened
}

const stamp = () => new Date().toTimeString().slice(0, 8);

export function useNova() {
  const [perm, requestPerm] = useCameraPermissions();
  const permRef = useRef(false);
  permRef.current = !!perm?.granted;

  const screenReader = useRef(false);
  const machine = useRef<NavigationMachine | undefined>(undefined);
  if (!machine.current) machine.current = new NavigationMachine(() => Date.now());

  const [view, setView] = useState<View>({
    state: 'LOADING', destinationId: null, error: null, cameraOn: false, cameraPaused: false,
    showSettings: false, lastInstruction: null, lines: [],
  });

  const dispatchRef = useRef<(e: NavEvent) => void>(() => {});

  const dispatch = useCallback((event: NavEvent) => {
    const m = machine.current!;
    const actions = m.handle(event);
    const newLines: string[] = [];
    let patch: Partial<View> = {};
    const finalState = m.state;
    for (const a of actions as Action[]) {
      switch (a.type) {
        case 'Speak': {
          newLines.push(`${stamp()} 🔊 ${a.text}`);
          const guiding = finalState === 'WAITING' || finalState === 'GUIDING';
          if (screenReader.current && !guiding) {
            // TalkBack is on and we are on a screen it reads: let it speak so the two voices do not fight.
            AccessibilityInfo.announceForAccessibility(a.text);
          } else {
            speakOwn(a.text, a.interrupt, a.protected);
          }
          break;
        }
        case 'StartCamera': patch = { ...patch, cameraOn: true, cameraPaused: false }; break;
        case 'StopCamera': patch = { ...patch, cameraOn: false }; break;
        case 'PauseCamera': patch = { ...patch, cameraPaused: true }; break;
        case 'ResumeCamera': patch = { ...patch, cameraPaused: false }; break;
        case 'ShowSettingsButton': patch = { ...patch, showSettings: true }; break;
        case 'ShowError': patch = { ...patch, error: { message: a.message, kind: a.kind } }; break;
        case 'RequestPermission':
          requestPerm().then((r) => dispatchRef.current({ type: 'PermissionResult', granted: r.granted }));
          break;
        case 'RetryStep':
          if (a.kind === 'DATA') {
            m.state = 'LOADING';
            dispatchRef.current({ type: 'DataLoaded', result: loadData(mapData, markersData) });
          }
          patch = { ...patch, error: null };
          break;
        case 'Log': {
          const f = Object.entries(a.fields).map(([k, v]) => `${k}=${v}`).join(' ');
          newLines.push(`${stamp()} • ${a.event} ${f}`.trim());
          break;
        }
      }
    }
    if (event.type === 'DestinationChosen' || event.type === 'PermissionResult') patch.showSettings = false;
    if (event.type === 'BackPressed') patch.error = null;
    setView((v) => ({
      ...v,
      ...patch,
      state: finalState,
      destinationId: m.destinationId,
      lastInstruction: m.lastInstruction,
      lines: [...newLines.reverse(), ...v.lines].slice(0, 40),
    }));
  }, [requestPerm]);
  dispatchRef.current = dispatch;

  // Load data once, then tick every second for the "no marker seen" reminders.
  useEffect(() => {
    dispatch({ type: 'DataLoaded', result: loadData(mapData, markersData) });
    const id = setInterval(() => dispatchRef.current({ type: 'Tick' }), 1000);
    return () => clearInterval(id);
  }, [dispatch]);

  useEffect(() => {
    AccessibilityInfo.isScreenReaderEnabled().then((on) => (screenReader.current = on));
    const sub = AccessibilityInfo.addEventListener('screenReaderChanged', (on) => (screenReader.current = on));
    const app = AppState.addEventListener('change', (s) =>
      dispatchRef.current({ type: s === 'active' ? 'AppForegrounded' : 'AppBackgrounded' }),
    );
    return () => {
      sub.remove();
      app.remove();
      stopSpeaking();
    };
  }, []);

  const chooseDestination = useCallback(
    (destinationId: string) => dispatch({ type: 'DestinationChosen', destinationId, cameraPermissionGranted: permRef.current }),
    [dispatch],
  );

  return { view, dispatch, chooseDestination };
}

export const placeName = (id: string) => {
  const n = nodeOf(mapData, id);
  return n ? speechName(n) : id;
};
