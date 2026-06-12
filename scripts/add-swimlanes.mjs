/**
 * Swimlanes для Camunda Modeler 4.12:
 * - laneSet в модели (USER / ADMIN / SERVICE)
 * - bpmn-auto-layout для горизонтальной раскладки узлов и рёбер
 * - DI: дорожки, рёбра, фигуры — siblings на BPMNPlane (как в user-order-cancel)
 * - вложенные BPMNShape внутри lane НЕ допускаются
 */
import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { layoutProcess } from 'bpmn-auto-layout';

const processesDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'backend/src/main/resources/processes');
const MODELER_EXPORTER = 'Camunda Modeler';
const MODELER_VERSION = '4.12.0';
const SKIP_FILES = new Set(['user-order-cancel.bpmn', 'admin-product-create.bpmn', 'user-lk-order.bpmn']);

const LANE_X = 160;
const LANE_H = 150;
const LANE_Y_SLOTS = [80, 250, 420];
const LANE_SEMANTIC_ORDER = ['USER', 'ADMIN', 'SERVICE'];
const LANE_DI_ORDER = ['SERVICE', 'ADMIN', 'USER'];

function buildLaneYMap(lanes) {
  const active = LANE_SEMANTIC_ORDER.filter((lane) => lanes[lane].length > 0);
  const map = {};
  active.forEach((lane, index) => {
    map[lane] = LANE_Y_SLOTS[index];
  });
  return map;
}

function deriveLanes(xml, processId) {
  const lanes = { USER: [], ADMIN: [], SERVICE: [] };
  const laneOf = new Map();
  const put = (lane, id) => {
    if (laneOf.has(id)) return;
    lanes[lane].push(id);
    laneOf.set(id, lane);
  };

  const isAdminProcess = processId.startsWith('admin-');
  const isServerProcess = processId.startsWith('server-');

  for (const m of xml.matchAll(/<bpmn:userTask id="([^"]+)"[^>]*camunda:candidateGroups="([^"]+)"/g)) {
    put(m[2] === 'admin' ? 'ADMIN' : 'USER', m[1]);
  }
  for (const m of xml.matchAll(/<bpmn:serviceTask id="([^"]+)"/g)) put('SERVICE', m[1]);
  for (const m of xml.matchAll(/<bpmn:exclusiveGateway id="([^"]+)"/g)) put('SERVICE', m[1]);
  for (const m of xml.matchAll(/<bpmn:intermediateCatchEvent id="([^"]+)"/g)) put('SERVICE', m[1]);
  for (const m of xml.matchAll(/<bpmn:startEvent id="([^"]+)"/g)) {
    if (isAdminProcess) put('ADMIN', m[1]);
    else if (isServerProcess) put('SERVICE', m[1]);
    else put('USER', m[1]);
  }
  for (const m of xml.matchAll(/<bpmn:endEvent id="([^"]+)"/g)) {
    put(m[1] === 'End_Rejected' ? 'USER' : 'SERVICE', m[1]);
  }
  return { lanes, laneOf };
}

function buildLaneSet(processId, lanes) {
  const defs = [
    ['Lane_User', 'USER', lanes.USER],
    ['Lane_Admin', 'ADMIN', lanes.ADMIN],
    ['Lane_Service', 'SERVICE', lanes.SERVICE],
  ].filter(([, , nodes]) => nodes.length > 0);

  const body = defs
    .map(([laneId, name, nodes]) => {
      const refs = nodes.map((n) => `      <bpmn:flowNodeRef>${n}</bpmn:flowNodeRef>`).join('\n');
      return `    <bpmn:lane id="${laneId}" name="${name}">\n${refs}\n    </bpmn:lane>`;
    })
    .join('\n');
  return `    <bpmn:laneSet id="LaneSet_${processId}">\n${body}\n    </bpmn:laneSet>\n`;
}

function stripDiagramAndLanes(xml) {
  let result = xml.replace(/<bpmn:collaboration[\s\S]*?<\/bpmn:collaboration>\s*/m, '');
  result = result.replace(/<bpmn:laneSet[\s\S]*?<\/bpmn:laneSet>\s*/m, '');
  result = result.replace(/<bpmndi:BPMNDiagram[\s\S]*?<\/bpmndi:BPMNDiagram>\s*/m, '');
  return result;
}

function ensureModelerExporter(xml) {
  const clean = xml.replace(/\s*exporter="[^"]*"/, '').replace(/\s*exporterVersion="[^"]*"/, '');
  return clean.replace(
    /<bpmn:definitions([^>]*)>/,
    `<bpmn:definitions$1 exporter="${MODELER_EXPORTER}" exporterVersion="${MODELER_VERSION}">`,
  );
}

function parseBoundaryHosts(xml) {
  const hosts = new Map();
  for (const m of xml.matchAll(/<bpmn:boundaryEvent id="([^"]+)" attachedToRef="([^"]+)"/g)) {
    hosts.set(m[1], m[2]);
  }
  return hosts;
}

