import { ExpressionTree, NestedExpression } from '../';

export type GmMathNode = {
  parent: GmMathNode | null;
  ls: GmMathNode | null;
  rs: GmMathNode | null;
  children: GmMathNode[];
  value: string;
  name: string;
  hidden?: boolean;
  to_ascii: (options?: ToAsciiOptionsType) => string;
  is_group: (...types: string[]) => boolean;
  getSelectorOfNodes: (nodes: GmMathNode[] | GmMathNode) => string;
  type?: 'AlgebraModel';
};

// Todo: find out what the type of ToAsciiOptionsType is
type ToAsciiOptionsType = any;

type GmFraction = GmMathNode & {
  get_top: () => GmMathNode[];
  get_bottom: () => GmMathNode[];
  get_fraction_bar: () => GmMathNode;
};

/** Creates a map from all ExpressionTree paths to a list of GM math nodes
 * at that path. */
export function createPathMap(
  gmTree: GmMathNode,
  tree: ExpressionTree,
): Map<string, GmMathNode[]> {
  const map = new Map();
  tree = flattenPartialExpressions(tree);
  tree = rearrangeNegativeProducts(tree);
  annotate(gmTree, tree, map);
  return map;
}

/**
 * Hides the implicit multiplication signs in the passed GM tree in accordance with the
 * passed solver tree.
 */
export function hideImplicitMultiplicationSigns(
  gmTree: GmMathNode,
  tree: ExpressionTree,
  pathMap: Map<string, GmMathNode[]>,
) {
  if (['Product', 'ImplicitProduct', 'SmartProduct'].includes(tree.type)) {
    (tree as NestedExpression).args.forEach((factor, index) => {
      const path = `${factor.path}:op`;
      const show =
        tree.type === 'SmartProduct'
          ? tree.signs[index]
          : tree.type === 'ImplicitProduct'
          ? false
          : index > 0;
      pathMap.get(path)?.forEach((node) => (node.hidden = !show));
    });
  } else if ('args' in tree) {
    tree.args.forEach((arg) => {
      hideImplicitMultiplicationSigns(gmTree, arg, pathMap);
    });
  }
}

/** Adds a "path" field to each node in the passed gmTree that correspond to the correct
 * paths in the passed solver ExpressionTree. */
