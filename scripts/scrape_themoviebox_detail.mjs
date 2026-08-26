import { chromium } from 'playwright';
import fs from 'fs';

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

  // Extract NUXXT_DATA from homepage
  console.log('[*] Extracting NUXXT_DATA...');
  const nuxtData = await page.evaluate(() => {
    const el = document.querySelector('#__NUXT_DATA__');
    return el ? el.textContent : null;
  });

  if (!nuxtData) {
    console.log('No NUXXT_DATA found');
    await browser.close();
    process.exit(1);
  }

  console.log(`NUXXT_DATA length: ${nuxtData.length}`);
  
  // Parse NUXXT_DATA
  const parsed = JSON.parse(nuxtData);
  console.log('NUXXT_DATA parsed, type:', Array.isArray(parsed) ? 'array' : typeof parsed);
  
  // Try to extract content from NUXXT_DATA
  const content = await page.evaluate((data) => {
    // Look for content arrays in the NUXXT data
    const results = [];
    
    // Try to find movie/show items
    function findItems(obj, depth = 0) {
      if (depth > 10) return;
      if (!obj || typeof obj !== 'object') return;
      
      if (Array.isArray(obj)) {
        // Check if this looks like a content array
        if (obj.length > 0 && obj[0] && typeof obj[0] === 'object') {
          const sample = obj[0];
          if (sample.title || sample.name || sample.subjectId || sample.id) {
            results.push({ type: 'array', length: obj.length, sample: JSON.stringify(sample).substring(0, 200) });
          }
        }
        obj.forEach(item => findItems(item, depth + 1));
      } else {
        for (const key in obj) {
          if (key === 'title' || key === 'name' || key === 'subjectId' || key === 'items' || key === 'subjects') {
            results.push({ type: 'object', key, value: JSON.stringify(obj[key]).substring(0, 200) });
          }
          findItems(obj[key], depth + 1);
        }
      }
    }
    
    findItems(data);
    return results.slice(0, 50);
  }, parsed);

  console.log('Found content structures:');
  content.forEach(item => console.log(`  ${item.type}: ${item.key || 'array'} - ${item.sample || item.value}`));

  // Try to extract all text content as fallback
  const allText = await page.evaluate(() => document.body.innerText);
  console.log(`\nHomepage text length: ${allText.length}`);
  console.log('First 2000 chars:');
  console.log(allText.substring(0, 2000));

  // Navigate to a detail page to get full metadata
  console.log('\n[*] Navigating to detail page...');
  const detailLinks = await page.evaluate(() => {
    return Array.from(document.querySelectorAll('a[href*="/detail/"]'))
      .map(a => a.getAttribute('href'))
      .filter(href => href && href.includes('/detail/'))
      .slice(0, 5);
  });

  if (detailLinks.length > 0) {
    const firstDetail = detailLinks[0];
    console.log(`Navigating to: ${firstDetail}`);
    await page.goto(`https://themoviebox.xyz${firstDetail}`, { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForTimeout(3000);

    // Extract detail page NUXXT_DATA
    const detailNuxt = await page.evaluate(() => {
      const el = document.querySelector('#__NUXT_DATA__');
      return el ? el.textContent : null;
    });

    if (detailNuxt) {
      console.log(`Detail NUXXT_DATA length: ${detailNuxt.length}`);
      
      // Try to extract structured data
      const detailData = await page.evaluate((nuxt) => {
        const data = JSON.parse(nuxt);
        
        function findStrings(obj, depth = 0) {
          if (depth > 15) return [];
          if (!obj || typeof obj !== 'object') return [];
          
          const results = [];
          if (Array.isArray(obj)) {
            obj.forEach(item => results.push(...findStrings(item, depth + 1)));
          } else {
            for (const key in obj) {
              const val = obj[key];
              if (typeof val === 'string' && val.length > 5 && val.length < 500) {
                results.push({ key, value: val });
              } else if (typeof val === 'object' && val !== null) {
                results.push(...findStrings(val, depth + 1));
              }
            }
          }
          return results;
        }
        
        return findStrings(data).slice(0, 100);
      }, detailNuxt);

      console.log('Found strings in detail NUXXT_DATA:');
      detailData.forEach(item => console.log(`  ${item.key}: ${item.value}`));
    }

    // Extract all text from detail page
    const detailText = await page.evaluate(() => document.body.innerText);
    console.log(`\nDetail page text length: ${detailText.length}`);
    console.log('First 2000 chars:');
    console.log(detailText.substring(0, 2000));
  }

  await browser.close();
  console.log('\n[*] Done');
})();
