import { TurnWord } from './geometry';

/** Every sentence the app speaks, in one place (same wording as the Kotlin app). */
export const Phrases = {
  READY: 'NOVA is ready. Where do you want to go?',
  OFF_ROUTE: 'This is not on your route. New route.',
  NO_MARKER_REMINDER: 'I have not seen a marker. Point the phone forward and move slowly.',
  HELP_AFTER_REMINDERS: 'Still no marker. Press Repeat, or ask someone nearby for help.',
  WHERE_AM_I_NOTHING: 'No marker seen yet.',
  GUIDANCE_STOPPED: 'Guidance stopped.',
  ASKING_CAMERA_PERMISSION: 'NOVA needs to use the camera to read markers. Please allow it when asked.',
  RETRYING: 'Trying again.',
  PERMISSION_DENIED: 'NOVA needs the camera to read markers. Please allow the camera in settings.',
  destinationChosen: (d: string) => `Going to ${d}. Point the phone forward at a marker.`,
  arrival: (d: string, doorSide?: string | null) =>
    doorSide ? `You have arrived. ${d} is on your ${doorSide}.` : 'You have arrived.',
  destinationBehind: (d: string) =>
    `${d} is behind you. This version cannot guide backwards. Ask someone nearby to help, or return to the entrance.`,
  backAt: (p: string) => `You are back at ${p}.`,
  whereAmI: (p: string, secondsAgo: number) => `Last marker: ${p}, ${secondsAgo} seconds ago.`,
  placeSentence: (name: string, isRoom: boolean) => (isRoom ? `You are beside ${name}.` : `You are at ${name}.`),
  turn(word: TurnWord) {
    switch (word) {
      case 'STRAIGHT': return 'Walk straight';
      case 'BEAR_RIGHT': return 'Bear right';
      case 'BEAR_LEFT': return 'Bear left';
      case 'TURN_RIGHT': return 'Turn right';
      case 'TURN_LEFT': return 'Turn left';
      case 'SHARP_RIGHT': return 'Turn sharp right';
      case 'SHARP_LEFT': return 'Turn sharp left';
      case 'AROUND': return 'Turn around';
    }
  },
  steps: (n: number) => (n === 1 ? '1 step' : `${n} steps`),
  walkStraight: (n: number) => `Walk straight about ${Phrases.steps(n)}.`,
  walk: (n: number) => `Walk about ${Phrases.steps(n)}.`,
  thenWalk: (n: number) => `Then walk about ${Phrases.steps(n)}.`,
  turnSentence: (w: TurnWord) => `${Phrases.turn(w)}.`,
  climb: (n: number) => `Climb ${Phrases.steps(n)} up.`,
  thenClimb: (n: number) => `Then climb ${Phrases.steps(n)} up.`,
};
