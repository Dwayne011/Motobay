// Renders the map behind the Play Store screenshots, using the same styles as the app:
// OpenFreeMap "liberty" in light mode and data/map/graphite.json in dark mode.
// The screenshot tests draw the app's own pins and screens on top of these pictures.
//
//   node scripts/store/render_map.mjs <out-dir>
//
// The camera must match StoreScreenshotTest: 360 x 640 dp at 3x, centred on Bloomsbury.
import { createRequire } from 'node:module';
const { chromium } = createRequire(import.meta.url)('playwright');
import { writeFileSync } from 'node:fs';

const out = process.argv[2] ?? '.';
const camera = { lat: 51.5205, lon: -0.1235, zoom: 15 };
const styles = {
  light: 'https://tiles.openfreemap.org/styles/liberty',
  dark: 'https://raw.githubusercontent.com/Dwayne011/Motobay/main/data/map/graphite.json',
};

const page = (style) => `<!doctype html><html><head>
<link href="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.css" rel="stylesheet">
<script src="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.js"></script>
<style>html,body,#map{margin:0;width:360px;height:640px}</style></head><body><div id="map"></div><script>
const map = new maplibregl.Map({ container: 'map', style: ${JSON.stringify(style)},
  center: [${camera.lon}, ${camera.lat}], zoom: ${camera.zoom}, attributionControl: false,
  interactive: false, fadeDuration: 0, preserveDrawingBuffer: true });
map.once('idle', () => { window.ready = true; });
</script></body></html>`;

const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--ignore-gpu-blocklist'] });
for (const [name, style] of Object.entries(styles)) {
  const tab = await browser.newPage({ viewport: { width: 360, height: 640 }, deviceScaleFactor: 3 });
  await tab.setContent(page(style));
  await tab.waitForFunction(() => window.ready === true, null, { timeout: 120000 });
  await tab.waitForTimeout(500);
  writeFileSync(`${out}/map-${name}.png`, await tab.screenshot({ type: 'png' }));
  console.log('rendered', name);
  await tab.close();
}
await browser.close();
