import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    ignoreHTTPSErrors: true
  });
  const page = await context.newPage();

  // Navigate to themoviebox.xyz
  console.log('[*] Navigating to themoviebox.xyz...');
  await page.goto('https://themoviebox.xyz/', { waitUntil: 'networkidle', timeout: 60000 });
  await page.waitForTimeout(5000);

  // Extract homepage content
  console.log('[*] Extracting homepage content...');
  const homeData = await page.evaluate(() => {
    const results = [];
    
    // Try to find Vue/Nuxt data
    const nuxtData = document.querySelector('#__NUXT_DATA__');
    if (nuxtData) {
      try {
        return { type: 'nuxt_data', data: JSON.parse(nuxtData.textContent || '') };
      } catch (e) {
        return { type: 'nuxt_data_error', error: e.message };
      }
    }
    
    // Extract text content
    const bodyText = document.body.innerText;
    return { type: 'text', data: bodyText };
  });

  console.log('Homepage data type:', homeData.type);
  if (homeData.type === 'text') {
    console.log('Homepage text length:', homeData.data.length);
    console.log('First 1000 chars:', homeData.data.substring(0, 1000));
  } else if (homeData.type === 'nuxt_data') {
    console.log('NUXXT_DATA found, length:', JSON.stringify(homeData.data).length);
  }

  // Try to find and click on some content to navigate to detail pages
  console.log('[*] Looking for content links...');
  const links = await page.evaluate(() => {
    const links = [];
    document.querySelectorAll('a[href*="/detail/"]').forEach(a => {
      links.push({
        href: a.getAttribute('href'),
        text: a.innerText.trim().substring(0, 100)
      });
    });
    return links.slice(0, 20);
  });

  console.log(`Found ${links.length} detail links:`);
  links.forEach(link => console.log(`  ${link.href}: ${link.text}`));

  await browser.close();
})();
