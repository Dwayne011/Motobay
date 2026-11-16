// Renders the Play Store icon (512 x 512) and feature graphic (1024 x 500) into play/.
//   node scripts/store/render_graphics.mjs
// The feature graphic shows play/screenshots/2-explore-preview-dark.png in a phone frame.
import { createRequire } from 'node:module';
import { copyFileSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
const { chromium } = createRequire(import.meta.url)('playwright');

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..', '..');
const work = mkdtempSync(join(tmpdir(), 'store-'));
const browser = await chromium.launch();

// Icon: the SVG at exactly 512 x 512.
const icon = await browser.newPage({ viewport: { width: 512, height: 512 } });
await icon.setContent(`<html><body style="margin:0">${readFileSync(join(here, 'icon.svg'), 'utf8')}</body></html>`);
const iconPng = await icon.screenshot({ clip: { x: 0, y: 0, width: 512, height: 512 } });
writeFileSync(join(root, 'play', 'icon-512.png'), iconPng);

// Feature graphic: the HTML with the icon and a screenshot beside it.
writeFileSync(join(work, 'icon.png'), iconPng);
copyFileSync(join(root, 'play', 'screenshots', '2-explore-preview-dark.png'), join(work, 'screen.png'));
copyFileSync(join(here, 'feature.html'), join(work, 'feature.html'));
const feature = await browser.newPage({ viewport: { width: 1024, height: 500 } });
await feature.goto('file://' + join(work, 'feature.html'));
await feature.waitForLoadState('networkidle');
await feature.evaluate(() => document.fonts.ready);
writeFileSync(join(root, 'play', 'feature-graphic.png'), await feature.screenshot({ clip: { x: 0, y: 0, width: 1024, height: 500 } }));
await browser.close();
console.log('wrote play/icon-512.png and play/feature-graphic.png');
