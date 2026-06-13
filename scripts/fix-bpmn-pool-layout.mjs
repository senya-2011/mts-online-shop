/**
 * Пул по соглашению Camunda Modeler:
 * - plane.bpmnElement = collaboration (не process)
 * - participant isHorizontal=true, подпись в левой полоске (30px)
 * - lanes.x = pool.x + 30, pool оборачивает lanes и все элементы
 */
import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const processesDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'backend/src/main/resources/processes');
export const POOL_LABEL_WIDTH = 30;
const POOL_PAD = 20;

function parseBounds(xml) {
  return [...xml.matchAll(
    /<bpmndi:BPMNShape id="([^"]+)" bpmnElement="([^"]+)"[^>]*>[\s\S]*?<(?:dc|omgdc):Bounds x="(-?\d+)" y="(-?\d+)" width="(-?\d+)" height="(-?\d+)"/g,
  )].map((m) => ({
    id: m[1],
    element: m[2],
    x: Number(m[3]),
    y: Number(m[4]),
    w: Number(m[5]),
    h: Number(m[6]),
  }));
}

function fixLaneLabels(xml) {
  return xml.replace(
    /(<bpmndi:BPMNShape id="Lane_[^"]+_di" bpmnElement="[^"]+" isHorizontal="true">[\s\S]*?<(?:dc|omgdc):Bounds x="(\d+)" y="(\d+)" width="\d+" height="\d+" \/>)[\s\S]*?<\/bpmndi:BPMNShape>/g,
    (block, prefix, x, y) => `${prefix}\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>`,
  );
}

function ensureCollaborationPlane(xml) {
  const collabId = xml.match(/<bpmn:collaboration id="([^"]+)"/)?.[1];
  if (!collabId) {
    return xml;
  }
  return xml.replace(
    /(<bpmndi:BPMNPlane id="[^"]+" bpmnElement=")[^"]+(")/,
    `$1${collabId}$2`,
  );
}

function buildParticipantShape(participantId, poolX, poolY, poolW, poolH) {
  return `<bpmndi:BPMNShape id="${participantId}_di" bpmnElement="${participantId}" isHorizontal="true">
        <dc:Bounds x="${poolX}" y="${poolY}" width="${poolW}" height="${poolH}" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>`;
}

export function fixPoolLayout(xml) {
  if (!xml.includes('<bpmn:collaboration')) {
    return xml;
  }

  xml = ensureCollaborationPlane(xml);

  const participantId = xml.match(/<bpmn:participant id="([^"]+)"[^>]*processRef=/)?.[1];
  if (!participantId) {
    return xml;
  }

  const shapes = parseBounds(xml);
  const laneShapes = shapes.filter((s) => s.element.startsWith('Lane_'));
  const contentShapes = shapes.filter(
    (s) => !s.element.startsWith('Participant_') && !s.element.startsWith('Lane_'),
  );

  const laneBounds = laneShapes.map((s) => ({ x: s.x, y: s.y, w: s.w, h: s.h }));
  if (laneBounds.length === 0) {
    return xml;
  }

  const laneMinX = Math.min(...laneBounds.map((b) => b.x));
  const laneMinY = Math.min(...laneBounds.map((b) => b.y));
  const laneMaxX = Math.max(...laneBounds.map((b) => b.x + b.w));
  const laneMaxY = Math.max(...laneBounds.map((b) => b.y + b.h));

  const contentMaxX = contentShapes.length
    ? Math.max(...contentShapes.map((s) => s.x + s.w))
    : laneMaxX;
  const contentMaxY = contentShapes.length
    ? Math.max(...contentShapes.map((s) => s.y + s.h))
    : laneMaxY;

  const poolX = laneMinX - POOL_LABEL_WIDTH;
  const poolY = laneMinY;
  const poolW = Math.max(laneMaxX, contentMaxX) - poolX + POOL_PAD;
  const poolH = Math.max(laneMaxY, contentMaxY) - poolY + POOL_PAD;

  const participantShape = buildParticipantShape(participantId, poolX, poolY, poolW, poolH);

  if (xml.includes(`${participantId}_di`)) {
    xml = xml.replace(
      /<bpmndi:BPMNShape id="Participant_[^"]+_di" bpmnElement="[^"]+" isHorizontal="(?:true|false)">[\s\S]*?<\/bpmndi:BPMNShape>/,
      participantShape,
    );
  } else {
    xml = xml.replace(/(<bpmndi:BPMNPlane[^>]*>\n)/, `$1      ${participantShape}\n`);
  }

  return fixLaneLabels(xml);
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  for (const file of readdirSync(processesDir).filter((f) => f.endsWith('.bpmn'))) {
    const path = join(processesDir, file);
    const input = readFileSync(path, 'utf8');
    if (!input.includes('<bpmn:collaboration')) {
      continue;
    }
    writeFileSync(path, fixPoolLayout(input), 'utf8');
    console.log(`OK ${file}`);
  }
}
