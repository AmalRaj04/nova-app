import * as Speech from 'expo-speech';

let protectedActive = false;

/**
 * Speaks with the phone's own text-to-speech.
 * interrupt = true cuts off whatever is playing; false queues after it.
 * protect = true means later speech may queue behind it but never cut it off (arrival).
 */
export function speakOwn(text: string, interrupt: boolean, protect: boolean) {
  if (protectedActive) interrupt = false;
  if (interrupt) Speech.stop();
  if (protect) protectedActive = true;
  Speech.speak(text, {
    language: 'en',
    rate: 1.0,
    onDone: () => {
      if (protect) protectedActive = false;
    },
    onStopped: () => {
      if (protect) protectedActive = false;
    },
    onError: () => {
      if (protect) protectedActive = false;
    },
  });
}

export function stopSpeaking() {
  protectedActive = false;
  Speech.stop();
}
