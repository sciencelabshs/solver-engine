/******************************************
 * Top-level rendering functions
 *******************************************/
import * as solverSDK from '@geogebra/solver-sdk';
import {
  AlternativeJson2,
  jsonToTree,
  MathJson,
  MathJson2,
  Metadata,
  PlanSelection,
  PlanSelectionJson2,
  PlanSelectionSolver,
  TaskJson2,
  Transformation,
  TransformationJson2,
} from '@geogebra/solver-sdk';
import { colorSchemes, settings } from './settings';
import {
  clone,
  containsNonTrivialStep,
  isCosmeticTransformation,
  isInvisibleChangeStep,
  isPedanticStep,
  isRearrangementStep,
  isThroughStep,
} from './util';
import { renderTest } from './render-test';
import { translationData } from './translations';
import { LatexTransformer } from '@geogebra/solver-sdk/src/renderer/tree-to-latex';

const findTransformationInSelections = (selections: PlanSelection[], methodId: string) => {
  for (const selection of selections) {
    if (selection.metadata.methodId === methodId) {
      return selection.transformation;
    }
  }
};

export const renderPlanSelections = (
  selections: PlanSelectionJson2[],
  testSelections: PlanSelectionSolver[],
) => {
  if (selections) {
    selections = selections.filter((selection) => containsNonTrivialStep(selection.transformation));
  }
  if (!selections || selections.length === 0) {
    return /* HTML */ `<div class="selections">No plans found</div>`;
  }
  return /* HTML */ `
    <div class="selections">
      ${selections.length} plans found
      <ol>
        ${selections
          .map(
            (selection) => /* HTML */ `<li>
              <div class="plan-selection">
                <div class="plan-id">${selection.metadata.methodId}</div>
                ${renderTransformationAndTest(
                  selection.transformation,
                  findTransformationInSelections(testSelections, selection.metadata.methodId),
                )}
              </div>
            </li>`,
          )
          .join('')}
      </ol>
    </div>
  `;
};

export const renderTransformationAndTest = (
  trans: TransformationJson2,
  testTrans?: Transformation,
  depth = 0,
) => {
  return /* HTML */ ` ${renderTransformation(trans, depth)}
  ${testTrans ? renderTest(testTrans) : ''}`;
};

/******************************************
 * Rendering a transformation
 ******************************************/

const renderTransformation = (trans: TransformationJson2, depth = 0): string => {
  const isThrough = isThroughStep(trans);
  if (!settings.showThroughSteps && isThrough) {
    return renderTransformation(trans.steps![0], depth);
  }

  const [fromColoring, toColoring] = createColorMaps(trans);
  const render = (expr: MathJson2, coloring?: LatexTransformer) =>
    solverSDK.treeToLatex(solverSDK.jsonToTree(expr, trans.path), settings.latexSettings, coloring);
  return /* HTML */ ` <div class="trans ${isThrough ? 'through-step' : ''}">
    ${renderExplanation(trans.explanation)}
    <div class="expr">
      ${renderExpression(
        `${render(
          trans.fromExpr,
          fromColoring,
        )} {\\color{#8888ff}\\thickspace\\longmapsto\\thickspace} ${render(
          trans.toExpr,
          toColoring,
        )}`,
      )}
    </div>
    ${renderSteps(trans.steps, depth, depth >= 0 || isThrough)}
    ${renderTasks(trans.tasks, depth, depth >= 0)}
    ${renderAlternatives(trans.alternatives, depth, false)}
  </div>`;
};

const createColorMaps = (
  trans: Transformation,
): [LatexTransformer | undefined, LatexTransformer | undefined] => {
  // First deal with the special case that the whole expression is transformed.  In this case there is no need to color
  // the path mapping.
  if (trans.pathMappings.length === 1) {
    const mapping = trans.pathMappings[0];
    if (mapping.type === 'Transform' && mapping.fromPaths[0] === trans.path) {
      return [undefined, undefined];
    }
  }
  // Else proceed with finding appropriate colors for the path mappings.
  const colors = colorSchemes[settings.selectedColorScheme];
  if (colors) {
    const [fromCM, toCM] = solverSDK.createColorMaps(trans.pathMappings, colors);
    return [solverSDK.coloringTransformer(fromCM), solverSDK.coloringTransformer(toCM)];
  } else {
    return [undefined, undefined];
  }
};

const renderSteps = (
  steps: TransformationJson2[] | null,
  depth = 0,
  open = false,
  title = '',
): string => {
  if (steps === null || steps.length === 0) {
    return '';
  }

  const renderedSteps = preprocessSteps(steps)
    .map((step) => {
      if (!settings.showPedanticSteps && isPedanticStep(step)) {
        return null;
      } else if (!settings.showCosmeticSteps && isCosmeticTransformation(step)) {
        return /* HTML */ `<span class="note">
          The expression has been rewritten in a normalized form
        </span>`;
      } else {
        return /* HTML */ `<li>${renderTransformation(step, depth - 1)}</li>`;
      }
    })
    .filter((step) => !!step);

  return /* HTML */ ` <details class="steps" ${open ? 'open' : ''}>
    <summary>
      ${title} ${renderedSteps.length} ${renderedSteps.length === 1 ? 'step' : 'steps'}
    </summary>
    <ol>
      ${renderedSteps.join('')}
    </ol>
  </details>`;
};

