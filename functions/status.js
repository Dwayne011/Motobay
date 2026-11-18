// Builds a bay's public status from riders' private reports: the latest "space now" or
// "full" report (no rider id), and yes/no totals for each safety feature.

const FEATURES = ['anchor', 'cctv', 'lit'];

/**
 * @param spaceReports [{ full: boolean, at: Date }]
 * @param featureDocs [{ anchor?: boolean, cctv?: boolean, lit?: boolean }]
 * @returns the bayStatus document's fields, or null when there's nothing to show.
 */
function summariseStatus(bayId, spaceReports, featureDocs) {
  let space = null;
  for (const r of spaceReports) {
    if (typeof r.full !== 'boolean' || !(r.at instanceof Date)) continue;
    if (!space || r.at > space.at) space = { full: r.full, at: r.at };
  }
  const features = {};
  for (const key of FEATURES) {
    let yes = 0;
    let no = 0;
    for (const d of featureDocs) {
      if (d[key] === true) yes += 1;
      else if (d[key] === false) no += 1;
    }
    if (yes + no > 0) features[key] = { yes, no };
  }
  if (!space && Object.keys(features).length === 0) return null;
  return { bayId, ...(space ? { space } : {}), features };
}

module.exports = { summariseStatus, FEATURES };
