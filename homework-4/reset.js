'use strict';

// Restores the "before pipeline" state so the pipeline can be re-run cleanly:
//   1. Copies the pristine buggy source (fixtures/app.buggy.js) → src/app.js
//   2. Removes all pipeline-generated output files
// Seeded inputs (research/codebase-research.md, implementation-plan.md,
// src/utils.js, tests/app.test.js, agents/, skills/, context/) are left intact.

const fs = require('fs');
const path = require('path');

const BASE_DIR = __dirname;

// Files written/overwritten by a pipeline run. src/app.js is restored from the
// fixture; the rest are deleted (regenerated on the next `npm run pipeline`).
const GENERATED_OUTPUTS = [
  'research/verified-research.md',
  'fix-summary.md',
  'security-report.md',
  'test-report.md',
  'tests/app.fixed.test.js',
];

function abs(p) {
  return path.resolve(BASE_DIR, p);
}

function log(msg) {
  process.stdout.write(`[reset] ${msg}\n`);
}

function main() {
  // 1. Restore buggy source
  const buggy = abs('fixtures/app.buggy.js');
  if (!fs.existsSync(buggy)) {
    console.error('[reset] Missing fixtures/app.buggy.js — cannot restore buggy source.');
    process.exit(1);
  }
  fs.copyFileSync(buggy, abs('src/app.js'));
  log('Restored src/app.js → buggy baseline (fixtures/app.buggy.js)');

  // 2. Remove generated outputs
  for (const out of GENERATED_OUTPUTS) {
    const full = abs(out);
    if (fs.existsSync(full)) {
      fs.rmSync(full);
      log(`Removed ${out}`);
    }
  }

  log('Done. Run `npm test` to confirm 2 failures (BUG-001, BUG-002), then `npm run pipeline`.');
}

main();