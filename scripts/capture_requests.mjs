import { chromium } from 'playwright';
import fs from 'fs';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    ignoreHTTPSErrors: true
  });
  const page = await context.newPage();

  const requests = [];
  page.on('response', async (response) => {
    const url = response.url();
    requests.push({
      url: url,
      type: response.headers()['content-type'] || '',
      status: response.status()
    });
  });

  console.log('[*] Navigating to detail page...');
  await page.goto('https://themoviebox.xyz/detail/apex-ibicaj7jji9', { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.waitForTimeout(10000);

  // Filter for interesting requests
  const interesting = requests.filter(r => 
    r.url.includes('stream') || 
    r.url.includes('video') || 
    r.url.includes('play') ||
    r.url.includes('source') ||
    r.url.includes('mp4') ||
    r.url.includes('m3u8') ||
    r.type.includes('video') ||
    r.type.includes('application/vnd')
  );

  console.log('Interesting requests:');
  interesting.forEach(r => console.log(`  ${r.status} ${r.type}: ${r.url}`));

  // Also look for any JSON responses
  const jsonResponses = requests.filter(r => r.type.includes('json'));
  console.log('\nJSON responses:');
  jsonResponses.slice(0, 10).forEach(r => console.log(`  ${r.status}: ${r.url}`));

  // Get all requests
  console.log('\nAll requests:');
  requests.forEach(r => console.log(`  ${r.status} ${r.type}: ${r.url}`));

  await browser.close();
})();
