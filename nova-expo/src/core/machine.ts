import { Config } from './config';
import { arrivalText, routeInstruction } from './instructions';
import { Phrases } from './phrases';
import { findRoute } from './route';
import { LoadResult, LoadedData, markerById, nodeOf, speechName } from './types';

export type NavState = 'LOADING' | 'HOME' | 'PERMISSION' | 'WAITING' | 'GUIDING' | 'ARRIVED' | 'ERROR';
export type ErrorKind = 'DATA' | 'CAMERA' | 'SPEECH';

export type NavEvent =
  | { type: 'DataLoaded'; result: LoadResult }
  | { type: 'DestinationChosen'; destinationId: string; cameraPermissionGranted: boolean }
  | { type: 'PermissionResult'; granted: boolean }
  /** An accepted sighting. In this Expo version it comes from the team-mode marker buttons. */
  | { type: 'Sighting'; markerId: number }
  | { type: 'Tick' }
  | { type: 'RepeatPressed' }
  | { type: 'WhereAmIPressed' }
  | { type: 'StopPressed' }
  | { type: 'ChooseAnotherPressed' }
  | { type: 'ErrorOccurred'; kind: ErrorKind; message: string }
  | { type: 'RetryPressed' }
  | { type: 'BackPressed' }
  | { type: 'AppBackgrounded' }
  | { type: 'AppForegrounded' };

export type Action =
  | { type: 'Speak'; text: string; interrupt: boolean; protected: boolean }
  | { type: 'StartCamera' }
  | { type: 'StopCamera' }
  | { type: 'PauseCamera' }
  | { type: 'ResumeCamera' }
  | { type: 'RequestPermission' }
  | { type: 'ShowSettingsButton' }
  | { type: 'ShowError'; message: string; kind: ErrorKind }
  | { type: 'RetryStep'; kind: ErrorKind }
  | { type: 'Log'; event: string; fields: Record<string, string> };

const speak = (text: string, interrupt = true, prot = false): Action => ({
  type: 'Speak', text, interrupt, protected: prot,
});
const log = (event: string, fields: Record<string, string> = {}): Action => ({ type: 'Log', event, fields });

/** The whole guidance logic as one pure state machine: events in, actions out. Port of NavigationMachine.kt. */
export class NavigationMachine {
  state: NavState = 'LOADING';
  destinationId: string | null = null;
  currentNodeId: string | null = null;
  lastInstruction: string | null = null;
  lastSightingNodeId: string | null = null;

  private data: LoadedData | null = null;
  private route: string[] | null = null;
  private routeIndex = 0;
  private lastSightingAtMs = 0;
  private pendingDestination: string | null = null;
  private timerStartMs = 0;
  private reminderCount = 0;
  private helpSpoken = false;
  private paused = false;
  private stateBeforeError: NavState = 'HOME';
  private failedKind: ErrorKind = 'DATA';

  constructor(private now: () => number, private stepLengthM: () => number = () => Config.WALKING_STEP_LENGTH_M) {}

  private get guiding() {
    return this.state === 'WAITING' || this.state === 'GUIDING';
  }

  handle(e: NavEvent): Action[] {
    const out: Action[] = [];
    switch (e.type) {
      case 'DataLoaded': this.onDataLoaded(e.result, out); break;
      case 'DestinationChosen': this.onDestinationChosen(e.destinationId, e.cameraPermissionGranted, out); break;
      case 'PermissionResult': this.onPermission(e.granted, out); break;
      case 'Sighting': this.onSighting(e.markerId, out); break;
      case 'Tick': this.onTick(out); break;
      case 'RepeatPressed': this.onRepeat(out); break;
      case 'WhereAmIPressed': this.onWhereAmI(out); break;
      case 'StopPressed': this.onStop(out); break;
      case 'ChooseAnotherPressed': this.onChooseAnother(out); break;
      case 'ErrorOccurred': this.onError(e.kind, e.message, out); break;
      case 'RetryPressed': this.onRetry(out); break;
      case 'BackPressed': this.onBack(out); break;
      case 'AppBackgrounded': this.onBackgrounded(out); break;
      case 'AppForegrounded': this.onForegrounded(out); break;
    }
    return out;
  }

