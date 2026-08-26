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
  
  // Helper to resolve a single reference
  function resolveRef(ref) {
    if (typeof ref !== 'number' || ref < 0 || ref >= raw.length) return ref;
    const val = raw[ref];
    if (typeof val === 'string' || typeof val === 'number' || typeof val === 'boolean') {
      return val;
    }
    return val;
  }

  // Extract all content items from the entire NUXXT_DATA
  const items = [];
  const seen = new Set();

  function scanForItems(obj) {
    if (!obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj)) {
      // Check if this array contains content items (skip wrapper arrays)
      if (obj.length > 0 && typeof obj[0] === 'object' && !Array.isArray(obj[0]) && obj[0].subjectId) {
        obj.forEach(item => {
          if (!item || !item.subjectId || seen.has(item.subjectId)) return;
          seen.add(item.subjectId);
          
          const subject = raw[item.subject];
          if (!subject) return;
          
          // Resolve fields
          const title = resolveRef(item.title) || resolveRef(subject.title) || 'Unknown';
          const detailPath = resolveRef(subject.detailPath) || resolveRef(item.detailPath) || '';
          
          // Resolve cover URL
          let coverUrl = '';
          const cover = subject.cover;
          if (cover && typeof cover === 'object' && typeof cover.url === 'number') {
            coverUrl = resolveRef(cover.url) || '';
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

  scanForItems(raw);

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
