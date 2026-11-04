import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const dir = path.dirname(fileURLToPath(import.meta.url));
const r = spawnSync(process.execPath, [path.join(dir, 'parse-protocol-url.mjs')], {
  stdio: 'inherit',
});
process.exit(r.status ?? 1);
