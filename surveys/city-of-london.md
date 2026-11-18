# City of London bay survey

Bays here come from riders' own visits, never from the City's map. The City's map layer is Ordnance Survey data that can't be reused.

## Next walk: to do

- [ ] Double-check every position below. Tall buildings may have thrown the GPS off.
- [ ] Count the spaces at each bay (or measure the bay's length).
- [ ] Save each position as an exported GPS pin (GPX, KML or a shared map link), not typed in, so the full precision is kept. Waiting a few seconds in the open for the GPS to settle helps between tall buildings.
- [ ] Note which street the bay is on and which side, plus the nearest junction or building number.
- [ ] Check the two covered bays near Tower Hill (Shorter Street, and Hammett Street at Trinity Square / Minories): on-street, or inside a car park? Their price stays unknown until then.
- [ ] Primrose Street / Appold Street is at the edge of the City, in Broadgate: confirm it's a public on-street bay.
- [ ] Re-record High Holborn / Furnival Street if it's really there: the pin landed just outside the City, so it was left out.

## November 2026 walk (second visit)

52 bays inside the City, in `data/council-bays.json` (ids `council/city-of-london/...`, source `survey-col`):

- 38 match a bay OpenStreetMap already has (within 25 m). These keep OpenStreetMap's position and gain the rider's note.
- 14 are new. They're marked approximate in the app ("pin is approximate") until re-checked:
  Camomile Street / St Mary Axe; Cannon Street / Bush Lane; Carmelite Street (South); Coleman Street / London Wall; Fenchurch Avenue / Lime Street; Fenchurch Street / Lloyd's Avenue; Finsbury Circus (North & East); John Carpenter Street / Tallis Street; King Street / Trump Street; Little Britain / Bartholomew Close; Primrose Street / Appold Street; St Mary Axe / Bury Street; Temple Avenue (South); Whitefriars Street / Ashentree Court.

Once a bay is re-checked, remove `"approximate": true` from its entry and add `capacity` (spaces) or `lengthMetres`.