function parseLayout(xml) {
  const shapes = new Map();
  const shapeRe =
    /<bpmndi:BPMNShape id="([^"]+)" bpmnElement="([^"]+)"([^>]*)>[\s\S]*?<dc:Bounds x="(\d+)" y="(\d+)" width="(\d+)" height="(\d+)"/g;
  let m;
  while ((m = shapeRe.exec(xml)) !== null) {
    if (m[2].startsWith('Lane_')) continue;
    shapes.set(m[2], {
      x: +m[4],
      y: +m[5],
      w: +m[6],
      h: +m[7],
      gateway: m[3].includes('isMarkerVisible="true"'),
      boundary: m[2].startsWith('Boundary_'),
      event: /^Start_|^End_|^Event_/.test(m[2]),
    });
  }

  const edges = new Map();
  const edgeRe = /<bpmndi:BPMNEdge id="([^"]+)" bpmnElement="([^"]+)">([\s\S]*?)<\/bpmndi:BPMNEdge>/g;
  while ((m = edgeRe.exec(xml)) !== null) {
    const waypoints = [...m[3].matchAll(/<di:waypoint x="(\d+)" y="(\d+)"/g)].map((wp) => ({
      x: +wp[1],
      y: +wp[2],
    }));
    edges.set(m[2], waypoints);
  }
  return { shapes, edges };
}

function laneFor(id, lanes) {
  if (lanes.USER.includes(id)) return 'USER';
  if (lanes.ADMIN.includes(id)) return 'ADMIN';
  return 'SERVICE';
}

function laneCenterY(lane, laneYMap) {
  return laneYMap[lane] + LANE_H / 2;
}

function placeInLane(shape, lane, laneYMap) {
  const cy = laneCenterY(lane, laneYMap);
  let y;
  if (shape.gateway) y = cy - 25;
  else if (shape.event) y = cy - 18;
  else y = cy - shape.h / 2;
  return { ...shape, y: Math.round(y) };
}

function placeBoundary(shape, host) {
  return {
    ...shape,
    x: Math.round(host.x + host.w / 2 - shape.w / 2),
    y: Math.round(host.y + host.h - shape.h / 2),
  };
}

function shapeCenter(b) {
  return { x: b.x + b.w / 2, y: b.y + b.h / 2 };
}

function routeEdge(source, target, boundsMap) {
  const sb = boundsMap.get(source);
  const tb = boundsMap.get(target);
  if (!sb || !tb) return [];
  const sc = shapeCenter(sb);
  const tc = shapeCenter(tb);
  const dy = Math.abs(tc.y - sc.y);
  if (dy < 12) {
    return [
      { x: Math.round(sc.x), y: Math.round(sc.y) },
      { x: Math.round(tc.x), y: Math.round(tc.y) },
    ];
  }
  const midX = Math.round((sc.x + tc.x) / 2);
  return [
    { x: Math.round(sc.x), y: Math.round(sc.y) },
    { x: midX, y: Math.round(sc.y) },
    { x: midX, y: Math.round(tc.y) },
    { x: Math.round(tc.x), y: Math.round(tc.y) },
  ];
}

function parseFlows(xml) {
  const flows = new Map();
  const re = /<bpmn:sequenceFlow id="([^"]+)" sourceRef="([^"]+)" targetRef="([^"]+)"/g;
  let m;
  while ((m = re.exec(xml)) !== null) flows.set(m[1], { source: m[2], target: m[3] });
  return flows;
}

function collectDownstream(startId, flows) {
  const seen = new Set();
  const queue = [startId];
  while (queue.length > 0) {
    const id = queue.shift();
    if (seen.has(id)) continue;
    seen.add(id);
    for (const flow of flows.values()) {
      if (flow.source === id) queue.push(flow.target);
    }
  }
  return seen;
}

function boxesOverlap(a, b) {
  return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
}

function resolveOverlaps(boundsMap, flows) {
  const failRoots = ['Flow_Fail', 'Flow_Rejected'];
  const failIds = new Set();
  for (const flowId of failRoots) {
    const flow = flows.get(flowId);
    if (!flow) continue;
    failIds.add(flow.target);
    for (const id of collectDownstream(flow.target, flows)) failIds.add(id);
  }

  for (const failId of failIds) {
    const node = boundsMap.get(failId);
    if (!node) continue;
    const gatewayFlow = flows.get('Flow_Fail');
    const gateway = gatewayFlow ? boundsMap.get(gatewayFlow.source) : null;
    if (gateway && failId !== gatewayFlow.source) {
      boundsMap.set(failId, {
        ...node,
        x: gateway.x,
        y: gateway.y + gateway.h + 40,
      });
    }
  }

  const ids = [...boundsMap.keys()];
  for (let i = 0; i < ids.length; i++) {
    for (let j = i + 1; j < ids.length; j++) {
      const a = boundsMap.get(ids[i]);
      const b = boundsMap.get(ids[j]);
      if (!boxesOverlap(a, b)) continue;
      const shiftId = failIds.has(ids[j]) ? ids[j] : failIds.has(ids[i]) ? ids[i] : ids[j];
      const current = boundsMap.get(shiftId);
      boundsMap.set(shiftId, { ...current, x: current.x + 180, y: current.y + 100 });
    }
  }
}

