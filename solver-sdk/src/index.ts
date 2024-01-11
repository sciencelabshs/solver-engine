/*
 * Copyright (c) 2023 GeoGebra GmbH, office@geogebra.org
 * This file is part of GeoGebra
 *
 * The GeoGebra source code is licensed to you under the terms of the
 * GNU General Public License (version 3 or later)
 * as published by the Free Software Foundation,
 * the current text of which can be found via this link:
 * https://www.gnu.org/licenses/gpl.html ("GPL")
 * Attribution (as required by the GPL) should take the form of (at least)
 * a mention of our name, an appropriate copyright notice
 * and a link to our website located at https://www.geogebra.org
 *
 * For further details, please see https://www.geogebra.org/license
 *
 */

import { MathJson } from './types';
import {
  LatexSettings,
  LatexTransformer,
  MathWords,
  treeToLatex,
  treeToSolver,
} from './renderer';
import { jsonToTree, latexToTree } from './parser';

export * from './renderer';
export * from './api';
export * from './parser';
export * from './paths';
export * from './types';
export * from './solutions';
export * from './translations';
export * from './math-generator';
export * from './graphing';

export function jsonToLatex(
  json: MathJson,
  settings?: LatexSettings,
  transformer?: LatexTransformer,
  mathWords?: MathWords,
): string {
  return treeToLatex(jsonToTree(json), settings, transformer, mathWords);
}

export function latexToSolver(latex: string): string {
  return treeToSolver(latexToTree(latex));
}