  private onDataLoaded(result: LoadResult, out: Action[]) {
    if (this.state !== 'LOADING') return;
    if (result.ok) {
      this.data = result.data;
      this.state = 'HOME';
      out.push(speak(Phrases.READY));
    } else {
      this.raiseError('DATA', result.errors.join(' '), out);
    }
  }

  private onDestinationChosen(dest: string, cameraGranted: boolean, out: Action[]) {
    const d = this.data;
    if (!d || this.state !== 'HOME' || !d.map.destinations.includes(dest)) return;
    if (cameraGranted) {
      this.startGuidance(dest, out);
    } else {
      this.pendingDestination = dest;
      this.state = 'PERMISSION';
      out.push(speak(Phrases.ASKING_CAMERA_PERMISSION), { type: 'RequestPermission' });
    }
  }

  private onPermission(granted: boolean, out: Action[]) {
    if (this.state !== 'PERMISSION') return;
    const dest = this.pendingDestination;
    this.pendingDestination = null;
    if (granted && dest) {
      this.startGuidance(dest, out);
    } else {
      this.state = 'HOME';
      out.push(speak(Phrases.PERMISSION_DENIED), { type: 'ShowSettingsButton' });
    }
  }

  private startGuidance(dest: string, out: Action[]) {
    this.destinationId = dest;
    this.route = null;
    this.routeIndex = 0;
    this.currentNodeId = null;
    this.lastInstruction = null;
    this.lastSightingNodeId = null;
    this.paused = false;
    this.state = 'WAITING';
    this.resetTimer();
    out.push(log('destination_chosen', { id: dest }), { type: 'StartCamera' }, speak(Phrases.destinationChosen(this.nodeName(dest))));
  }

  private onSighting(markerId: number, out: Action[]) {
    const d = this.data;
    if (!d || !this.guiding || this.paused) return;
    const marker = markerById(d.markers, markerId);
    if (!marker) {
      out.push(log('unknown_marker', { marker: String(markerId) }));
      return;
    }
    const nodeId = marker.nodeId;
    this.currentNodeId = nodeId;
    this.lastSightingNodeId = nodeId;
    this.lastSightingAtMs = this.now();
    this.resetTimer();
    out.push(log('sighting', { marker: String(markerId), node: nodeId }));

    const dest = this.destinationId;
    if (!dest) return;
    if (nodeId === dest) {
      this.state = 'ARRIVED';
      out.push(log('arrival', { node: dest }), speak(arrivalText(d.map, dest), true, true), { type: 'StopCamera' });
      return;
    }
    if (this.state === 'WAITING') {
      this.plan(nodeId, marker.facingDeg, null, out);
      return;
    }
    const route = this.route;
    const index = route ? route.indexOf(nodeId) : -1;
    if (route && index > this.routeIndex) {
      this.routeIndex = index;
      this.speakInstruction(null, this.instruction(route, index, marker.facingDeg)!, out);
    } else if (route && index >= 0 && index < this.routeIndex) {
      this.plan(nodeId, marker.facingDeg, Phrases.backAt(this.nodeName(nodeId)), out);
    } else if (route && index === this.routeIndex) {
      out.push(log('repeat_sighting', { node: nodeId }));
    } else {
      out.push(log('off_route', { marker: String(markerId), node: nodeId }));
      this.plan(nodeId, marker.facingDeg, Phrases.OFF_ROUTE, out);
    }
  }

  private plan(nodeId: string, facingDeg: number, prefix: string | null, out: Action[]) {
    const d = this.data;
    const dest = this.destinationId;
    if (!d || !dest) return;
    const route = findRoute(d.map, nodeId, dest);
    if (!route) {
      out.push(speak(Phrases.destinationBehind(this.nodeName(dest))));
      return;
    }
    this.route = route;
    this.routeIndex = 0;
    this.state = 'GUIDING';
    this.speakInstruction(prefix, this.instruction(route, 0, facingDeg)!, out);
  }

