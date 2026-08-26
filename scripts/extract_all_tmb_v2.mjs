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

  const raw = JSON.parse(nuxtData);
  const resolved = new Array(raw.length);
  for (let i = 0; i < raw.length; i++) {
    resolved[i] = raw[i];
  }

  // Debug: check what resolved[71] is
  console.log('Debug: resolved[71] =', JSON.stringify(resolved[71]).substring(0, 100));

  // Resolve references
  function resolveRef(ref) {
    if (Array.isArray(ref) && ref.length === 2 && (ref[0] === 'ShallowReactive' || ref[0] === 'Reactive')) {
      return resolveRef(resolved[ref[1]]);
    }
    if (typeof ref === 'number' && ref >= 0 && ref < resolved.length) {
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
        if (typeof item === 'number' && item >= 0 && item < resolved.length) {
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
      } else if (typeof val === 'number' && val >= 0 && val < resolved.length) {
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

  function scanForItems(obj) {
    if (!obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj)) {
      // Skip wrapper arrays
      if (obj.length > 0 && Array.isArray(obj[0])) return;
      
      // Check if this array contains content items
      if (obj.length > 0 && typeof obj[0] === 'object' && obj[0].subjectId) {
        obj.forEach(item => {
          if (!item || !item.subjectId || seen.has(item.subjectId)) return;
          seen.add(item.subjectId);
          
          const subject = item.subject ? resolveRef(item.subject) : null;
          if (!subject) return;
          
          console.log(`Debug item: title=${resolveRef(item.title)}, subject type=${typeof subject}, subject keys=${subject ? Object.keys(subject).slice(0, 10).join(',') : 'null'}`);
          if (subject.cover) {
            console.log(`Debug cover: type=${typeof subject.cover}, keys=${Object.keys(subject.cover).join(',')}`);
          }
          
          // Resolve fields
          const title = resolveRef(item.title) || resolveRef(subject.title) || 'Unknown';
          const detailPath = resolveRef(subject.detailPath) || resolveRef(item.detailPath) || '';
          
          // Resolve cover URL
          let coverUrl = '';
          const coverRef = subject.cover;
          const cover = resolveRef(coverRef);
          if (cover && typeof cover === 'object') {
            const urlRef = cover.url;
            console.log(`Debug urlRef=${urlRef}, type=${typeof urlRef}`);
            if (typeof urlRef === 'number') {
              const resolvedUrl = resolveRef(urlRef);
              console.log(`Debug resolvedUrl=${resolvedUrl}`);
              coverUrl = resolvedUrl || '';
            } else if (typeof urlRef === 'string') {
              coverUrl = urlRef;
            }
          }
          
          // Resolve other metadata
          const genre = resolveRef(subject.genre) || '';
          const releaseDate = resolveRef(subject.releaseDate) || '';
          const year = releaseDate ? String(releaseDate).split('-')[0] : '';
          const rating = resolveRef(subject.imdbRatingValue) || '';
          const country = resolveRef(subject.countryName) || '';
          const description = resolveRef(subject.description) || '';
          
          items.push({
            id: String(subject.subjectId || item.subjectId),
            title: title,
            detailPath: detailPath,
            detailUrl: detailPath ? `https://themoviebox.xyz/detail/${detailPath}` : '',
            coverUrl: coverUrl,
            genre: genre,
            year: year,
            rating: rating,
            country: country,
            description: description,
            source: 'themoviebox.xyz',
            category: genre.includes('Animation') || genre.includes('Anime') ? 'Animation' : 
                      genre.includes('TV') || genre.includes('Series') ? 'TV Series' : 'Movies'
          });
        });
      }
      obj.forEach(item => scanForItems(item));
    } else {
      for (const key in obj) {
        scanForItems(obj[key]);
      }
    }
  }

  scanForItems(data);

  console.log(`\n[*] Extracted ${items.length} unique content items`);
  
  // Show stats
  const withMetadata = items.filter(item => item.title && item.title !== 'Unknown');
  console.log(`[*] ${withMetadata.length} items have titles`);
  
  const withCover = items.filter(item => item.coverUrl);
  console.log(`[*] ${withCover.length} items have cover images`);
  
  const withGenre = items.filter(item => item.genre);
  console.log(`[*] ${withGenre.length} items have genre`);
  
  const withYear = items.filter(item => item.year);
  console.log(`[*] ${withYear.length} items have year`);
  
  // Show sample
  withMetadata.slice(0, 10).forEach(item => {
    console.log(`\n  ${item.title} (${item.year})`);
    console.log(`    Genre: ${item.genre}`);
    console.log(`    Rating: ${item.rating}`);
    console.log(`    Country: ${item.country}`);
    console.log(`    Cover: ${item.coverUrl ? 'yes' : 'no'}`);
    console.log(`    Detail: ${item.detailUrl}`);
  });

  // Save to file
  const outputPath = '/tmp/themoviebox_full_content.json';
  fs.writeFileSync(outputPath, JSON.stringify(items, null, 2));
  console.log(`\n[*] Saved ${items.length} items to ${outputPath}`);

})();
