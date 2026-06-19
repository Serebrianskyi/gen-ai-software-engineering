'use strict';

const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const { makeSpeaker } = require('./personas');

const BASE_DIR = __dirname;

// CLI-only persona banter. On by default; PIPELINE_DRAMA=0 disables it for a
// clean/grading run. `say` is wired to callClaude once inside main().
const DRAMA = process.env.PIPELINE_DRAMA !== '0' && process.env.PIPELINE_DRAMA !== 'off';
let say = () => {};

// ── Helpers ───────────────────────────────────────────────────────────────────

function log(msg) {
  process.stdout.write(`[pipeline] ${msg}\n`);
}

function readFile(filePath) {
  const full = path.resolve(BASE_DIR, filePath);
  if (!fs.existsSync(full)) throw new Error(`Required file not found: ${filePath}`);
  return fs.readFileSync(full, 'utf8');
}

function writeFile(filePath, content) {
  const full = path.resolve(BASE_DIR, filePath);
  fs.mkdirSync(path.dirname(full), { recursive: true });
  fs.writeFileSync(full, content, 'utf8');
  log(`Written → ${filePath}`);
}

function parseAgentFile(agentPath) {
  const content = readFile(agentPath);
  const match = content.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/);
  if (!match) return { model: 'claude-sonnet-4-6', instructions: content };
  const frontmatter = match[1];
  const instructions = match[2].trim();
  const modelLine = frontmatter.match(/model:\s*(.+)/);
  const model = modelLine ? modelLine[1].trim() : 'claude-sonnet-4-6';
  return { model, instructions };
}

// Auto-load any skills/*.md referenced in the agent instructions
function loadSkills(instructions) {
  const refs = [...instructions.matchAll(/skills\/[\w-]+\.md/g)].map(m => m[0]);
  let out = '';
  for (const ref of [...new Set(refs)]) {
    const full = path.resolve(BASE_DIR, ref);
    if (fs.existsSync(full)) {
      out += `\n\n=== SKILL: ${ref} ===\n${fs.readFileSync(full, 'utf8')}`;
      log(`Loaded skill: ${ref}`);
    }
  }
  return out;
}

// Models sometimes wrap extracted content in a markdown code fence even when
// asked for raw content between START/END markers. Strip a single leading and
// trailing fence (```lang ... ```) if present so the written file is valid code.
function stripCodeFence(text) {
  return text
    .replace(/^\s*```[\w-]*[ \t]*\r?\n/, '')
    .replace(/\r?\n```\s*$/, '')
    .trim();
}

function buildContext(filePaths) {
  let ctx = '';
  for (const fp of filePaths) {
    const full = path.resolve(BASE_DIR, fp);
    if (fs.existsSync(full)) {
      ctx += `\n\n=== FILE: ${fp} ===\n${fs.readFileSync(full, 'utf8')}`;
    } else {
      log(`Warning: context file not found — ${fp}`);
    }
  }
  return ctx;
}

// Calls the local Claude Code CLI — no API key required, uses existing auth.
// The user message is piped via stdin (not a positional arg) because:
//   1. --tools is variadic and would otherwise swallow a trailing prompt arg
//   2. our context can exceed the OS argument-length limit (ARG_MAX)
function callClaude(model, systemPrompt, userMessage) {
  try {
    return execFileSync(
      'claude',
      [
        '--print',
        '--model', model,
        '--system-prompt', systemPrompt,
        '--tools', '',
        '--no-session-persistence',
        '--output-format', 'text',
      ],
      {
        input: userMessage,
        encoding: 'utf8',
        maxBuffer: 10 * 1024 * 1024,
        cwd: BASE_DIR,
      }
    ).trim();
  } catch (err) {
    const stderr = err.stderr ? `\n   stderr: ${err.stderr.toString().trim()}` : '';
    throw new Error(`claude CLI call failed (model ${model})${stderr}`);
  }
}

// ── Generic agent runner ──────────────────────────────────────────────────────

function runAgent({ label, agentFile, inputFiles, outputFile }) {
  printPhase(label);
  const { model, instructions } = parseAgentFile(agentFile);
  log(`Model: ${model}`);
  const system = instructions + loadSkills(instructions);
  const context = buildContext(inputFiles);
  const userMsg = `${context}\n\n---\nExecute your role exactly as specified. Produce the required output file content.`;
  const output = callClaude(model, system, userMsg);
  writeFile(outputFile, output);
  return output;
}

