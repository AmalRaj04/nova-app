import { Config } from './config';
import { classifyTurn, turnAngle } from './geometry';
import { Phrases } from './phrases';
import { Leg, buildLeg } from './route';
import { MapData, isRoom, nodeOf, speechName } from './types';

const roundHalfUp = (x: number) => Math.floor(x + 0.5 + 1e-9);

/** Spoken text for one leg. Walk edges are summed and rounded once; stairs use exact step counts. */
export function legInstruction(map: MapData, leg: Leg, facingDeg: number, stepLengthM = Config.WALKING_STEP_LENGTH_M) {
  const start = nodeOf(map, leg.startNodeId)!;
  const sentences = [Phrases.placeSentence(speechName(start), isRoom(start))];
  const turn = classifyTurn(turnAngle(facingDeg, leg.firstHeadingDeg));
  let movementSpoken = false;
  let walkMetres = 0;
  let firstGroup = true;

  const flushWalk = () => {
    if (walkMetres >= Config.MIN_WALK_MENTION_M) {
      const n = roundHalfUp(walkMetres / stepLengthM);
      sentences.push(
        firstGroup && turn === 'STRAIGHT' ? Phrases.walkStraight(n) : firstGroup ? Phrases.walk(n) : Phrases.thenWalk(n),
      );
      movementSpoken = true;
    }
    walkMetres = 0;
  };

  if (turn !== 'STRAIGHT') sentences.push(Phrases.turnSentence(turn));
  for (const edge of leg.edges) {
    if (edge.kind === 'stairs') {
      flushWalk();
      firstGroup = false;
      const n = edge.steps!;
      sentences.push(movementSpoken ? Phrases.thenClimb(n) : Phrases.climb(n));
      movementSpoken = true;
    } else {
      walkMetres += edge.weight;
    }
  }
  flushWalk();
  for (const e of leg.edges) {
    const h = (e.hint ?? '').trim();
    if (h) sentences.push(h);
  }
  return sentences.join(' ');
}

/** Instruction for the leg starting at route[startIndex], or null at the end of the route. */
export function routeInstruction(
  map: MapData,
  markerNodeIds: Set<string>,
  route: string[],
  startIndex: number,
  facingDeg: number,
  stepLengthM = Config.WALKING_STEP_LENGTH_M,
) {
  const leg = buildLeg(map, markerNodeIds, route, startIndex);
  return leg ? legInstruction(map, leg, facingDeg, stepLengthM) : null;
}

export function arrivalText(map: MapData, destinationId: string) {
  const n = nodeOf(map, destinationId)!;
  return Phrases.arrival(speechName(n), n.doorSide);
}
