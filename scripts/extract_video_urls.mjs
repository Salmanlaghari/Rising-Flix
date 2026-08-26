import { chromium } from 'playwright';
import fs from 'fs';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    ignoreHTTPSErrors: true
  });
  const page = await context.newPage();

  // Listen for network requests
  const videoUrls = [];
  page.on('response', async (response) => {
    const url = response.url();
    if (url.includes('.mp4') || url.includes('.m3u8') || url.includes('stream') || url.includes('video')) {
      videoUrls.push(url);
    }
  });

  console.log('[*] Navigating to detail page...');
  await page.goto('https://themoviebox.xyz/detail/apex-ibicaj7jji9', { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.waitForTimeout(10000);

  console.log('[*] Looking for video player...');
  
  // Try to find video elements
  const videoInfo = await page.evaluate(() => {
    const videos = Array.from(document.querySelectorAll('video'));
    const iframes = Array.from(document.querySelectorAll('iframe'));
    const players = Array.from(document.querySelectorAll('[class*="player"], [class*="video"], [id*="player"], [id*="video"]'));
    
    return {
      videoCount: videos.length,
      videoSrcs: videos.map(v => v.src || v.querySelector('source')?.src || '').filter(Boolean),
      iframeCount: iframes.length,
      iframeSrcs: iframes.map(i => i.src).filter(Boolean),
      playerCount: players.length,
      playerClasses: players.map(p => p.className).slice(0, 10)
    };
  });

  console.log('Video info:', JSON.stringify(videoInfo, null, 2));

  // Try to find play button and click it
  console.log('[*] Looking for play button...');
  const playButton = await page.locator('button:has-text("Watch"), button:has-text("Play"), [class*="play"], [class*="Play"]').first();
  if (await playButton.count() > 0) {
    console.log('Found play button, clicking...');
    await playButton.click();
    await page.waitForTimeout(5000);
  }

  // Check for any new video URLs after clicking
  console.log('Video URLs found:', videoUrls);

  // Take screenshot
  await page.screenshot({ path: '/tmp/themoviebox_detail.png' });
  console.log('Screenshot saved to /tmp/themoviebox_detail.png');

  // Get all network requests
  const requests = await page.evaluate(() => {
    return window.__NUXT__ ? 'NUXXT available' : 'No NUXXT';
  });
  console.log('Page state:', requests);

  await browser.close();
})();
