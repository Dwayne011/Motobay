// Keeps ratingSummaries/{bay} (count and star total, public) in step with ratings/{rider_bay}
// (private to each rider). The app shows each bay's average from the summary, so nobody can
// see which bays a particular rider has rated.
const { onDocumentWritten } = require('firebase-functions/v2/firestore');
const { onRequest } = require('firebase-functions/v2/https');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { summarise, summaryId, validBayId } = require('./summary');
const { summariseStatus } = require('./status');

initializeApp(process.env.FIRESTORE_EMULATOR_HOST ? { projectId: process.env.GCLOUD_PROJECT || "motobays-rules-test" } : undefined);
const db = getFirestore('motobays');

// eur3 (the database's multi-region) is served by Cloud Functions in Europe.
const REGION = 'europe-west1';

/** Recounts one bay inside a transaction, so two ratings landing together can't leave a stale total. */
async function recount(bayId) {
  const ref = db.collection('ratingSummaries').doc(summaryId(bayId));
  await db.runTransaction(async (t) => {
    const snap = await t.get(db.collection('ratings').where('bayId', '==', bayId));
    const summary = summarise(bayId, snap.docs.map((d) => d.get('stars')));
    if (summary) t.set(ref, { ...summary, updatedAt: FieldValue.serverTimestamp() });
    else t.delete(ref);
  });
}

exports.onRatingWritten = onDocumentWritten(
  { document: 'ratings/{id}', database: 'motobays', region: REGION },
  async (event) => {
    const before = validBayId(event.data?.before?.data()?.bayId);
    const after = validBayId(event.data?.after?.data()?.bayId);
    for (const bayId of new Set([before, after].filter(Boolean))) await recount(bayId);
  },
);

/** Rebuilds bayStatus/{bay} from the bay's space reports and safety answers. */
async function recountStatus(bayId) {
  const ref = db.collection('bayStatus').doc(summaryId(bayId));
  await db.runTransaction(async (t) => {
    const spaces = await t.get(db.collection('spaceReports').where('bayId', '==', bayId));
    const features = await t.get(db.collection('bayFeatures').where('bayId', '==', bayId));
    const status = summariseStatus(
      bayId,
      spaces.docs.map((d) => ({ full: d.get('full'), suspended: d.get('suspended'), at: d.get('at')?.toDate?.() })),
      features.docs.map((d) => d.data()),
    );
    if (status) t.set(ref, { ...status, updatedAt: FieldValue.serverTimestamp() });
    else t.delete(ref);
  });
}

async function onReportWritten(event) {
  const before = validBayId(event.data?.before?.data()?.bayId);
  const after = validBayId(event.data?.after?.data()?.bayId);
  for (const bayId of new Set([before, after].filter(Boolean))) await recountStatus(bayId);
}

exports.onSpaceReportWritten = onDocumentWritten(
  { document: 'spaceReports/{id}', database: 'motobays', region: REGION },
  onReportWritten,
);

exports.onBayFeaturesWritten = onDocumentWritten(
  { document: 'bayFeatures/{id}', database: 'motobays', region: REGION },
  onReportWritten,
);

// One-off: builds the totals for ratings made before this function existed. Runs once;
// after that a marker document makes it do nothing, so calling the URL again is harmless.
exports.backfillRatingSummaries = onRequest({ region: REGION }, async (req, res) => {
  const marker = db.collection('meta').doc('ratingSummaries');
  if ((await marker.get()).exists) {
    res.status(200).send('Already done.');
    return;
  }
  const byBay = new Map();
  (await db.collection('ratings').get()).forEach((d) => {
    const bayId = validBayId(d.get('bayId'));
    if (bayId) byBay.set(bayId, [...(byBay.get(bayId) ?? []), d.get('stars')]);
  });
  for (const bayId of byBay.keys()) await recount(bayId);
  await marker.set({ backfilledAt: FieldValue.serverTimestamp(), bays: byBay.size });
  res.status(200).send(`Built totals for ${byBay.size} bays.`);
});

// Fuel prices: every 30 minutes from Fuel Finder, run in London because the service only
// answers UK requests. Writes fuel/prices.json.gz (gzipped JSON) to the default Storage bucket.
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { defineSecret } = require('firebase-functions/params');
const { getStorage } = require('firebase-admin/storage');
const { gzipSync } = require('node:zlib');
const { fetchFuel } = require('./fuel');

const FUEL_ID = defineSecret('FUEL_FINDER_CLIENT_ID');
const FUEL_SECRET = defineSecret('FUEL_FINDER_CLIENT_SECRET');

exports.refreshFuelPrices = onSchedule(
  { schedule: 'every 30 minutes', region: 'europe-west2', timeZone: 'Europe/London', secrets: [FUEL_ID, FUEL_SECRET], timeoutSeconds: 300, memory: '512MiB', retryCount: 0 },
  async () => {
    const file = await fetchFuel(FUEL_ID.value(), FUEL_SECRET.value(), (m) => console.log(m));
    if (file.stations.length < 1000) throw new Error(`Only ${file.stations.length} stations; not publishing.`);
    // Plain gzip (no Content-Encoding), so every client gets the same bytes and unzips them itself.
    await getStorage().bucket().file('fuel/prices.json.gz').save(gzipSync(JSON.stringify(file)), {
      contentType: 'application/gzip',
      metadata: { cacheControl: 'public, max-age=900' },
    });
    console.log(`Published ${file.stations.length} stations`);
  },
);
