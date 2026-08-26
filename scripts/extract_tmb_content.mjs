import { chromium } from 'playwright';
import fs from 'fs';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    ignoreHTTPSErrors: true
  });
  const page = await context.newPage();

  console.log('[*] Navigating to themoviebox.xyz...');
  await page.goto('https://themoviebox.xyz/', { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.waitForTimeout(8000);

  // Extract NUXXT_DATA
  const nuxtData = await page.evaluate(() => {
    const el = document.querySelector('#__NUXT_DATA__');
    return el ? el.textContent : null;
  });

  await browser.close();

  if (!nuxtData) {
    console.log('No NUXXT_DATA found');
    process.exit(1);
  }

  console.log(`NUXXT_DATA length: ${nuxtData.length}`);

  // Parse NUXXT_DATA
  const raw = JSON.parse(nuxtData);
  const resolved = new Array(raw.length);
  for (let i = 0; i < raw.length; i++) {
    resolved[i] = raw[i];
  }

  function resolveRef(ref) {
    if (Array.isArray(ref) && ref.length === 2 && ref[0] === 'ShallowReactive') {
      return resolveRef(resolved[ref[1]]);
    }
    if (Array.isArray(ref) && ref.length === 2 && ref[0] === 'Reactive') {
      return resolveRef(resolved[ref[1]]);
    }
    if (typeof ref === 'number' && ref < resolved.length) {
      return resolveRef(resolved[ref]);
    }
    return ref;
  }

  function resolveObject(obj) {
    if (!obj || typeof obj !== 'object') return obj;
    if (Array.isArray(obj)) {
      return obj.map(item => {
        if (Array.isArray(item) && item.length === 2 && (item[0] === 'ShallowReactive' || item[0] === 'Reactive')) {
          return resolveRef(item);
        }
        if (typeof item === 'number' && item < resolved.length) {
          return resolveRef(item);
        }
        return resolveObject(item);
      });
    }
    const result = {};
    for (const key in obj) {
      const val = obj[key];
      if (Array.isArray(val) && val.length === 2 && (val[0] === 'ShallowReactive' || val[0] === 'Reactive')) {
        result[key] = resolveRef(val);
      } else if (typeof val === 'number' && val < resolved.length) {
        result[key] = resolveRef(val);
      } else {
        result[key] = resolveObject(val);
      }
    }
    return result;
  }

  const data = resolveObject(raw);

  // Extract all content items
  const items = [];
  const seen = new Set();

  function extractItems(obj) {
    if (!obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj)) {
      // Check if this array contains content items
      if (obj.length > 0 && typeof obj[0] === 'object' && obj[0].subjectId) {
        obj.forEach(item => {
          if (item && item.subjectId && !seen.has(item.subjectId)) {
            seen.add(item.subjectId);
            // Resolve all references
            const resolvedItem = {};
            for (const key in item) {
              resolvedItem[key] = resolveRef(item[key]);
            }
            items.push({
              id: resolvedItem.id || resolvedItem.subjectId,
              subjectId: resolvedItem.subjectId,
              title: resolvedItem.title || '',
              detailPath: resolvedItem.detailPath || '',
              image: resolvedItem.image || resolvedItem.poster || resolvedItem.backdrop || '',
              url: resolvedItem.url || '',
              subjectType: resolvedItem.subjectType || '',
              category: resolvedItem.subjectType || 'movie',
              source: 'themoviebox.xyz',
              detailUrl: resolvedItem.detailPath ? `https://themoviebox.xyz/detail/${resolvedItem.detailPath}` : ''
            });
          }
        });
      }
      obj.forEach(item => extractItems(item));
    } else {
      for (const key in obj) {
        extractItems(obj[key]);
      }
    }
  }

  extractItems(data);

  console.log(`\n[*] Extracted ${items.length} unique content items`);
  
  // Show sample items
  items.slice(0, 10).forEach(item => {
    console.log(`  ${item.title} (${item.subjectId}) - ${item.detailUrl}`);
  });

  // Save to file
  const outputPath = '/tmp/themoviebox_content.json';
  fs.writeFileSync(outputPath, JSON.stringify(items, null, 2));
  console.log(`\n[*] Saved to ${outputPath}`);

  // Also try to extract categories/sections
  const sections = [];
  function extractSections(obj, depth = 0) {
    if (depth > 15 || !obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj) && obj.length > 0 && typeof obj[0] === 'object') {
      // Look for section headers
      const first = obj[0];
      if (first.title && first.subjects !== undefined) {
        sections.push({
          title: first.title,
          subjectsCount: first.subjects ? (Array.isArray(first.subjects) ? first.subjects.length : 0) : 0
        });
      }
      obj.forEach(item => extractSections(item, depth + 1));
    } else {
      for (const key in obj) {
        extractSections(obj[key], depth + 1);
      }
    }
  }

  extractSections(data);
  console.log(`\n[*] Found ${sections.length} sections:`);
  sections.forEach(sec => console.log(`  ${sec.title} (${sec.subjectsCount} subjects)`));

  fs.writeFileSync('/tmp/themoviebox_sections.json', JSON.stringify(sections, null, 2));

})();