function buildModelerDiagram(processId, lanes, layoutShapes, flows, boundaryHosts) {
  const laneYMap = buildLaneYMap(lanes);
  const minX = Math.min(...[...layoutShapes.values()].map((s) => s.x));
  const xShift = Math.max(0, LANE_X + 80 - minX);

  const boundsMap = new Map();
  for (const [id, shape] of layoutShapes) {
    if (shape.boundary) continue;
    const lane = laneFor(id, lanes);
    boundsMap.set(id, placeInLane({ ...shape, x: shape.x + xShift }, lane, laneYMap));
  }
  for (const [id, shape] of layoutShapes) {
    if (!shape.boundary) continue;
    const hostId = boundaryHosts.get(id);
    const host = boundsMap.get(hostId);
    if (!host) continue;
    boundsMap.set(id, placeBoundary({ ...shape, x: shape.x + xShift }, host));
  }

  resolveOverlaps(boundsMap, flows);

  const maxRight = Math.max(...[...boundsMap.values()].map((b) => b.x + b.w));
  const laneW = Math.max(800, maxRight - LANE_X + 120);

  const activeLanes = LANE_DI_ORDER.filter((lane) => lanes[lane].length > 0);
  const laneShapes = activeLanes.map((lane) => {
    const laneId = `Lane_${lane.charAt(0) + lane.slice(1).toLowerCase()}`;
    const laneTop = laneYMap[lane];
    return `      <bpmndi:BPMNShape id="${laneId}_di" bpmnElement="${laneId}" isHorizontal="true">
        <dc:Bounds x="${LANE_X}" y="${laneTop}" width="${laneW}" height="${LANE_H}" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="${LANE_X + 12}" y="${laneTop + 12}" width="60" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>`;
  });

  const edges = [...flows.entries()].map(([flowId, flow]) => {
    const pts = routeEdge(flow.source, flow.target, boundsMap)
      .map((p) => `<di:waypoint x="${p.x}" y="${p.y}" />`)
      .join('\n        ');
    return `      <bpmndi:BPMNEdge id="${flowId}_di" bpmnElement="${flowId}">
        ${pts}
      </bpmndi:BPMNEdge>`;
  });

  const nodeShapes = [...boundsMap.entries()].map(([id, b]) => {
    const gw = b.gateway ? ' isMarkerVisible="true"' : '';
    let extra = '';
    if (id === 'End_Rejected' || id === 'End_Cancelled' || id === 'End_Failed') {
      extra = `
        <bpmndi:BPMNLabel>
          <dc:Bounds x="${b.x - 8}" y="${b.y + 36}" width="75" height="14" />
        </bpmndi:BPMNLabel>`;
    }
    return `      <bpmndi:BPMNShape id="${id}_di" bpmnElement="${id}"${gw}>
        <dc:Bounds x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" />${extra}
      </bpmndi:BPMNShape>`;
  });

  // Порядок как в user-order-cancel (Modeler 4.12): lanes → edges → shapes
  return `  <bpmndi:BPMNDiagram id="BPMNDiagram_${processId}">
    <bpmndi:BPMNPlane id="BPMNPlane_${processId}" bpmnElement="${processId}">
${laneShapes.join('\n')}
${edges.join('\n')}
${nodeShapes.join('\n')}
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>`;
}

async function processFile(file) {
  if (SKIP_FILES.has(file)) {
    console.log(`SKIP ${file}`);
    return;
  }
  const path = join(processesDir, file);
  let xml = readFileSync(path, 'utf8');
  const processId = xml.match(/<bpmn:process id="([^"]+)"/)[1];
  const { lanes } = deriveLanes(xml, processId);
  const flows = parseFlows(xml);
  const boundaryHosts = parseBoundaryHosts(xml);

  xml = stripDiagramAndLanes(xml);
  xml = xml.replace(/(<bpmn:process[^>]*>\s*)/, `$1${buildLaneSet(processId, lanes)}`);
  xml = ensureModelerExporter(xml);

  let laidOut;
  try {
    laidOut = await layoutProcess(xml);
  } catch (err) {
    console.error(`FAIL layout ${file}: ${err.message}`);
    process.exitCode = 1;
    return;
  }

  const { shapes: layoutShapes } = parseLayout(laidOut);
  const diagram = buildModelerDiagram(processId, lanes, layoutShapes, flows, boundaryHosts);
  const withoutDiagram = laidOut
    .replace(/<bpmndi:BPMNDiagram[\s\S]*?<\/bpmndi:BPMNDiagram>\s*/m, '')
    .replace(/<\/bpmn:definitions>\s*$/m, '');
  writeFileSync(path, `${withoutDiagram.trimEnd()}\n${diagram}\n</bpmn:definitions>\n`, 'utf8');
  console.log(`OK ${file}`);
}

for (const file of readdirSync(processesDir).filter((f) => f.endsWith('.bpmn'))) {
  await processFile(file);
}
