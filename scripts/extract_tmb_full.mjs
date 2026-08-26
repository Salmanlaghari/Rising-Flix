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

  // Extract all content items with full metadata
  const items = [];
  const seen = new Set();

  function extractItems(obj) {
    if (!obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj)) {
      if (obj.length > 0 && typeof obj[0] === 'object' && obj[0].subjectId) {
        obj.forEach(item => {
          if (item && item.subjectId && !seen.has(item.subjectId)) {
            seen.add(item.subjectId);
            // Resolve all references in the item
            const resolvedItem = {};
            for (const key in item) {
              resolvedItem[key] = resolveRef(item[key]);
            }
            
            const detailPath = resolvedItem.detailPath || '';
            items.push({
              id: String(resolvedItem.id || resolvedItem.subjectId),
              subjectId: String(resolvedItem.subjectId),
              title: resolvedItem.title || '',
              detailPath: detailPath,
              image: resolvedItem.image || resolvedItem.poster || resolvedItem.backdrop || '',
              cover: resolvedItem.cover || '',
              url: resolvedItem.url || '',
              subjectType: resolvedItem.subjectType || '',
              category: resolvedItem.subjectType || 'movie',
              source: 'themoviebox.xyz',
              detailUrl: detailPath ? `https://themoviebox.xyz/detail/${detailPath}` : '',
              releaseDate: resolvedItem.releaseDate || '',
              year: resolvedItem.releaseDate ? String(resolvedItem.releaseDate).split('-')[0] : '',
              duration: resolvedItem.duration || '',
              genre: resolvedItem.genre || '',
              countryName: resolvedItem.countryName || '',
              imdbRatingValue: resolvedItem.imdbRatingValue || '',
              imdbRatingCount: resolvedItem.imdbRatingCount || '',
              corner: resolvedItem.corner || '',
              trailer: resolvedItem.trailer || '',
              subtitles: resolvedItem.subtitles || '',
              dubs: resolvedItem.dubs || '',
              staffList: resolvedItem.staffList || [],
              stills: resolvedItem.stills || [],
              season: resolvedItem.season || '',
              freeEpisodeCount: resolvedItem.freeEpisodeCount || '',
              requiredVipLevel: resolvedItem.requiredVipLevel || '',
              hasResource: resolvedItem.hasResource || false
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
  
  // Show sample items with metadata
  const withMetadata = items.filter(item => item.title && (item.genre || item.releaseDate || item.imdbRatingValue));
  console.log(`[*] ${withMetadata.length} items have additional metadata`);
  
  withMetadata.slice(0, 5).forEach(item => {
    console.log(`\n  Title: ${item.title}`);
    console.log(`  Year: ${item.year}`);
    console.log(`  Genre: ${item.genre}`);
    console.log(`  Rating: ${item.imdbRatingValue}`);
    console.log(`  Country: ${item.countryName}`);
    console.log(`  Duration: ${item.duration}`);
    console.log(`  Detail: ${item.detailUrl}`);
  });

  // Save to file
  const outputPath = '/tmp/themoviebox_full_metadata.json';
  fs.writeFileSync(outputPath, JSON.stringify(items, null, 2));
  console.log(`\n[*] Saved ${items.length} items to ${outputPath}`);

  // Extract sections/categories
  const sections = [];
  function extractSections(obj, depth = 0) {
    if (depth > 15 || !obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj) && obj.length > 0 && typeof obj[0] === 'object') {
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