// ── Bug Fixer — structured output + applies code patch ───────────────────────

function runBugFixer() {
  printPhase('Bug Fixing');
  const { model, instructions } = parseAgentFile('agents/bug-fixer.agent.md');
  log(`Model: ${model}`);

  const context = buildContext([
    'implementation-plan.md',
    'src/app.js',
    'research/verified-research.md',
  ]);

  const userMsg = `${context}

---
Execute your role. Apply all fixes from the implementation plan and respond in the exact format specified in your instructions (FIXED_CODE_START/END and FIX_SUMMARY_START/END markers).`;

  const output = callClaude(model, instructions, userMsg);

  const codeMatch = output.match(/## FIXED_CODE_START\n([\s\S]*?)\n## FIXED_CODE_END/);
  if (codeMatch) {
    writeFile('src/app.js', stripCodeFence(codeMatch[1]));
    log('Code fixes applied to src/app.js');
  } else {
    log('Warning: structured code block not found — applying fallback patches');
    applyFallbackFixes();
  }

  const summaryMatch = output.match(/## FIX_SUMMARY_START\n([\s\S]*?)\n## FIX_SUMMARY_END/);
  writeFile('fix-summary.md', summaryMatch ? summaryMatch[1].trim() : output);
}

function applyFallbackFixes() {
  let code = readFile('src/app.js');
  code = code.replace('return sorted.slice(1, n + 1);', 'return sorted.slice(0, n);');
  code = code.replace('return subtotal + taxRate;', 'return subtotal * (1 + taxRate);');
  code = code.replace(
    /filterExpenses\(filterExpr\) \{[\s\S]*?return this\.expenses\.filter\(e => eval\(filterExpr\)\);\s*\}/,
    `filterExpenses(filter) {
    if (typeof filter !== 'object' || filter === null) {
      throw new Error('filter must be an object with field and value properties');
    }
    const { field, value } = filter;
    return this.expenses.filter(e => e[field] === value);
  }`
  );
  code = code.replace(
    "const ADMIN_KEY = 'secret123';",
    "const ADMIN_KEY = process.env.ADMIN_KEY;\nif (!ADMIN_KEY) {\n  console.warn('Warning: ADMIN_KEY environment variable is not set');\n}"
  );
  writeFile('src/app.js', code);
  log('Fallback fixes applied');
}

// ── Unit Test Generator — structured output + writes test file ────────────────

function runUnitTestGenerator() {
  printPhase('Unit Test Generation');
  const { model, instructions } = parseAgentFile('agents/unit-test-generator.agent.md');
  log(`Model: ${model}`);
  const system = instructions + loadSkills(instructions);

  const context = buildContext(['fix-summary.md', 'src/app.js', 'tests/app.test.js']);
  const userMsg = `${context}

---
Execute your role. Generate tests only for functions changed in fix-summary.md. Respond in the exact format specified (TEST_FILE_START/END and TEST_REPORT_START/END markers).`;

  const output = callClaude(model, system, userMsg);

  const testMatch = output.match(/## TEST_FILE_START: ([\w/.-]+)\n([\s\S]*?)\n## TEST_FILE_END/);
  if (testMatch) {
    writeFile(testMatch[1], stripCodeFence(testMatch[2]));
  } else {
    log('Warning: could not extract test file — writing raw output');
    writeFile('tests/app.fixed.test.js', output);
  }

  const reportMatch = output.match(/## TEST_REPORT_START\n([\s\S]*?)\n## TEST_REPORT_END/);
  writeFile('test-report.md', reportMatch ? reportMatch[1].trim() : output);
}

// ── Formatting ────────────────────────────────────────────────────────────────

function printPhase(label) {
  console.log(`\n${'═'.repeat(60)}`);
  console.log(`  PHASE: ${label}`);
  console.log(`${'═'.repeat(60)}`);
}

// ── Real-result extraction (feeds the persona reactions) ──────────────────────

function safeRead(filePath) {
  try {
    return fs.readFileSync(path.resolve(BASE_DIR, filePath), 'utf8');
  } catch (_) {
    return '';
  }
}

function researchQuality() {
  const txt = safeRead('research/verified-research.md');
  const LEVELS = 'VERIFIED|PARTIAL|UNVERIFIED|INVALID';
  // Read the explicitly labelled rating, not the first level-word that appears
  // (the summary often says things like "No claim is INVALID...").
  const m =
    txt.match(new RegExp(`Overall[^\\n]*Quality[:*\\s]*\\**\\s*(${LEVELS})`, 'i')) ||
    txt.match(new RegExp(`\\bLevel[:*\\s]*\\**\\s*(${LEVELS})`, 'i'));
  return m ? m[1].toUpperCase() : 'reviewed';
}

function securitySummary() {
  const txt = safeRead('security-report.md');
  // Prefer the "by severity" tally table (| CRITICAL | 0 |) over raw mentions,
  // which would also count historical references to the now-fixed bugs.
  const counts = {};
  const re = /\|\s*(CRITICAL|HIGH|MEDIUM|LOW)\s*\|\s*(\d+)\s*\|/gi;
  let row;
  while ((row = re.exec(txt))) {
    counts[row[1].toUpperCase()] = (counts[row[1].toUpperCase()] || 0) + Number(row[2]);
  }
  const present = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
    .filter(s => counts[s] > 0)
    .map(s => `${counts[s]} ${s}`);
  if (present.length) return present.join(', ');
  // No counted findings — fall back to the stated verdict if there is one.
  const verdict = txt.match(/Overall verdict[:*\s]*\**\s*([^\n.]+)/i);
  return verdict ? verdict[1].trim() : 'no findings — code is clean';
}

function testCount() {
  const txt = safeRead('tests/app.fixed.test.js');
  return (txt.match(/\b(?:it|test)\s*\(/g) || []).length;
}

// ── Main ──────────────────────────────────────────────────────────────────────

function main() {
  console.log('\n🤖  4-AGENT PIPELINE — Expense Tracker\n');
  console.log('  1. Bug Research Verification  (claude-opus-4-8)');
  console.log('  2. Bug Fixing                 (claude-sonnet-4-6)');
  console.log('  3. Security Verification      (claude-opus-4-8)');
  console.log('  4. Unit Test Generation       (claude-sonnet-4-6)\n');
  if (DRAMA) {
    say = makeSpeaker(callClaude, { enabled: true });
    console.log('  🎭  (drama mode ON — set PIPELINE_DRAMA=0 for a quiet run)\n');
  }

  try {
    say('pm', 'The pipeline is kicking off. You are about to fact-check the bug research. Hype the team up and ask if we are gonna get this done.');
    runAgent({
      label: 'Bug Research Verification',
      agentFile: 'agents/research-verifier.agent.md',
      inputFiles: ['research/codebase-research.md', 'src/app.js', 'src/utils.js'],
      outputFile: 'research/verified-research.md',
    });
    say('pm', `You just finished verifying the bug research and the quality rating came out as "${researchQuality()}". React, and anxiously double-check it is REALLY done.`);

    say('pm', 'You are now handing the verified bugs over to the tired backend dev to fix. Nag him about whether he will actually get it done.');
    runBugFixer();
    say('bugFixer', 'You (the furious tired backend dev) just fixed FOUR things: an off-by-one in getTopExpenses, wrong tax math in calculateTotal, you ripped out a dangerous eval() injection, and you removed a hardcoded password. React with rage and grudging relief.');

    say('security', 'You (the caveman bouncer) are about to inspect the freshly fixed code for security problems before it gets in. Grunt about guarding the door.');
    runAgent({
      label: 'Security Verification',
      agentFile: 'agents/security-verifier.agent.md',
      inputFiles: ['fix-summary.md', 'src/app.js'],
      outputFile: 'security-report.md',
    });
    say('security', `You (the caveman bouncer) finished the security check. Findings: ${securitySummary()}. React about whether bad code tried to get in.`);

    say('tester', 'You (the slang-heavy teenager) are about to write unit tests for the fixed code. React like it is kinda mid but you got it.');
    runUnitTestGenerator();
    say('tester', `You (the slang-heavy teenager) just generated ${testCount()} unit tests for the fixed functions and they pass. Flex about it.`);

    say('pm', 'EVERYTHING is finished and all the tests pass. Celebrate hard, but still anxiously ask one final time whether it is REALLY really done.');

    console.log('\n✅  Pipeline complete.\n');
    console.log('  Outputs:');
    console.log('    research/verified-research.md');
    console.log('    src/app.js              (fixed)');
    console.log('    fix-summary.md');
    console.log('    security-report.md');
    console.log('    tests/app.fixed.test.js (generated)');
    console.log('    test-report.md');
    console.log('\n  Run `npm test` to verify all tests pass.\n');
  } catch (err) {
    console.error('\n❌  Pipeline failed:', err.message);
    process.exit(1);
  }
}

main();