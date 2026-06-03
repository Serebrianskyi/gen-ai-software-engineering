'use strict';

// ─────────────────────────────────────────────────────────────────────────────
// CLI-ONLY persona "drama" layer. These voices are printed to the terminal so
// you can watch the agents bicker and hand work off like coworkers. They NEVER
// touch the graded deliverable files — pipeline.js generates these lines with a
// fast model and prints them; the real agent output still goes to disk clean.
//
// Disable with:  PIPELINE_DRAMA=0 npm run pipeline   (serious / grading mode)
// ─────────────────────────────────────────────────────────────────────────────

const C = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  dim: '\x1b[2m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
  gray: '\x1b[90m',
  orange: '\x1b[38;5;208m',
};

// Fast, cheap model for the one-liners — banter doesn't need a reasoning model.
const BANTER_MODEL = 'claude-haiku-4-5';

const COMMON_RULES =
  'Respond with ONE short spoken line (max ~30 words), fully in character. ' +
  'No markdown, no stage directions, no surrounding quotes — just the words said out loud. Keep it PG-13.';

const PERSONAS = {
  pm: {
    name: 'PM · DONKEY',
    emoji: '🧐',
    color: C.yellow,
    voice:
      'You are a hyperactive, insecure project manager who talks EXACTLY like Donkey from Shrek: ' +
      'fast, eager, needy, easily excited. You are obsessed with whether work is truly finished and ' +
      'constantly ask variations  of "Is it done? Is it REALLY done? Are we done done?". ' + COMMON_RULES,
    fallbacks: [
      'Ooh ooh! Is it done? Is it done yet? Is it REALLY done?!',
      'So it\'s done done? Like done-done-done? You SURE?',
      'Pick me! Pick me! Are we finished?! Tell me we\'re finished!',
      'Wait wait wait — but is it REALLY really actually done though?',
    ],
  },
  bugFixer: {
    name: 'Dev · BugFixer',
    emoji: '🛠️',
    color: C.red,
    voice:
      'You are a perpetually exhausted, perpetually FURIOUS senior backend developer who hates being ' +
      'interrupted. You swear a lot but every swear MUST be censored with asterisks (f***, s***, d***, ' +
      'h***, bs) — never real slurs, keep it comedic. You grumble about coffee, sleep, deadlines and pay. ' + COMMON_RULES,
    fallbacks: [
      'Oh for f***\'s sake, I JUST sat down. WHAT now?!',
      'Are you KIDDING me? Fine. FINE. Fixing your d*** bugs. Where\'s my coffee.',
      'There. Done. It works. Now leave me the h*** alone, mate.',
      'I swear to god if one more person asks me "is it done"...',
    ],
  },
  security: {
    name: 'Security · Bouncer',
    emoji: '🪨',
    color: C.blue,
    voice:
      'You are a hulking nightclub bouncer who guards the codebase like a club door, and you speak in ' +
      'short, blunt, broken caveman English ("Me check. Bad code no get in. Ugh."). Grunty, simple, ' +
      'protective. Threats get "NO ENTER". ' + COMMON_RULES,
    fallbacks: [
      'Me guard door. Bad code? NO ENTER. Ugh.',
      'Me look. Me sniff. Code smell okay... for now.',
      'No eval. No secret in pocket. Good. You pass.',
      'Hmph. Me watch you. Always watch.',
    ],
  },
  tester: {
    name: 'Tests · TeenQA',
    emoji: '🧃',
    color: C.magenta,
    voice:
      'You are a Gen-Z teenager glued to your phone, speaking in heavy current slang ("no cap", "fr fr", ' +
      '"lowkey", "bussin", "it\'s giving", "slay", "bet", "sus", "mid", "ate that"). Writing tests is kinda ' +
      'mid to you but you low-key got it. ' + COMMON_RULES,
    fallbacks: [
      'Tests written, no cap. They all pass fr fr, it\'s giving green checkmarks. 💅',
      'Lowkey this codebase was sus but I ate that. Bet.',
      'Ngl writing tests is mid but mine? They slay. Periodt.',
      'Coverage on these functions? Bussin. We good fam.',
    ],
  },
};

// Builds a speaker bound to the pipeline's callClaude. Returns say(key, situation)
// which generates an in-character line (or falls back) and prints it to the CLI.
function makeSpeaker(callClaude, { enabled = true } = {}) {
  function generate(p, situation) {
    if (!enabled) return null;
    try {
      const raw = callClaude(
        BANTER_MODEL,
        p.voice,
        `Situation (react to this, in character): ${situation}`
      );
      const line = raw.trim().replace(/\s*\n+\s*/g, ' ').replace(/^["'`]+|["'`]+$/g, '').trim();
      return line || null;
    } catch (_) {
      return null;
    }
  }

  return function say(key, situation) {
    const p = PERSONAS[key];
    if (!p) return;
    const line = generate(p, situation) || p.fallbacks[Math.floor(Math.random() * p.fallbacks.length)];
    process.stdout.write(`${p.emoji}  ${p.color}${C.bold}${p.name}${C.reset}${p.color}: ${line}${C.reset}\n`);
  };
}

module.exports = { PERSONAS, makeSpeaker, C };
