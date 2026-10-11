const fs = require('fs');

const host = process.env.SITE_HOST;
const apiBaseUrl = process.env.API_BASE_URL;
if (!host || !apiBaseUrl) {
  throw new Error('SITE_HOST and API_BASE_URL are required');
}

const file = 'src/app/core/config/app-settings.ts';
const origin = `https://${host}`;
const patched = fs
  .readFileSync(file, 'utf8')
  .replaceAll('http://localhost:8080/bff', `${apiBaseUrl.replace(/\/$/, '')}/bff`)
  .replaceAll('http://localhost:4200', origin);

if (patched.includes('localhost')) {
  throw new Error('localhost still present after patch');
}

fs.writeFileSync(file, patched);
