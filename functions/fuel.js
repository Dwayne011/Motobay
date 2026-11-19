// Fuel prices from the UK government's Fuel Finder scheme (Motor Fuel Price (Open Data)
// Regulations 2025, Open Government Licence v3.0). Fuel Finder only answers requests from
// the UK, so this runs in Google's London region. It fetches every forecourt and its
// current prices, and publishes one compact file to Cloud Storage (fuel/prices.json.gz) that
// the app downloads. The client id and secret are Secret Manager secrets, never in code.

const API = 'https://www.fuel-finder.service.gov.uk/api/v1';
const USER_AGENT = 'MotoBays (https://github.com/Dwayne011/Motobay)';
// The grades a motorcycle uses, plus diesel for completeness of the forecourt's sign.
const GRADES = { E10: 'e10', E5: 'e5', B7_STANDARD: 'b7' };

async function call(path, { method = 'GET', token, body } = {}) {
  const res = await fetch(API + path, {
    method,
    headers: {
      Accept: 'application/json',
      'User-Agent': USER_AGENT,
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${method} ${path}: HTTP ${res.status} ${text.slice(0, 200)}`);
  return text ? JSON.parse(text) : null;
}

/** The access token, wherever the reply puts it. */
function tokenFrom(reply) {
  const t = reply?.access_token ?? reply?.data?.access_token;
  if (!t) throw new Error(`No access token in reply: ${Object.keys(reply || {}).join(',')}`);
  return t;
}

/** Every batch of a paged endpoint (up to 500 forecourts each) until one comes back empty or short. */
async function allBatches(path, token, log) {
  const out = [];
  for (let batch = 1; batch <= 200; batch++) {
    const reply = await call(`${path}?batch-number=${batch}`, { token });
    const rows = Array.isArray(reply) ? reply : (reply?.data ?? []);
    if (!Array.isArray(rows)) throw new Error(`Unexpected reply shape from ${path}`);
    out.push(...rows);
    log?.(`${path} batch ${batch}: ${rows.length}`);
    if (rows.length < 500) break;
  }
  return out;
}

const num = (v) => {
  const n = typeof v === 'number' ? v : parseFloat(v);
  return Number.isFinite(n) ? n : null;
};

/**
 * Joins forecourts and prices into the published file. Each station is
 * [id, name, brand, lat, lon, postcode, e10, e5, b7, updated, motorway]; prices are pence
 * per litre (null when not sold), updated is the newest price time (ISO, or null).
 * Closed forecourts and ones without a position are left out.
 */
function buildFile(stations, prices, now = new Date()) {
  const priceById = new Map();
  for (const p of prices) {
    const grades = {};
    let updated = null;
    for (const fp of p.fuel_prices ?? []) {
      const key = GRADES[fp.fuel_type];
      const price = num(fp.price);
      if (!key || price == null || price <= 0) continue;
      grades[key] = Math.round(price * 10) / 10;
      if (fp.price_last_updated && (!updated || fp.price_last_updated > updated)) updated = fp.price_last_updated;
    }
    priceById.set(p.node_id, { grades, updated });
  }
  const rows = [];
  for (const s of stations) {
    if (s.permanent_closure || s.temporary_closure) continue;
    const lat = num(s.location?.latitude);
    const lon = num(s.location?.longitude);
    if (lat == null || lon == null || lat < 49 || lat > 61.5 || lon < -9 || lon > 2.5) continue;
    const p = priceById.get(s.node_id);
    if (!p || Object.keys(p.grades).length === 0) continue;
    rows.push([
      s.node_id,
      s.trading_name || s.brand_name || 'Petrol station',
      s.brand_name || null,
      Math.round(lat * 1e6) / 1e6,
      Math.round(lon * 1e6) / 1e6,
      s.location?.postcode || null,
      p.grades.e10 ?? null,
      p.grades.e5 ?? null,
      p.grades.b7 ?? null,
      p.updated,
      s.is_motorway_service_station === true,
    ]);
  }
  rows.sort((a, b) => (a[0] < b[0] ? -1 : 1));
  return {
    generated: now.toISOString(),
    attribution: 'Contains public sector information licensed under the Open Government Licence v3.0 (Fuel Finder).',
    fields: ['id', 'name', 'brand', 'lat', 'lon', 'postcode', 'e10', 'e5', 'b7', 'updated', 'motorway'],
    stations: rows,
  };
}

async function fetchFuel(clientId, clientSecret, log) {
  const token = tokenFrom(await call('/oauth/generate_access_token', {
    method: 'POST', body: { client_id: clientId, client_secret: clientSecret },
  }));
  const stations = await allBatches('/pfs', token, log);
  const prices = await allBatches('/pfs/fuel-prices', token, log);
  return buildFile(stations, prices);
}

module.exports = { buildFile, fetchFuel, tokenFrom };
