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

  // Parse NUXXT_DATA (it's a reference-based array)
  const raw = JSON.parse(nuxtData);
  
  // Resolve references
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
  
  // Save raw resolved data for inspection
  fs.mkdirSync('/tmp/tmb_data', { recursive: true });
  fs.writeFileSync('/tmp/tmb_data/resolved.json', JSON.stringify(data, null, 2));
  console.log('Saved resolved data to /tmp/tmb_data/resolved.json');

  // Try to extract content
  console.log('\n[*] Searching for content in resolved data...');
  
  function findContent(obj, path = '', depth = 0) {
    if (depth > 20) return [];
    if (!obj || typeof obj !== 'object') return [];
    
    const results = [];
    
    if (Array.isArray(obj)) {
      // Check if this is a content array
      if (obj.length > 0 && typeof obj[0] === 'object' && (obj[0].title || obj[0].name || obj[0].subjectId)) {
        results.push({ type: 'content_array', path, count: obj.length, sample: JSON.stringify(obj[0]).substring(0, 300) });
      }
      obj.forEach((item, i) => {
        results.push(...findContent(item, `${path}[${i}]`, depth + 1));
      });
    } else {
      for (const key in obj) {
        const val = obj[key];
        if (key === 'title' && typeof val === 'string' && val.length > 0) {
          results.push({ type: 'title', path, value: val });
        } else if (key === 'subjects' && Array.isArray(val)) {
          results.push({ type: 'subjects', path, count: val.length });
        } else if (key === 'items' && Array.isArray(val)) {
          results.push({ type: 'items', path, count: val.length });
        } else if (key === 'data' && typeof val === 'object') {
          results.push({ type: 'data', path, keys: Object.keys(val) });
        }
        results.push(...findContent(val, `${path}.${key}`, depth + 1));
      }
    }
    return results;
  }

  const content = findContent(data);
  console.log(`Found ${content.length} content structures`);
  content.slice(0, 30).forEach(item => {
    console.log(`  ${item.type} at ${item.path}: ${JSON.stringify(item).substring(0, 200)}`);
  });

})();
