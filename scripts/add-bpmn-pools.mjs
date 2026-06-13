/**
 * Оборачивает процессы в collaboration + participant (pool) для Camunda Modeler.
 */
import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { fixPoolLayout, POOL_LABEL_WIDTH } from './fix-bpmn-pool-layout.mjs';

const processesDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'backend/src/main/resources/processes');
const POOL_NAME = 'MTS Online Shop';

function addPool(xml) {
  if (/<bpmn:collaboration[\s>]/.test(xml)) {
    return fixPoolLayout(xml);
  }

  const processIdMatch = xml.match(/<bpmn:process id="([^"]+)"/);
  if (!processIdMatch) {
    throw new Error('process id not found');
  }

  const processId = processIdMatch[1];
  const suffix = processId.replace(/-/g, '_');
  const collabId = `Collaboration_${suffix}`;
  const participantId = `Participant_${suffix}`;

  const collaborationXml =
    `  <bpmn:collaboration id="${collabId}">\n` +
    `    <bpmn:participant id="${participantId}" name="${POOL_NAME}" processRef="${processId}" />\n` +
    `  </bpmn:collaboration>\n\n`;

  xml = xml.replace(/\n(\s*)<bpmn:process/, `\n${collaborationXml}$1<bpmn:process`);

  const planeMatch = xml.match(/<bpmndi:BPMNPlane id="([^"]+)" bpmnElement="([^"]+)"/);
  if (!planeMatch) {
    throw new Error('BPMNPlane not found');
  }

  xml = xml.replace(
    `<bpmndi:BPMNPlane id="${planeMatch[1]}" bpmnElement="${planeMatch[2]}"`,
    `<bpmndi:BPMNPlane id="${planeMatch[1]}" bpmnElement="${collabId}"`,
  );

  const laneBounds = [...xml.matchAll(
    /<bpmndi:BPMNShape id="Lane_[^"]+_di"[\s\S]*?<dc:Bounds x="(\d+)" y="(\d+)" width="(\d+)" height="(\d+)"/g,
  )].map((m) => ({
    x: Number(m[1]),
    y: Number(m[2]),
    w: Number(m[3]),
    h: Number(m[4]),
  }));

  if (laneBounds.length === 0) {
    throw new Error('lane bounds not found');
  }

  const minX = Math.min(...laneBounds.map((b) => b.x));
  const minY = Math.min(...laneBounds.map((b) => b.y));
  const maxX = Math.max(...laneBounds.map((b) => b.x + b.w));
  const maxY = Math.max(...laneBounds.map((b) => b.y + b.h));

  const poolX = minX - POOL_LABEL_WIDTH;
  const poolY = minY;
  const poolW = maxX - poolX;
  const poolH = maxY - minY;

  const poolShape =
    `      <bpmndi:BPMNShape id="${participantId}_di" bpmnElement="${participantId}" isHorizontal="true">\n` +
    `        <dc:Bounds x="${poolX}" y="${poolY}" width="${poolW}" height="${poolH}" />\n` +
    `        <bpmndi:BPMNLabel />\n` +
    `      </bpmndi:BPMNShape>\n`;

  xml = xml.replace(/(<bpmndi:BPMNPlane[^>]*>\n)/, `$1${poolShape}`);

  return xml;
}

for (const file of readdirSync(processesDir).filter((f) => f.endsWith('.bpmn'))) {
  const path = join(processesDir, file);
  const input = readFileSync(path, 'utf8');
  try {
    const output = addPool(input);
    writeFileSync(path, output, 'utf8');
    console.log(`OK ${file}`);
  } catch (err) {
    console.error(`FAIL ${file}:`, err.message);
    process.exitCode = 1;
  }
}
