const http = require('http');
const fs = require('fs');
const path = require('path');

const DIST = path.join(__dirname, '..', 'frontend', 'dist');
const BACKEND = 'http://localhost:13355';
const PORT = 5200;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.json': 'application/json',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
};

const server = http.createServer((req, res) => {
  // /api/* → proxy to backend
  if (req.url.startsWith('/api')) {
    const options = {
      hostname: 'localhost',
      port: 13355,
      path: req.url,
      method: req.method,
      headers: { ...req.headers, host: 'localhost:13355' },
    };
    const proxyReq = http.request(options, proxyRes => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    });
    proxyReq.on('error', e => { res.writeHead(502); res.end('Backend unreachable'); });
    req.pipe(proxyReq);
    return;
  }
  // Static files from dist
  let filePath = path.join(DIST, req.url === '/' ? 'index.html' : req.url);
  if (!fs.existsSync(filePath)) {
    // SPA fallback: return index.html for any non-file route
    filePath = path.join(DIST, 'index.html');
  }
  const ext = path.extname(filePath);
  const ct = MIME[ext] || 'application/octet-stream';
  try {
    const data = fs.readFileSync(filePath);
    res.writeHead(200, { 'Content-Type': ct, 'Cache-Control': 'no-store' });
    res.end(data);
  } catch (e) {
    res.writeHead(404);
    res.end('Not found');
  }
});

server.listen(PORT, () => {
  console.log(`Static+Proxy server on http://localhost:${PORT}`);
});
