/**
 * Rising Flix Video Proxy Server
 * 
 * This server:
 * 1. Proxies video streams from MovieBox.pk
 * 2. Strips all MovieBox branding/headers
 * 3. Serves clean video content to Rising Flix app
 * 4. Caches content metadata
 * 
 * Usage: node proxy-server.js
 */

const express = require('express');
const cors = require('cors');
const axios = require('axios');
const fs = require('fs');
const path = require('path');
const { extractVideoUrlsFromApi, extractVideoUrlsFromHtml } = require('./video-extractor');

const app = express();
const PORT = process.env.PORT || 8080;

// Middleware
app.use(cors({
  origin: '*',
  methods: ['GET', 'HEAD', 'OPTIONS'],
  allowedHeaders: ['Range', 'Content-Type', 'Accept'],
  exposedHeaders: ['Content-Range', 'Accept-Ranges', 'Content-Length'],
}));

// Content cache
let contentCache = new Map();
const cachePath = path.join(__dirname, 'output', 'content_cache.json');

// Load cache from file
function loadCache() {
  try {
    if (fs.existsSync(cachePath)) {
      const data = JSON.parse(fs.readFileSync(cachePath, 'utf8'));
      contentCache = new Map(Object.entries(data));
      console.log(`✅ Loaded ${contentCache.size} items from cache`);
    }
  } catch (error) {
    console.error('Error loading cache:', error.message);
  }
}

// Save cache to file
function saveCache() {
  try {
    const dir = path.dirname(cachePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    const data = Object.fromEntries(contentCache);
    fs.writeFileSync(cachePath, JSON.stringify(data, null, 2));
  } catch (error) {
    console.error('Error saving cache:', error.message);
  }
}

// Load cache on startup
loadCache();

// Helper: Make request with retry
async function makeRequest(url, options = {}, retries = 3) {
  for (let i = 0; i < retries; i++) {
    try {
      const response = await axios({
        url,
        timeout: 30000,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36',
          ...options.headers,
        },
        ...options,
      });
      return response;
    } catch (error) {
      if (i < retries - 1) {
        await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
      } else {
        throw error;
      }
    }
  }
}

// ===== API ENDPOINTS =====

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    cachedContent: contentCache.size,
  });
});

// Get all content (for Rising Flix app)
app.get('/api/content', async (req, res) => {
  try {
    const { category, page = 1, limit = 50 } = req.query;
    
    // Get content from cache or scrape
    let allContent = Array.from(contentCache.values());
    
    // If cache is empty, provide sample structure
    if (allContent.length === 0) {
      allContent = getSampleContent();
    }
    
    // Filter by category if provided
    if (category) {
      allContent = allContent.filter(item => 
        item.category?.toLowerCase() === category.toLowerCase()
      );
    }
    
    // Paginate
    const startIndex = (page - 1) * limit;
    const endIndex = startIndex + parseInt(limit);
    const paginatedContent = allContent.slice(startIndex, endIndex);
    
    // Format for Rising Flix
    const response = {
      featured: allContent[0] || null,
      categories: formatCategories(allContent),
    };
    
    res.json(response);
  } catch (error) {
    console.error('Error getting content:', error.message);
    res.status(500).json({ error: 'Failed to get content' });
  }
});

// Get specific content details
app.get('/api/content/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    // Check cache first
    let content = contentCache.get(id);
    
    if (!content) {
      // Try to fetch from MovieBox
      content = await fetchContentFromMovieBox(id);
      
      if (content) {
        contentCache.set(id, content);
        saveCache();
      }
    }
    
    if (!content) {
      return res.status(404).json({ error: 'Content not found' });
    }
    
    res.json(content);
  } catch (error) {
    console.error('Error getting content details:', error.message);
    res.status(500).json({ error: 'Failed to get content details' });
  }
});

