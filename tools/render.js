// Renders the SVG/HTML sources listed by generate.py to PNG.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const jobs = JSON.parse(fs.readFileSync(path.join(__dirname, '.build', 'jobs.json'), 'utf8'));

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  for (const [src, out, width, height, transparent] of jobs) {
    await page.setViewportSize({ width, height });
    const file = path.join(root, src);
    if (src.endsWith('.svg')) {
      const svg = fs.readFileSync(file, 'utf8');
      await page.setContent(`<body style="margin:0;background:transparent">${svg}</body>`);
    } else {
      await page.goto('file://' + file);
    }
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(root, out), omitBackground: transparent });
  }
  await browser.close();
  console.log(`${jobs.length} images rendered`);
})();