  private speakInstruction(prefix: string | null, text: string, out: Action[]) {
    if (prefix === null) {
      out.push(speak(text));
      this.lastInstruction = text;
    } else {
      out.push(speak(prefix), speak(text, false));
      this.lastInstruction = `${prefix} ${text}`;
    }
    this.resetTimer();
  }

  private onTick(out: Action[]) {
    if (!this.guiding || this.paused) return;
    const elapsed = this.now() - this.timerStartMs;
    const due = Config.NO_MARKER_REMINDER_MS + this.reminderCount * Config.REMINDER_REPEAT_MS;
    if (this.reminderCount < Config.MAX_REMINDERS) {
      if (elapsed >= due) {
        this.reminderCount++;
        out.push(log('reminder', { count: String(this.reminderCount) }), speak(Phrases.NO_MARKER_REMINDER));
      }
    } else if (!this.helpSpoken && elapsed >= due) {
      this.helpSpoken = true;
      out.push(log('help'), speak(Phrases.HELP_AFTER_REMINDERS));
    }
  }

  private onRepeat(out: Action[]) {
    if (this.state !== 'GUIDING' || !this.lastInstruction) return;
    this.resetTimer();
    out.push(speak(this.lastInstruction));
  }

  private onWhereAmI(out: Action[]) {
    if (!this.guiding) return;
    const node = this.lastSightingNodeId;
    out.push(
      speak(
        node === null
          ? Phrases.WHERE_AM_I_NOTHING
          : Phrases.whereAmI(this.nodeName(node), Math.floor((this.now() - this.lastSightingAtMs) / 1000)),
      ),
    );
  }

  private onStop(out: Action[]) {
    if (!this.guiding) return;
    this.state = 'HOME';
    out.push({ type: 'StopCamera' }, speak(Phrases.GUIDANCE_STOPPED));
  }

  private onChooseAnother(out: Action[]) {
    if (this.state !== 'ARRIVED') return;
    this.state = 'HOME';
    out.push({ type: 'StopCamera' }, speak(Phrases.READY));
  }

  private onError(kind: ErrorKind, message: string, out: Action[]) {
    if (this.state === 'ERROR') return;
    if (kind === 'CAMERA' && !this.guiding) return;
    this.raiseError(kind, message, out);
  }

  private raiseError(kind: ErrorKind, message: string, out: Action[]) {
    if (this.state !== 'ERROR') this.stateBeforeError = this.state;
    this.failedKind = kind;
    this.state = 'ERROR';
    out.push(log('error', { kind, message }), { type: 'ShowError', message, kind }, speak(message));
  }

  private onRetry(out: Action[]) {
    if (this.state !== 'ERROR') return;
    const b = this.stateBeforeError;
    this.state = b === 'LOADING' || b === 'WAITING' || b === 'GUIDING' ? b : 'HOME';
    if (this.guiding) this.resetTimer();
    out.push(speak(Phrases.RETRYING), { type: 'RetryStep', kind: this.failedKind });
  }

  private onBack(out: Action[]) {
    if (this.state !== 'ERROR') return;
    this.state = 'HOME';
    out.push({ type: 'StopCamera' });
  }

  private onBackgrounded(out: Action[]) {
    if (!this.guiding || this.paused) return;
    this.paused = true;
    out.push({ type: 'PauseCamera' });
  }

  private onForegrounded(out: Action[]) {
    if (!this.guiding || !this.paused) return;
    this.paused = false;
    this.resetTimer();
    out.push({ type: 'ResumeCamera' });
  }

  private resetTimer() {
    this.timerStartMs = this.now();
    this.reminderCount = 0;
    this.helpSpoken = false;
  }

  private nodeName(id: string) {
    const n = this.data ? nodeOf(this.data.map, id) : undefined;
    return n ? speechName(n) : id;
  }

  private instruction(route: string[], index: number, facingDeg: number) {
    const d = this.data!;
    const markerNodes = new Set(d.markers.markers.map((m) => m.nodeId));
    return routeInstruction(d.map, markerNodes, route, index, facingDeg, this.stepLengthM());
  }
}
