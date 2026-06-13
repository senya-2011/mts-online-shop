import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { layoutProcess } from 'bpmn-auto-layout';
import { fixPoolLayout } from './fix-bpmn-pool-layout.mjs';

const processesDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'backend/src/main/resources/processes');
const files = readdirSync(processesDir).filter((f) => f.endsWith('.bpmn'));

for (const file of files) {
  const inputPath = join(processesDir, file);
  const xml = readFileSync(inputPath, 'utf8');
  try {
    if (xml.includes('<bpmn:collaboration')) {
      writeFileSync(inputPath, fixPoolLayout(xml), 'utf8');
      console.log(`FIX ${file}`);
      continue;
    }
    const laidOut = await layoutProcess(xml);
    writeFileSync(inputPath, fixPoolLayout(laidOut), 'utf8');
    console.log(`OK ${file}`);
  } catch (err) {
    console.error(`FAIL ${file}:`, err.message);
    process.exitCode = 1;
  }
}
