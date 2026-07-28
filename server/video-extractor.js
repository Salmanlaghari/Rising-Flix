/**
 * MovieBox Video URL Extractor
 * 
 * Extracts direct video streaming URLs from MovieBox.pk
 * Uses multiple methods to find video sources
 */

const axios = require('axios');
const cheerio = require('cheerio');

const CONFIG = {
  userAgent: 'Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36',
  apiBaseUrl: 'https://h5-api.aoneroom.com',
  cdnBaseUrl: 'https://pacdn.aoneroom.com',
};

// Known video CDN patterns from MovieBox
const VIDEO_CDN_PATTERNS = [
  'pacdn.aoneroom.com',
  'fecdn.trasre.com',
  'cdn.aoneroom.com',
  'media.aoneroom.com',
];

// Extract video URLs from page HTML
function extractVideoUrlsFromHtml(html) {
  const urls = new Set();
  
  // Pattern 1: Direct MP4/M3U8 URLs
  const videoPatterns = [
    /https?:\/\/[^"'\s<>]+\.mp4[^"'\s<>]*/g,
    /https?:\/\/[^"'\s<>]+\.m3u8[^"'\s<>]*/g,
    /https?:\/\/[^"'\s<>]+\.ts[^"'\s<>]*/g,
    /https?:\/\/[^"'\s<>]+\.m4s[^"'\s<>]*/g,
  ];
  
  for (const pattern of videoPatterns) {
    let match;
    while ((match = pattern.exec(html)) !== null) {
      const url = match[0].replace(/['"<>]/g, '');
      if (!url.includes('.js') && !url.includes('.css') && !url.includes('.html')) {
        urls.add(url);
      }
    }
  }
  
  // Pattern 2: Video player configuration
  const configPatterns = [
    /videoUrl['":\s]*['"]([^'"]+)['"]/gi,
    /video_url['":\s]*['"]([^'"]+)['"]/gi,
    /stream_url['":\s]*['"]([^'"]+)['"]/gi,
    /playUrl['":\s]*['"]([^'"]+)['"]/gi,
    /play_url['":\s]*['"]([^'"]+)['"]/gi,
    /source['":\s]*['"]([^'"]+)['"]/gi,
    /src['":\s]*['"]([^'"]+\.mp4[^'"]*)['"]/gi,
    /src['":\s]*['"]([^'"]+\.m3u8[^'"]*)['"]/gi,
  ];
  
  for (const pattern of configPatterns) {
    let match;
    while ((match = pattern.exec(html)) !== null) {
      if (match[1] && (match[1].includes('.mp4') || match[1].includes('.m3u8'))) {
        urls.add(match[1]);
      }
    }
  }
  
  // Pattern 3: JSON-LD and structured data
  const jsonPatterns = [
    /"contentUrl"\s*:\s*"([^"]+)"/gi,
    /"embedUrl"\s*:\s*"([^"]+)"/gi,
    /"url"\s*:\s*"(https?:\/\/[^"]+\.(mp4|m3u8)[^"]*)"/gi,
  ];
  
  for (const pattern of jsonPatterns) {
    let match;
    while ((match = pattern.exec(html)) !== null) {
      if (match[1]) {
        urls.add(match[1]);
      }
    }
  }
  
  return Array.from(urls);
}

// Extract video URLs from API response
function extractVideoUrlsFromApi(data) {
  const urls = new Set();
  
  function traverse(obj) {
    if (!obj || typeof obj !== 'object') return;
    
    for (const key in obj) {
      const value = obj[key];
      
      if (typeof value === 'string') {
        // Check if it's a video URL
        if (value.match(/\.(mp4|m3u8|ts|m4s)(\?|$)/i) || 
            value.match(/video|stream|media|play/i)) {
          if (value.startsWith('http')) {
            urls.add(value);
          }
        }
      } else if (typeof value === 'object') {
        traverse(value);
      }
    }
  }
  
  traverse(data);
  return Array.from(urls);
}

// Get video URLs for a specific content ID
async function getVideoUrls(contentId) {
  console.log(`🔍 Extracting video URLs for content: ${contentId}`);
  
  const urls = new Set();
  
  try {
    // Method 1: Try the media-player/get-domain endpoint
    const domainResponse = await axios.get(`${CONFIG.apiBaseUrl}/wefeed-h5api-bff/media-player/get-domain`, {
      params: { subjectId: contentId },
      headers: {
        'User-Agent': CONFIG.userAgent,
        'Referer': 'https://moviebox.pk/',
        'Origin': 'https://moviebox.pk',
      },
      timeout: 10000,
    }).catch(() => null);
    
    if (domainResponse?.data) {
      const domainUrls = extractVideoUrlsFromApi(domainResponse.data);
      domainUrls.forEach(url => urls.add(url));
    }
    
    // Method 2: Try the subject detail endpoint
    const detailResponse = await axios.get(`${CONFIG.apiBaseUrl}/wefeed-h5api-bff/subject`, {
      params: { id: contentId, page: 1, perPage: 1 },
      headers: {
        'User-Agent': CONFIG.userAgent,
        'Referer': 'https://moviebox.pk/',
        'Origin': 'https://moviebox.pk',
      },
      timeout: 10000,
    }).catch(() => null);
    
    if (detailResponse?.data) {
      const detailUrls = extractVideoUrlsFromApi(detailResponse.data);
      detailUrls.forEach(url => urls.add(url));
    }
    
    // Method 3: Try scraping the detail page
    const pageResponse = await axios.get(`https://moviebox.pk/moviedetail/temp-${contentId}`, {
      params: { id: contentId },
      headers: {
        'User-Agent': CONFIG.userAgent,
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
      },
      timeout: 15000,
      maxRedirects: 5,
    }).catch(() => null);
    
    if (pageResponse?.data) {
      const pageUrls = extractVideoUrlsFromHtml(pageResponse.data);
      pageUrls.forEach(url => urls.add(url));
    }
    
  } catch (error) {
    console.error(`Error extracting video URLs: ${error.message}`);
  }
  
  const result = Array.from(urls);
  console.log(`✅ Found ${result.length} video URLs`);
  return result;
}

// Generate video URL for content
// This creates a URL that points to our proxy server
function generateProxyUrl(contentId, originalUrl, proxyBaseUrl) {
  if (!originalUrl) return null;
  
  // Encode the original URL
  const encodedUrl = Buffer.from(originalUrl).toString('base64url');
  
  return `${proxyBaseUrl}/stream/${contentId}/${encodedUrl}`;
}

module.exports = {
  extractVideoUrlsFromHtml,
  extractVideoUrlsFromApi,
  getVideoUrls,
  generateProxyUrl,
  CONFIG,
};
