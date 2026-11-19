const test = require('node:test');
const assert = require('node:assert/strict');
const { buildFile, tokenFrom } = require('./fuel');

const station = (id, over = {}) => ({
  node_id: id, trading_name: `Station ${id}`, brand_name: 'BRAND',
  location: { latitude: '51.5', longitude: '-0.1', postcode: 'SW1A 1AA' }, ...over,
});

test('prices are joined to stations in pence, with the newest update time', () => {
  const f = buildFile([station('a')], [{ node_id: 'a', fuel_prices: [
    { fuel_type: 'E10', price: '0139.9000', price_last_updated: '2026-11-18T08:00:00' },
    { fuel_type: 'E5', price: '0151.9000', price_last_updated: '2026-11-18T09:00:00' },
    { fuel_type: 'B10', price: '0140.0000' },
  ] }], new Date('2026-11-19T00:00:00Z'));
  assert.deepEqual(f.stations, [['a', 'Station a', 'BRAND', 51.5, -0.1, 'SW1A 1AA', 139.9, 151.9, null, '2026-11-18T09:00:00', false]]);
  assert.match(f.attribution, /Open Government Licence/);
});

test('closed, unplaced and unpriced forecourts are left out', () => {
  const f = buildFile(
    [station('closed', { permanent_closure: true }), station('nowhere', { location: {} }), station('noprice'), station('ok')],
    [{ node_id: 'closed', fuel_prices: [{ fuel_type: 'E10', price: '140' }] },
     { node_id: 'nowhere', fuel_prices: [{ fuel_type: 'E10', price: '140' }] },
     { node_id: 'noprice', fuel_prices: [{ fuel_type: 'E10', price: null }] },
     { node_id: 'ok', fuel_prices: [{ fuel_type: 'E10', price: '141.0' }] }],
  );
  assert.deepEqual(f.stations.map((s) => s[0]), ['ok']);
});

test('the token is found at the top level or under data', () => {
  assert.equal(tokenFrom({ access_token: 'x' }), 'x');
  assert.equal(tokenFrom({ data: { access_token: 'y' } }), 'y');
  assert.throws(() => tokenFrom({}));
});
