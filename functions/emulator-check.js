// End-to-end check of the rating totals and bay status against the Firestore emulator: writes ratings,
// hands each change to the real onRatingWritten handler, and checks the totals.
//   firebase emulators:exec --only firestore --project motobays-rules-test "node functions/emulator-check.js"
const assert = require('node:assert/strict');
const fns = require('./index');
const { getFirestore } = require('firebase-admin/firestore');

const db = getFirestore('motobays');
const ratings = db.collection('ratings');
const snap = (data) => ({ data: () => data });

/** Writes (or deletes, when stars is null) a rating and runs the trigger as Firestore would. */
async function rate(uid, bayId, stars) {
  const ref = ratings.doc(`${uid}_${bayId.replace(/\//g, '_')}`);
  const before = (await ref.get()).data();
  const after = stars == null ? undefined : { bayId, stars, authorUid: uid, updatedAt: new Date() };
  if (after) await ref.set(after); else await ref.delete();
  await fns.onRatingWritten.run({ data: { before: snap(before), after: snap(after) }, params: { id: ref.id } });
}

async function summary(id) {
  const d = await db.collection('ratingSummaries').doc(id).get();
  return d.exists ? { count: d.get('count'), total: d.get('total') } : null;
}

(async () => {
  await rate('a', 'node/1', 5);
  await rate('b', 'node/1', 4);
  assert.deepEqual(await summary('node_1'), { count: 2, total: 9 });
  await rate('a', 'node/1', 1); // a rider changes their rating
  assert.deepEqual(await summary('node_1'), { count: 2, total: 5 });
  await rate('b', 'node/1', null); // a rider takes theirs back
  assert.deepEqual(await summary('node_1'), { count: 1, total: 1 });
  await rate('a', 'council/camden/x-1', 3); // ids with several slashes
  assert.deepEqual(await summary('council_camden_x-1'), { count: 1, total: 3 });
  await rate('a', 'node/1', null); // the last rating goes, and so does the total
  assert.equal(await summary('node_1'), null);
  // Space reports and safety details: the public status keeps only the latest report and totals.
  const report = async (col, uid, bayId, data) => {
    const ref = db.collection(col).doc(`${uid}_${bayId.replace(/\//g, '_')}`);
    const before = (await ref.get()).data();
    const after = data == null ? undefined : { bayId, authorUid: uid, ...data };
    if (after) await ref.set(after); else await ref.delete();
    const handler = col === 'spaceReports' ? fns.onSpaceReportWritten : fns.onBayFeaturesWritten;
    await handler.run({ data: { before: snap(before), after: snap(after) }, params: { id: ref.id } });
  };
  const status = async (id) => {
    const d = await db.collection('bayStatus').doc(id).get();
    return d.exists ? { full: d.get('space')?.full, features: d.get('features') } : null;
  };
  await report('spaceReports', 'a', 'node/9', { full: true, at: new Date(Date.now() - 60_000) });
  await report('spaceReports', 'b', 'node/9', { full: false, at: new Date() });
  await report('bayFeatures', 'a', 'node/9', { anchor: true, cctv: false, updatedAt: new Date() });
  await report('bayFeatures', 'b', 'node/9', { anchor: true, updatedAt: new Date() });
  assert.deepEqual(await status('node_9'), { full: false, features: { anchor: { yes: 2, no: 0 }, cctv: { yes: 0, no: 1 } } });
  assert.equal((await db.collection('bayStatus').doc('node_9').get()).get('space').authorUid, undefined);
  await report('spaceReports', 'b', 'node/9', null); // a rider deletes their account
  assert.equal((await status('node_9')).full, true);
  for (const uid of ['a', 'b']) await report('bayFeatures', uid, 'node/9', null);
  await report('spaceReports', 'a', 'node/9', null);
  assert.equal(await status('node_9'), null);
  console.log('emulator check passed');
  process.exit(0);
})().catch((e) => { console.error(e); process.exit(1); });
