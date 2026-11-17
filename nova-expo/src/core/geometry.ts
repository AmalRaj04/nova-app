import { Config } from './config';

export type TurnWord =
  | 'STRAIGHT' | 'BEAR_RIGHT' | 'BEAR_LEFT' | 'TURN_RIGHT' | 'TURN_LEFT'
  | 'SHARP_RIGHT' | 'SHARP_LEFT' | 'AROUND';

export const normalize360 = (deg: number) => {
  const r = deg % 360;
  return r < 0 ? r + 360 : r;
};

/** Bearing of the move from point 1 to point 2: atan2(dx, -dy) mod 360 (y points down). */
export const bearingBetween = (x1: number, y1: number, x2: number, y2: number) =>
  normalize360((Math.atan2(x2 - x1, -(y2 - y1)) * 180) / Math.PI);

/** Turn from facing to heading, wrapped to (-180, 180]. Positive is right. */
export function turnAngle(facingDeg: number, headingDeg: number) {
  const a = normalize360(headingDeg - facingDeg);
  return a > 180 ? a - 360 : a;
}

/** Each upper limit belongs to the lower band (20 is still straight). */
export function classifyTurn(angle: number): TurnWord {
  const size = Math.abs(angle);
  const right = angle > 0;
  if (size <= Config.STRAIGHT_MAX_DEG) return 'STRAIGHT';
  if (size <= Config.BEAR_MAX_DEG) return right ? 'BEAR_RIGHT' : 'BEAR_LEFT';
  if (size <= Config.TURN_MAX_DEG) return right ? 'TURN_RIGHT' : 'TURN_LEFT';
  if (size <= Config.SHARP_MAX_DEG) return right ? 'SHARP_RIGHT' : 'SHARP_LEFT';
  return 'AROUND';
}