// Search content
app.get('/api/search', async (req, res) => {
  try {
    const { q, page = 1, limit = 20 } = req.query;
    
    if (!q) {
      return res.status(400).json({ error: 'Search query required' });
    }
    
    const allContent = Array.from(contentCache.values());
    const query = q.toLowerCase();
    
    const results = allContent.filter(item =>
      item.title?.toLowerCase().includes(query) ||
      item.description?.toLowerCase().includes(query) ||
      item.category?.toLowerCase().includes(query)
    );
    
    const startIndex = (page - 1) * limit;
    const endIndex = startIndex + parseInt(limit);
    const paginatedResults = results.slice(startIndex, endIndex);
    
    res.json({
      results: paginatedResults,
      total: results.length,
      page: parseInt(page),
      limit: parseInt(limit),
    });
  } catch (error) {
    console.error('Error searching:', error.message);
    res.status(500).json({ error: 'Search failed' });
  }
});

// Get trending content
app.get('/api/trending', (req, res) => {
  const { limit = 10 } = req.query;
  const allContent = Array.from(contentCache.values());
  
  // Sort by rating or return first N
  const trending = allContent
    .sort((a, b) => (parseFloat(b.rating) || 0) - (parseFloat(a.rating) || 0))
    .slice(0, parseInt(limit));
  
  res.json(trending);
});

// ===== VIDEO PROXY ENDPOINTS =====

// Stream video content
app.get('/stream/:contentId/*', async (req, res) => {
  try {
    const { contentId } = req.params;
    const encodedUrl = req.params[0];
    
    // Decode the original URL
    const originalUrl = Buffer.from(encodedUrl, 'base64url').toString('utf-8');
    
    console.log(`📹 Streaming: ${contentId} -> ${originalUrl.substring(0, 100)}...`);
    
    // Forward range headers for video seeking
    const headers = {
      'User-Agent': 'Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36',
      'Accept': '*/*',
      'Accept-Encoding': 'identity',
      'Connection': 'keep-alive',
    };
    
    if (req.headers.range) {
      headers['Range'] = req.headers.range;
    }
    
    // Make request to original video URL
    const response = await axios({
      method: 'GET',
      url: originalUrl,
      headers: headers,
      responseType: 'stream',
      timeout: 60000,
      maxRedirects: 5,
    });
    
    // Set response headers (strip MovieBox headers)
    const responseHeaders = {
      'Content-Type': response.headers['content-type'] || 'video/mp4',
      'Accept-Ranges': 'bytes',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS',
      'Access-Control-Allow-Headers': 'Range',
      'Cache-Control': 'public, max-age=3600',
    };
    
    if (response.headers['content-length']) {
      responseHeaders['Content-Length'] = response.headers['content-length'];
    }
    
    if (response.headers['content-range']) {
      responseHeaders['Content-Range'] = response.headers['content-range'];
      res.status(206);
    }
    
    res.set(responseHeaders);
    
    // Pipe the video stream
    response.data.pipe(res);
    
    // Handle errors
    response.data.on('error', (error) => {
      console.error('Stream error:', error.message);
      if (!res.headersSent) {
        res.status(500).end();
      }
    });
    
  } catch (error) {
    console.error('Proxy error:', error.message);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Failed to stream video' });
    }
  }
});

// Get video URL for content
app.get('/api/video/:contentId', async (req, res) => {
  try {
    const { contentId } = req.params;
    const { quality = '720p' } = req.query;
    
    // Check cache for video URL
    let content = contentCache.get(contentId);
    
    if (!content) {
      content = await fetchContentFromMovieBox(contentId);
      
      if (content) {
        contentCache.set(contentId, content);
        saveCache();
      }
    }
    
    if (!content || !content.videoUrl) {
      return res.status(404).json({ error: 'Video not found' });
    }
    
    // Generate proxy URL
    const proxyUrl = `${req.protocol}://${req.get('host')}/stream/${contentId}/${Buffer.from(content.videoUrl).toString('base64url')}`;
    
    res.json({
      contentId: contentId,
      title: content.title,
      videoUrl: proxyUrl,
      originalUrl: content.videoUrl,
      quality: quality,
      poster: content.poster,
    });
  } catch (error) {
    console.error('Error getting video URL:', error.message);
    res.status(500).json({ error: 'Failed to get video URL' });
  }
});

