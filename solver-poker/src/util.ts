import { Transformation } from '@geogebra/solver-sdk';
import { settings } from './settings';
import * as assert from 'assert';

export const isRearrangementStep = (transformation: Transformation) => {
  return transformation.tags && transformation.tags.includes('Rearrangement');
};

export const isPedanticStep = (transformation: Transformation) => {
  return transformation.tags && transformation.tags.includes('Pedantic');
};

export const isCosmeticStep = (transformation: Transformation) => {
  return transformation.tags && transformation.tags.includes('Cosmetic');
};

export function isInvisibleChangeStep(transformation: Transformation): boolean {
  return (
    (transformation.tags && transformation.tags.includes('InvisibleChange')) ||
    (!transformation.tasks?.length &&
      transformation.steps?.length === 1 &&
      isInvisibleChangeStep(transformation.steps[0]))
  );
}

export const isCosmeticTransformation = (transformation: Transformation): boolean => {
  if (!transformation.steps || !transformation.steps.length) {
    return isCosmeticStep(transformation);
  }

  // Needs the cast because of a bug in Typescript lmao
  // Strangely it is not needed for `some`
  // Remove when they have fixed this problem
  return (transformation.steps as Transformation[]).every(isCosmeticTransformation);
};

const isTrivialStep = (transformation: Transformation) => {
  if (!transformation.tags) return false;

  return (
    (!settings.showRearrangementSteps && isRearrangementStep(transformation)) ||
    (!settings.showPedanticSteps && isPedanticStep(transformation)) ||
    (!settings.showCosmeticSteps && isCosmeticStep(transformation)) ||
    (!settings.showInvisibleChangeSteps && isInvisibleChangeStep(transformation))
  );
};

export const containsNonTrivialStep = (transformation: Transformation): boolean => {
  if (!transformation.steps || !transformation.steps.length) {
    return !isTrivialStep(transformation);
  }

  return transformation.steps.some(containsNonTrivialStep);
};

export const isThroughStep = (trans: Transformation) =>
  !!trans.steps && trans.steps.length === 1 && trans.steps[0].path === trans.path;

// If we end up using this more, then we can change to a library that has a faster cloning
// solution (the clone in jsondiffpatch), but for now it is probably faster to not have an
// extra dependency just for this.
export const clone = (obj: ParsedJson) => JSON.parse(JSON.stringify(obj));

type ParsedJson = string | number | boolean | null | ParsedJson[] | { [key: string]: ParsedJson };