const renderTasks = (tasks: TaskJson2[] | null, depth = 0, open = false) => {
  if (tasks === null || tasks.length === 0) {
    return '';
  }
  return /* HTML */ ` <details class="tasks" ${open ? 'open' : ''}>
    <summary>${tasks.length} ${tasks.length === 1 ? 'task' : 'tasks'}</summary>
    <ol>
      ${tasks.map((task) => `<li>${renderTask(task, depth - 1)}</li>`).join('')}
    </ol>
  </details>`;
};

const renderAlternatives = (alternatives: AlternativeJson2[] | null, depth = 0, open = false) => {
  if (alternatives === null || alternatives.length === 0) {
    return '';
  }
  return `<details class='alternatives' ${open ? 'open' : ''}>
    <summary>${
      alternatives.length === 1
        ? 'alternative method'
        : alternatives.length + ' alternative methods'
    }</summary>
    <ol>
    ${alternatives
      .map(
        (alt) =>
          `<li>
              ${renderExplanation(alt.explanation)}
              ${renderSteps(alt.steps, depth, open)}
          </li>`,
      )
      .join('')}
    </ol>
    </details>`;
};

const renderTask = (task: TaskJson2, depth = 0): string => {
  return /* HTML */ `<div class="task">
    ${renderExplanation(task.explanation)}
    ${!task.steps
      ? renderExpression(task.startExpr)
      : task.steps.length === 1
      ? renderTransformation(task.steps[0], depth - 1)
      : renderTaskTransformation(task) + renderSteps(task.steps, depth - 1, depth >= 0)}
  </div>`;
};

const renderTaskTransformation = (task: TaskJson2) => {
  if (task.steps === null) {
    return '';
  }
  const fromExpr = jsonToTree(task.startExpr);
  const toExpr = jsonToTree(task.steps[task.steps.length - 1].toExpr);

  const rendering =
    solverSDK.specialTransformationLatex(fromExpr, toExpr, settings.latexSettings) ||
    `${solverSDK.treeToLatex(fromExpr, settings.latexSettings)} 
      {\\color{#8888ff}\\thickspace\\longmapsto\\thickspace} 
      ${solverSDK.treeToLatex(toExpr, settings.latexSettings)}`;

  return `<div className='expr'>${renderExpression(rendering)}</div>`;
};

const preprocessSteps = (steps: TransformationJson2[]) => {
  // We clone because we may edit the objects
  steps = preprocessInvisibleChangeSteps(clone(steps));
  if (settings.showRearrangementSteps || steps.every((step) => isRearrangementStep(step))) {
    return steps;
  }
  // Rearrangement steps are "collapsed" with the previous step if it exists
  const processedSteps = [];
  let lastStep = null;
  for (const step of steps) {
    if (lastStep !== null && isRearrangementStep(step)) {
      lastStep.toExpr = step.toExpr;
    } else {
      lastStep = step;
      processedSteps.push(step);
    }
  }
  return processedSteps;
};

const preprocessInvisibleChangeSteps = (steps: TransformationJson2[]) => {
  if (settings.showInvisibleChangeSteps || steps.every((step) => isInvisibleChangeStep(step))) {
    return steps;
  }
  // InvisibleChange steps are "collapsed" with the next step if it exists
  const processedStepsReversed = [];
  let lastProcessedStep = null;
  for (const step of steps.reverse()) {
    if (lastProcessedStep !== null && isInvisibleChangeStep(step)) {
      lastProcessedStep.fromExpr = step.fromExpr;
    } else {
      lastProcessedStep = step;
      processedStepsReversed.push(step);
    }
  }
  return processedStepsReversed.reverse();
};

const renderWarning = (content: string) =>
  /* HTML */ `<div class="warning${settings.hideWarnings ? ' hidden' : ''}">${content}</div>`;

const getExplanationString = (expl: Metadata) => {
  let explanationString = translationData[expl.key];
  const warnings = [];
  if (!explanationString) {
    warnings.push(`Missing default translation for ${expl.key}`);
    explanationString = `${expl.key}(${[...expl.params.keys()].map((i) => `%${i + 1}`).join()})`;
  }

  for (const [i, param] of expl.params.entries()) {
    // replacing "%1", "%2", ... with the respective rendered expression
    if (explanationString.includes('%' + (i + 1))) {
      explanationString = explanationString.replaceAll(
        '%' + (i + 1),
        renderExpression(param.expression),
      );
    } else {
      warnings.push(
        `Missing %${i + 1} in default translation, should contain ${renderExpression(
          param.expression,
        )}`,
      );
    }
  }
  const unusedPlaceholders = explanationString.match(/%[1-9]/g);
  if (unusedPlaceholders) {
    for (const placeholder of unusedPlaceholders) {
      warnings.push(`Missing parameter for placeholder ${placeholder}`);
    }
  }
  return { explanationString, warnings };
};

const renderExplanation = (expl?: Metadata | null) => {
  if (!expl) {
    return '';
  }

  const { explanationString, warnings } = getExplanationString(expl);

  return /* HTML */ ` <div class="plan-explanation">
    <div class="translation-key${!settings.showTranslationKeys ? ' hidden' : ''}">${expl.key}</div>
    ${explanationString ? /* HTML */ `<div title="${expl.key}">${explanationString}</div>` : ''}
    ${warnings ? warnings.map(renderWarning).join('') : ''}
  </div>`;
};

const renderExpression = (expr: MathJson2 | MathJson | string) =>
  `\\(\\displaystyle ${
    typeof expr === 'string' ? expr : solverSDK.jsonToLatex(expr, settings.latexSettings)
  }\\)`;