// ===== HELPER FUNCTIONS =====

// Fetch content from MovieBox
async function fetchContentFromMovieBox(contentId) {
  console.log(`🔍 Fetching content from MovieBox: ${contentId}`);
  
  try {
    // Try to get content details from MovieBox API
    const response = await makeRequest(`https://h5-api.aoneroom.com/wefeed-h5api-bff/subject`, {
      params: { id: contentId, page: 1, perPage: 1 },
      headers: {
        'Referer': 'https://moviebox.pk/',
        'Origin': 'https://moviebox.pk',
      },
    });
    
    if (response?.data?.data) {
      const data = response.data.data;
      
      // Extract video URLs
      const videoUrls = extractVideoUrlsFromApi(data);
      
      return {
        id: contentId,
        title: data.title || data.name || 'Unknown',
        description: data.description || data.desc || '',
        poster: data.poster || data.image || data.cover || '',
        backdrop: data.backdrop || data.bgImage || '',
        rating: data.rating || data.score || '0',
        year: data.year || data.releaseDate || '',
        duration: data.duration || '',
        category: data.category || data.type || 'Movies',
        videoUrl: videoUrls[0] || null,
        videoUrls: videoUrls,
      };
    }
  } catch (error) {
    console.error('Error fetching from MovieBox:', error.message);
  }
  
  return null;
}

// Format content into categories for Rising Flix
function formatCategories(content) {
  const categories = new Map();
  
  for (const item of content) {
    const category = item.category || 'Other';
    
    if (!categories.has(category)) {
      categories.set(category, {
        id: `cat_${category.toLowerCase().replace(/\s+/g, '_')}`,
        name: category,
        icon: getCategoryIcon(category),
        items: [],
      });
    }
    
    categories.get(category).items.push({
      id: item.id,
      title: item.title,
      category: item.category,
      videoUrl: item.videoUrl || '',
      thumbnailUrl: item.poster || item.thumbnailUrl || '',
      backdrop: item.backdrop || item.poster || '',
      description: item.description || '',
      rating: item.rating || '0',
      quality: item.quality || 'HD',
      year: item.year || '',
      duration: item.duration || '',
    });
  }
  
  return Array.from(categories.values());
}

// Get icon for category
function getCategoryIcon(category) {
  const icons = {
    'Movies': 'movie',
    'Series': 'tv',
    'Dramas': 'face',
    'Anime': 'animation',
    'Sports': 'sports',
    'Live': 'live_tv',
    'Documentary': 'document',
  };
  
  return icons[category] || 'movie';
}

// Sample content for testing
function getSampleContent() {
  return [
    {
      id: 'sample_001',
      title: 'Sample Movie',
      description: 'This is a sample movie. Run the scraper to get real content.',
      poster: 'https://via.placeholder.com/300x450/1a1a2e/06b6d4?text=Run+Scraper',
      backdrop: 'https://via.placeholder.com/1200x600/1a1a2e/06b6d4?text=Run+Scraper',
      rating: '0.0',
      year: '2026',
      duration: '0 min',
      category: 'Movies',
      videoUrl: '',
      quality: 'HD',
    },
  ];
}

// ===== START SERVER =====

app.listen(PORT, () => {
  console.log(`
╔════════════════════════════════════════════════════════════╗
║                                                            ║
║   🎬 Rising Flix Video Proxy Server                       ║
║                                                            ║
║   Server running on: http://localhost:${PORT}               ║
║                                                            ║
║   API Endpoints:                                           ║
║   • GET /api/content       - All content                   ║
║   • GET /api/content/:id   - Content details               ║
║   • GET /api/search?q=     - Search content                ║
║   • GET /api/trending      - Trending content              ║
║   • GET /api/video/:id     - Get video URL                 ║
║   • GET /stream/:id/*      - Stream video                  ║
║                                                            ║
║   Cached content: ${contentCache.size} items                              ║
║                                                            ║
╚════════════════════════════════════════════════════════════╝
  `);
});

module.exports = app;
