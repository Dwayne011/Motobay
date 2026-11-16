const test = require('node:test');
const assert = require('node:assert/strict');
const { summarise, summaryId, validBayId } = require('./summary');

test('totals count only whole stars from 1 to 5', () => {
  assert.deepEqual(summarise('node/1', [5, 4, 0, 6, 2.5, '3', null]), { bayId: 'node/1', count: 2, total: 9 });
});
test('no valid ratings means no summary', () => {
  assert.equal(summarise('node/1', []), null);
  assert.equal(summarise('node/1', [0, 9]), null);
});
test('summary ids replace every slash', () => {
  assert.equal(summaryId('council/camden/bedford-place-1'), 'council_camden_bedford-place-1');
});
test('bay ids must be 1 to 100 characters of text', () => {
  assert.equal(validBayId('node/1'), 'node/1');
  assert.equal(validBayId(''), null);
  assert.equal(validBayId('x'.repeat(101)), null);
  assert.equal(validBayId(42), null);
});
