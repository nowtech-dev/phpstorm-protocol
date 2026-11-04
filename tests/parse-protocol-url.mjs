/** Same logic as open-url.js (must stay in sync). */
export function normalizeIncomingUrl(raw) {
  let url = raw;
  if (!url) {
    return '';
  }
  url = String(url).trim();
  while (url.length > 0 && (url.charAt(0) === '\\' || url.charAt(0) === '/')) {
    url = url.substring(1);
  }
  while (url.length > 0 && (url.charAt(url.length - 1) === '\\' || url.charAt(url.length - 1) === '/')) {
    url = url.substring(0, url.length - 1);
  }
  if (url.length > 1 && url.charAt(0) === '"' && url.charAt(url.length - 1) === '"') {
    url = url.substring(1, url.length - 1);
  }
  return url;
}

export function parseProtocolUrl(rawUrl) {
  let url = normalizeIncomingUrl(rawUrl);
  try {
    url = decodeURIComponent(url);
  } catch {
    url = normalizeIncomingUrl(rawUrl);
  }

  const match =
    /^(?:phpstorm|idea):\/\/open\/?\?(?:url=file:\/\/|file=)(.+?)(?:&line=(\d+))?$/i.exec(url);
  if (!match) {
    return null;
  }

  let file = decodeURIComponent(match[1].replace(/\+/g, ' '));
  file = file.replace(/^[\\\/]+([A-Za-z]:)/, '$1');
  if (file.startsWith('file://')) {
    file = file.slice(7);
  }
  file = file.replace(/^[\\\/]+([A-Za-z]:)/, '$1');

  return {
    file,
    line: match[2] || '',
  };
}

const cases = [
  {
    url: 'phpstorm://open?url=file://C:/dev/app/index.php&line=42',
    file: 'C:/dev/app/index.php',
    line: '42',
  },
  {
    url: 'phpstorm://open?file=C:\\dev\\app\\index.php&line=10',
    file: 'C:\\dev\\app\\index.php',
    line: '10',
  },
  {
    url: 'idea://open?url=file:///C:/Users/KAZZ/foo.php&line=1',
    file: 'C:/Users/KAZZ/foo.php',
    line: '1',
  },
  {
    url: 'phpstorm://open/?url=file%3A%2F%2FC%3A%2Ftmp%2Fa.php&line=5',
    file: 'C:/tmp/a.php',
    line: '5',
  },
  {
    url: 'phpstorm://open?file=C:/x.php',
    file: 'C:/x.php',
    line: '',
  },
  {
    url: '\\phpstorm://open?file=C:\\Windows\\win.ini&line=1\\',
    file: 'C:\\Windows\\win.ini',
    line: '1',
  },
];

let failed = 0;
for (const c of cases) {
  const parsed = parseProtocolUrl(c.url);
  if (!parsed) {
    console.error('FAIL parse returned null:', c.url);
    failed++;
    continue;
  }
  if (parsed.file !== c.file || parsed.line !== c.line) {
    console.error('FAIL', c.url);
    console.error('  expected', c);
    console.error('  got     ', parsed);
    failed++;
  }
}

const reg = parseProtocolUrl('phpstorm://open?file=C:\\project\\src\\Main.php&line=3');
if (!reg || reg.file !== 'C:\\project\\src\\Main.php' || reg.line !== '3') {
  console.error('FAIL regression (scheme captured as path?)', reg);
  failed++;
}

if (failed > 0) {
  process.exit(1);
}
console.log(`OK ${cases.length + 1} parse tests`);
