/* global renderMathInElement */

// Globally changes the rendering of steps.
let showThroughSteps = false;

// Holds all default translations as a key: translation map
let translationData = {};

const el = (id) => document.getElementById(id);

const isThroughStep = (trans) =>
    trans.steps &&
    trans.steps.length === 1 &&
    trans.steps[0].path === trans.path;

/******************************************
 * API access
 ******************************************/

const requestApplyPlan = async (
    planId,
    input,
    curriculum = "",
    format = "latex"
) => {
    const response = await fetch(`/api/v1.0-alpha0/plans/${planId}/apply`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ input, format, curriculum }),
    });
    return await response.json();
};

const requestSelectPlans = async (input, curriculum = "", format = "latex") => {
    const response = await fetch(`/api/v1.0-alpha0/selectPlans`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ input, format, curriculum }),
    });
    return await response.json();
};

const fetchPlans = () =>
    fetch("/api/v1.0-alpha0/plans/").then((response) => response.json());

const fetchDefaultTranslations = () =>
    fetch("./DefaultTranslations.json").then((resp) => resp.json());

/******************************************
 * Setting up
 ******************************************/

const initPlans = (plans) => {
    const options = plans
        .sort()
        .map((plan) => `<option value="${plan}">${plan}</option>`)
        .join("");

    el("plansSelect").innerHTML = `
        <option value="selectPlans">Select Plans</option>
        ${options}`;
    // Default to something simple
    el("plansSelect").value = "selectPlans";
};

/******************************************
 * Functions to execute plans and render the result
 *******************************************/

const selectPlansOrApplyPlan = (planId, input, curriculum) => {
    if (planId === "selectPlans") {
        return selectPlans(input, curriculum);
    } else {
        return applyPlan(planId, input, curriculum);
    }
};

const applyPlan = async (planId, input, curriculum = "") => {
    const result = await requestApplyPlan(planId, input, curriculum);
    el("source").innerHTML = JSON.stringify(result, null, 4);
    if (result.error !== undefined) {
        console.log(result);
        el(
            "result"
        ).innerHTML = `Error: ${result.error}<br/>Message: ${result.message}`;
    } else {
        const solverResult = await requestApplyPlan(
            planId,
            input,
            curriculum,
            "solver"
        );
        el("result").innerHTML = renderTransformationAndTest(
            result,
            solverResult,
            1
        );
        renderMathInElement(el("result"));
    }
};

const selectPlans = async (input, curriculum = "") => {
    const result = await requestSelectPlans(input, curriculum);
    el("source").innerHTML = JSON.stringify(result, null, 4);
    if (result.error !== undefined) {
        console.log(result);
        el(
            "result"
        ).innerHTML = `Error: ${result.error}<br/>Message: ${result.message}`;
    } else {
        console.log(result);
        const testResult = await requestSelectPlans(
            input,
            curriculum,
            "solver"
        );
        el("result").innerHTML = renderPlanSelections(result, testResult);
        renderMathInElement(el("result"));
    }
};

/******************************************
 * Top-level rendering functions
 *******************************************/

const findTransformationInSelections = (selections, methodId) => {
    for (let selection of selections) {
        if (selection.metadata.methodId === methodId) {
            return selection.transformation;
        }
    }
};

const renderPlanSelections = (selections, testSelections) => {
    if (!selections || selections.length === 0) {
        return `<div class="selections">No plans found</div>`;
    }
    return `
    <div class ="selections">${selections.length} plans found
        <ol>
            ${selections
                .map(
                    (selection) =>
                        `<li>${renderPlanSelection(
                            selection,
                            findTransformationInSelections(
                                testSelections,
                                selection.metadata.methodId
                            )
                        )}</li>`
                )
                .join("")}
        </ol>
    </div>
    `;
};

const renderPlanSelection = (selection, testTransformation) => {
    return `
    <div class="plan-selection">
        <div class="plan-id">${selection.metadata.methodId}</div>
        ${renderTransformationAndTest(
            selection.transformation,
            testTransformation
        )}
    </div>`;
};

const renderTransformationAndTest = (trans, testTrans, depth = 0) => {
    return `
    ${renderTransformation(trans, depth)}
    ${renderTest(testTrans)}`;
};

/******************************************
 * Rendering a transformation
 ******************************************/

