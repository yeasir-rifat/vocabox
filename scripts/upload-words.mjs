#!/usr/bin/env node
// Uploads words.json to Cloud Firestore, one pack per category (split when a category is large).
//
//   cd scripts && npm install
//   node upload-words.mjs --dry                      # check + show the packs, uploads nothing
//   node upload-words.mjs                            # upload ../app/src/main/assets/words.json
//   node upload-words.mjs path/to/other-words.json   # upload another file
//   Add --allow-duplicates to upload even if the same word appears twice.
//
// Needs a service account key (Firebase console > Project settings > Service accounts > Generate new
// private key). Save it as scripts/serviceAccountKey.json, or set GOOGLE_APPLICATION_CREDENTIALS.
// NEVER commit that key (it is in .gitignore).

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const dry = args.includes('--dry');
const file = path.resolve(
  args.find((a) => !a.startsWith('--')) ?? path.join(here, '../app/src/main/assets/words.json')
);

const MAX_WORDS_PER_PACK = 300;
const MAX_BYTES_PER_PACK = 700_000; // Firestore allows 1 MiB per document

function fail(message) {
  console.error('ERROR: ' + message);
  process.exit(1);
}

// ---- read and check the file -------------------------------------------------------------
const data = JSON.parse(fs.readFileSync(file, 'utf8'));
if (!Number.isInteger(data.version)) fail('"version" must be a whole number at the top of the file.');
if (!Array.isArray(data.words) || data.words.length === 0) fail('"words" is empty.');

const seen = new Set();
for (const [i, w] of data.words.entries()) {
  if (!Number.isInteger(w.id)) fail(`words[${i}] has no numeric "id".`);
  if (seen.has(w.id)) fail(`Duplicate id ${w.id} (${w.word}).`);
  seen.add(w.id);
  for (const key of ['word', 'partOfSpeech', 'banglaMeaning', 'englishMeaning']) {
    if (typeof w[key] !== 'string' || !w[key].trim()) fail(`id ${w.id}: "${key}" is missing.`);
  }
}

// Same word + category + part of speech under two different ids would show up twice in the app.
const norm = (t) => String(t ?? '').trim().toLowerCase().replace(/\s+/g, ' ');
const firstSeen = new Map();
const repeats = [];
for (const w of data.words) {
  const key = `${norm(w.type || 'WORD')}|${norm(w.word)}|${norm(w.partOfSpeech)}`;
  if (firstSeen.has(key)) repeats.push(`  "${w.word}" (${w.type || 'WORD'}, ${w.partOfSpeech}): id ${firstSeen.get(key)} and id ${w.id}`);
  else firstSeen.set(key, w.id);
}
if (repeats.length) {
  console.error(`Repeated words found (${repeats.length}):\n${repeats.join('\n')}`);
  if (!args.includes('--allow-duplicates')) {
    fail('Remove the repeats (keep the old id), or run with --allow-duplicates if they are intended.');
  }
}

// ---- split into packs: one category (type) at a time -------------------------------------
const byType = new Map();
for (const w of data.words) {
  const type = w.type || 'WORD';
  if (!byType.has(type)) byType.set(type, []);
  byType.get(type).push(w);
}

const packs = []; // { id, type, words }
for (const [type, list] of byType) {
  const base = type.toLowerCase().replace(/[^a-z0-9_]/g, '_');
  let current = [];
  let bytes = 2;
  let n = 1;
  const flush = () => {
    if (current.length) packs.push({ id: `${base}_${n++}`, type, words: current });
    current = [];
    bytes = 2;
  };
  for (const w of list) {
    const size = Buffer.byteLength(JSON.stringify(w)) + 1;
    if (current.length >= MAX_WORDS_PER_PACK || bytes + size > MAX_BYTES_PER_PACK) flush();
    current.push(w);
    bytes += size;
  }
  flush();
}

console.log(`File: ${file}`);
console.log(`Version ${data.version}, ${data.words.length} words, ${packs.length} packs:`);
for (const p of packs) {
  const kb = (Buffer.byteLength(JSON.stringify(p.words)) / 1024).toFixed(1);
  console.log(`  ${p.id.padEnd(24)} ${String(p.words.length).padStart(5)} words  ${kb} KB`);
}
if (dry) {
  console.log('Dry run: nothing uploaded.');
  process.exit(0);
}

// ---- upload ------------------------------------------------------------------------------
const { initializeApp, cert, applicationDefault } = await import('firebase-admin/app');
const { getFirestore } = await import('firebase-admin/firestore');

const keyPath = process.env.GOOGLE_APPLICATION_CREDENTIALS ?? path.join(here, 'serviceAccountKey.json');
initializeApp({
  credential: fs.existsSync(keyPath)
    ? cert(JSON.parse(fs.readFileSync(keyPath, 'utf8')))
    : applicationDefault()
});
const db = getFirestore();

// 1) packs first, 2) meta last (the app only reads packs listed in meta, so it never sees half an upload)
const batch = db.batch();
for (const p of packs) {
  batch.set(db.collection('packs').doc(p.id), {
    json: JSON.stringify(p.words),
    count: p.words.length,
    type: p.type
  });
}
await batch.commit();

await db.collection('meta').doc('words').set({
  version: data.version,
  stamp: Date.now(),
  total: data.words.length,
  packs: packs.map((p) => p.id)
});

// 3) remove packs that are no longer part of the list
const keep = new Set(packs.map((p) => p.id));
const stale = (await db.collection('packs').listDocuments()).filter((d) => !keep.has(d.id));
await Promise.all(stale.map((d) => d.delete()));

console.log(`Uploaded ${data.words.length} words in ${packs.length} packs. Removed ${stale.length} old packs.`);
