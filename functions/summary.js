/** The rating totals for one bay from its ratings' star values; null when nobody has rated it. */
function summarise(bayId, starsList) {
  let count = 0;
  let total = 0;
  for (const s of starsList) {
    if (Number.isInteger(s) && s >= 1 && s <= 5) {
      count += 1;
      total += s;
    }
  }
  return count === 0 ? null : { bayId, count, total };
}

/** The summary document id for a bay id: slashes are not allowed in document ids. */
const summaryId = (bayId) => bayId.replace(/\//g, '_');

/** A bay id as the rules allow it (1 to 100 characters), or null. */
const validBayId = (v) => (typeof v === 'string' && v.length > 0 && v.length <= 100 ? v : null);

module.exports = { summarise, summaryId, validBayId };