const renderTransformation = (trans, depth = 0) => {
    const isThrough = isThroughStep(trans);
    if (isThrough && !showThroughSteps) {
        return renderTransformation(trans.steps[0], depth);
    }
    return `
    <div class="trans ${isThrough ? "through-step" : ""}">
        ${trans.planId ? `<div class="plan-id">${trans.planId}</div>` : ""}
        ${renderExplanation(trans.explanation)}
        <div class="expr">${renderExpression(
            `${trans.fromExpr} {\\color{#8888ff}\\thickspace\\longmapsto\\thickspace} ${trans.toExpr}`
        )}</div>
        ${renderSteps(trans.steps, depth, depth >= 0 || isThrough)}
    </div>`;
};

const renderSteps = (steps, depth = 0, open = false) => {
    if (steps === null || steps.length === 0) {
        return "";
    }
    return `
    <details class="steps" ${open ? "open" : ""}>
        <summary>${steps.length} ${
        steps.length === 1 ? "step" : "steps"
    }</summary>
        <ol>
            ${steps
                .map(
                    (step) =>
                        `<li>${renderTransformation(step, depth - 1)}</li>`
                )
                .join("")}
        </ol>
    </details>`;
};

const renderWarning = (content) => `<div class="warning">${content}</div>`;

const getExplanationString = (expl) => {
    let explanationString = translationData[expl.key];
    let warnings = [];
    if (!explanationString) {
        warnings.push(`Missing default translation for ${expl.key}`);
        explanationString = `${expl.key}(${[...expl.params.keys()]
            .map((i) => `%${i + 1}`)
            .join()})`;
    }

    for (let [i, param] of expl.params.entries()) {
        // replacing "%1", "%2", ... with the respective rendered expression
        if (explanationString.includes("%" + (i + 1))) {
            explanationString = explanationString.replace(
                "%" + (i + 1),
                renderExpression(param.expression)
            );
        } else {
            warnings.push(
                `Missing %${
                    i + 1
                } in default translation, should contain ${renderExpression(
                    param.expression
                )}`
            );
        }
    }
    let unusedPlaceholders = explanationString.match(/%[1-9]/g);
    if (unusedPlaceholders) {
        for (let placeholder of unusedPlaceholders) {
            warnings.push(`Missing parameter for placeholder ${placeholder}`);
        }
    }
    return { explanationString, warnings };
};

const renderExplanation = (expl) => {
    if (!expl) {
        return "";
    }

    const { explanationString, warnings } = getExplanationString(expl);

    return `
    <div class="plan-explanation">
        ${
            explanationString
                ? `<div title="${expl.key}">${explanationString}</div>`
                : ""
        }
        ${warnings ? warnings.map(renderWarning).join("") : ""}
    </div>`;
};

const renderExpression = (expr) => `\\(\\displaystyle${expr}\\)`;

/******************************************
 * Rendering a plan test source code.
 ******************************************/

class StringBuilder {
    constructor() {
        this.lines = [];
    }

    addLine(line) {
        this.lines.push(line);
    }

    toString() {
        return this.lines.join("\n");
    }
}

class IndentBuilder {
    constructor(parent, indent = "") {
        this.parent = parent;
        this.indent = indent;
    }

    child(indent = "    ") {
        return new IndentBuilder(this, indent);
    }

    addLine(line) {
        this.parent.addLine(this.indent + line);
        return this;
    }

    do(writeLines) {
        writeLines(this);
    }

    nest(line, writeLines, open = " {", close = "}") {
        this.addLine(line + open);
        writeLines(this.child());
        this.addLine(close);
    }
}

const renderTest = (trans) => {
    const stringBuilder = new StringBuilder();
    new IndentBuilder(stringBuilder).do(buildTest(trans));
    return `
    <details>
        <summary>Test Code</summary>
        <pre>${stringBuilder}</pre>
    </details>
    `;
};

const buildTest = (trans) => (builder) => {
    builder.nest("testPlan", (builder) => {
        builder
            .addLine(`plan = FILL_ME_IN`)
            .addLine(`inputExpr = "${trans.fromExpr}"`)
            .addLine("")
            .nest("check", buildTestBody(trans));
    });
};

const buildTestBody = (trans) => (builder) => {
    const throughStep = isThroughStep(trans);
    if (throughStep && !showThroughSteps) {
        builder.do(buildTestBody(trans.steps[0]));
        return;
    }
    if (throughStep) {
        builder.addLine("// Through step");
    } else {
        builder
            .addLine(`fromExpr = "${trans.fromExpr}"`)
            .addLine(`toExpr = "${trans.toExpr}"`);
        if (trans.explanation) {
            builder.nest(`explanation`, (builder) => {
                // By convention, the name of the explanation enum in the code is
                // [category]Explanation.[name] given that the key is [category].[name]
                const key = trans.explanation.key.replace(".", "Explanation.");
                builder.addLine(`key = ${key}`);
            });
        }
    }
    if (trans.steps) {
        for (let step of trans.steps) {
            builder.addLine("").nest("step", buildTestBody(step));
        }
    }
};

/******************************************
 * Do initial setup and register event handlers
 ******************************************/

window.onload = () => {
    fetchDefaultTranslations().then((translations) => {
        translationData = translations;
    });

    fetchPlans().then((plans) => {
        initPlans(plans);
        const url = new URL(window.location);
        const planId = url.searchParams.get("plan");
        const input = url.searchParams.get("input");
        const curriculum = url.searchParams.get("curriculum");
        if (planId) {
            el("plansSelect").value = planId;
        }
        if (input) {
            el("input").value = input;
        }
        if (curriculum) {
            el("curriculumSelect").value = curriculum;
        }
        if (planId && input) {
            selectPlansOrApplyPlan(planId, input, curriculum);
        }
    });

    el("form").onsubmit = (evt) => {
        evt.preventDefault();
        const planId = el("plansSelect").value;
        const input = el("input").value;
        const curriculum = el("curriculumSelect").value;
        const url = new URL(window.location);
        url.searchParams.set("plan", planId);
        url.searchParams.set("input", input);
        const urlString = url.toString();
        history.replaceState({ url: urlString }, null, urlString);
        selectPlansOrApplyPlan(planId, input, curriculum);
    };

    el("showThroughSteps").onchange = (evt) => {
        showThroughSteps = evt.target.checked;
        const planId = el("plansSelect").value;
        const input = el("input").value;
        const curriculum = el("curriculumSelect").value;
        if (input !== "") {
            selectPlansOrApplyPlan(planId, input, curriculum);
        }
    };
};