function annotate(
  gmTree: GmMathNode,
  tree: ExpressionTree,
  map: Map<string, GmMathNode[]>,
) {
  // if (!gmTree) debugger;
  if (gmTree.name === 'brackets') {
    // brackets are not part of the tree structure in the solver
    annotate(gmTree.children[1], tree, map);
    // only annotate the brackets themselves if they are not inner nested
    // brackets
    if (gmTree.parent?.name !== 'brackets' && gmTree.parent?.name !== 'abs-val') {
      appendMap(gmTree, `${tree.path}:()`, map);
      appendMap(gmTree.children[0], `${tree.path}:(`, map);
      appendMap(gmTree.children[2], `${tree.path}:)`, map);
    }
    return;
  }
  appendMap(gmTree, tree.path, map);
  const treeType = tree.type;
  switch (treeType) {
    case 'Number':
    case 'Variable':
      break;
    case 'Sum':
      gmTree.children.forEach((addend, i) => {
        // could be inside a partial sum, use tree.args[i] instead or /${i}
        const path = tree.args[i].path;
        appendMap(addend, `${path}:group`, map);
        const addendHasBrackets = !!tree.args[i].decorators?.length;
        if (
          (tree.args[i].type === 'Minus' || tree.args[i].type === 'PlusMinus') &&
          !addendHasBrackets
        ) {
          annotate(addend, tree.args[i], map);
        } else {
          appendMap(
            addend.children[0]!,
            path + (addendHasBrackets ? ':op()' : ':op'),
            map,
          );
          annotate(addend.children[1], tree.args[i], map);
        }
      });
      break;
    case 'Plus':
      // not supported in GM
      annotate(gmTree, tree.args[0], map);
      break;
    case 'Minus':
    case 'PlusMinus':
    case 'DivideBy':
      appendMap(gmTree.children[0], `${tree.path}:op`, map);
      annotate(gmTree.children[1], tree.args[0], map);
      break;
    case 'Product':
    case 'ImplicitProduct':
    case 'SmartProduct':
      gmTree.children.forEach((factor, i) => {
        appendMap(factor, `${tree.path}/${i}:group`, map);
        if (tree.args[i].type === 'DivideBy') {
          annotate(factor, tree.args[i], map);
        } else {
          appendMap(factor.children[0]!, `${tree.path}/${i}:op`, map);
          annotate(factor.children[1], tree.args[i], map);
        }
      });
      break;
    case 'Fraction':
      // GM puts a list of muldiv nodes into the numerator and denominator while the
      // Solver either directly puts a single node, or wraps several nodes into a Product.
      appendMap((gmTree as GmFraction).get_fraction_bar(), `${tree.path}:/`, map);
      if (['Product', 'ImplicitProduct', 'SmartProduct'].includes(tree.args[0].type)) {
        (gmTree as GmFraction).get_top().forEach((muldiv, i) => {
          appendMap(muldiv, `${tree.path}/0`, map);
          appendMap(muldiv.children[0]!, `${tree.path}/0/${i}:op`, map);
          annotate(muldiv.children[1], (tree.args[0] as NestedExpression).args[i], map);
        });
      } else {
        // single node in numerator
        appendMap(gmTree.children[0], `${tree.path}/0:group`, map);
        appendMap(gmTree.children[0].children[0]!, `${tree.path}/0:op`, map);
        annotate(gmTree.children[0].children[1], tree.args[0], map);
      }

      if (['Product', 'ImplicitProduct', 'SmartProduct'].includes(tree.args[1].type)) {
        (gmTree as GmFraction).get_bottom().forEach((muldiv, i) => {
          appendMap(muldiv, `${tree.path}/1`, map);
          appendMap(muldiv.children[0]!, `${tree.path}/1/${i}:op`, map);
          annotate(muldiv.children[1], (tree.args[1] as NestedExpression).args[i], map);
        });
      } else {
        // single node in denominator
        const node = gmTree.children[gmTree.children.length - 1];
        appendMap(node, `${tree.path}/1:group`, map);
        appendMap(node.children[0]!, `${tree.path}/1:op`, map);
        annotate(node.children[1], tree.args[1], map);
      }
      break;
    case 'Power':
      annotate(gmTree.children[0], tree.args[0], map);
      appendMap(gmTree.children[1], `${tree.path}/1:group`, map);
      annotate(gmTree.children[1].children[0], tree.args[1], map);
      break;
    case 'SquareRoot':
      appendMap(gmTree.children[0], `${tree.path}:idx`, map);
      appendMap(gmTree.children[1], `${tree.path}:op`, map);
      appendMap(gmTree.children[2], `${tree.path}:op`, map);
      annotate(gmTree.children[3], tree.args[0], map);
      break;
    case 'Root':
      appendMap(gmTree.children[0], `${tree.path}:idx`, map);
      annotate(gmTree.children[0].children[0], tree.args[1], map);
      appendMap(gmTree.children[1], `${tree.path}:op`, map);
      appendMap(gmTree.children[2], `${tree.path}:op`, map);
      annotate(gmTree.children[3], tree.args[0], map);
      break;
    case 'AbsoluteValue':
      appendMap(gmTree, `${tree.path}:()`, map);
      appendMap(gmTree.children[0], `${tree.path}:(`, map);
      annotate(gmTree.children[1], tree.args[0], map);
      appendMap(gmTree.children[2], `${tree.path}:)`, map);
      break;
    case 'Equation':
    case 'LessThan':
    case 'GreaterThan':
    case 'LessThanEqual':
    case 'GreaterThanEqual':
      annotate(gmTree.children[0], tree.args[0], map);
      appendMap(gmTree.children[1], `${tree.path}:op`, map);
      annotate(gmTree.children[2], tree.args[1], map);
      break;
    case 'EquationSystem':
    case 'MixedNumber':
    case 'Name':
    case '/undefined/':
    case '/infinity/':
    case '/reals/':
    case 'AddEquations':
    case 'SubtractEquations':
    case 'EquationUnion':
    case 'StatementWithConstraint':
    case 'Solution':
    case 'Identity':
    case 'Contradiction':
    case 'ImplicitSolution':
    case 'SetSolution':
    case 'VariableList':
    case 'Tuple':
    case 'FiniteSet':
    case 'CartesianProduct':
    case 'SetUnion':
    case 'OpenInterval':
    case 'ClosedInterval':
    case 'OpenClosedInterval':
    case 'ClosedOpenInterval':
      throw new Error('Unsupported type: ' + tree.type);
    default:
      needToImplement(treeType);
  }
}

function needToImplement(type: never): never {
  throw new Error(`Need to implement: ${type}`);
}

/** Adjust the Solver tree structure for products with a leading "-" to match GM's
 * structure. The solver puts a negative sign directly around a product without
 * parentheses, but gmath doesn't allow that and puts the negative sign around the first
 * factor. */
function rearrangeNegativeProducts(
  expr: ExpressionTree,
  parentIsSum = false,
): ExpressionTree {
  if (
    expr.type === 'Minus' &&
    !parentIsSum &&
    (expr.args[0].type === 'Product' ||
      expr.args[0].type === 'ImplicitProduct' ||
      expr.args[0].type === 'SmartProduct') &&
    !expr.args[0].decorators?.length
  ) {
    const prod = expr.args[0];
    return {
      ...prod,
      args: [
        { ...expr, args: [rearrangeNegativeProducts(prod.args[0])] },
        ...prod.args.slice(1).map((arg) => rearrangeNegativeProducts(arg)),
      ],
    };
  } else {
    if (!('args' in expr)) return expr;
    else
      return {
        ...expr,
        args: expr.args.map((arg) => rearrangeNegativeProducts(arg, expr.type === 'Sum')),
      };
  }
}

/** Add entry to path map array. */
function appendMap(node: GmMathNode, path: string, map: Map<string, GmMathNode[]>) {
  const existing = map.get(path) || [];
  existing.push(node);
  map.set(path, existing);
}

/** Returns a new ExpressionTree with all nested partial expressions removed to match GM's
 * structure. */
function flattenPartialExpressions(expr: ExpressionTree): ExpressionTree {
  if (expr.type === 'Sum' || expr.type === 'SmartProduct') {
    return {
      ...expr,
      args: expr.args
        .flatMap((child) => {
          if (
            (child.type === 'Sum' || child.type === 'SmartProduct') &&
            child.decorators?.[0] === 'PartialBracket'
          )
            return child.args;
          else return child;
        })
        .map(flattenPartialExpressions),
    };
  } else {
    if (!('args' in expr)) return expr;
    else
      return {
        ...expr,
        args: expr.args.map(flattenPartialExpressions),
      };
  }
}