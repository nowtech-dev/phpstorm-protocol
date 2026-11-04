import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = path.dirname(fileURLToPath(import.meta.url));
const jsPath = path.join(dir, '../src/main/resources/protocol/open-url.js');
const text = fs.readFileSync(jsPath, 'utf8');
const lines = text.split(/\r?\n/);

const forbidden = [
  { re: /\b(let|const)\b/, msg: 'use var (JScript ES3)' },
  { re: /=>/, msg: 'arrow functions not supported' },
  { re: /`/, msg: 'template literals not supported' },
];

let failed = 0;
for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  for (const rule of forbidden) {
    if (rule.re.test(line)) {
      console.error(`FAIL line ${i + 1}: ${rule.msg}: ${line.trim()}`);
      failed++;
    }
  }
  if (/,\s*$/.test(line)) {
    const next = (lines[i + 1] || '').trim();
    if (next === '}' || next === '};' || next === ')' || next === ');') {
      console.error(`FAIL line ${i + 1}: trailing comma before ${next}`);
      failed++;
    }
  }
}

if (failed > 0) {
  process.exit(1);
}
console.log('OK JScript ES3 lint');
