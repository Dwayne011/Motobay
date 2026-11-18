const test = require('node:test');
const assert = require('node:assert/strict');
const { summariseStatus } = require('./status');

test('the latest space report wins, with no rider id', () => {
  const s = summariseStatus('node/1', [
    { full: true, at: new Date('2026-11-18T09:00:00Z') },
    { full: false, at: new Date('2026-11-18T10:00:00Z') },
  ], []);
  assert.deepEqual(s, { bayId: 'node/1', space: { full: false, at: new Date('2026-11-18T10:00:00Z') }, features: {} });
});
test('feature answers are counted yes and no; missing answers are ignored', () => {
  const s = summariseStatus('node/1', [], [{ anchor: true, cctv: false }, { anchor: true }, { anchor: false, lit: 'yes' }]);
  assert.deepEqual(s.features, { anchor: { yes: 2, no: 1 }, cctv: { yes: 0, no: 1 } });
  assert.equal(s.space, undefined);
});
test('bad reports are skipped, and nothing left means no status', () => {
  assert.equal(summariseStatus('node/1', [{ full: 'yes', at: new Date() }, { full: true, at: null }], [{}]), null);
});
