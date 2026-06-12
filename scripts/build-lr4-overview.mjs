/**
 * Обзорная BPMN ЛР4: 5 процессов, дорожки USER/ADMIN/SERVICE, пул BANK.
 * Раскладка по секциям (без наложения несвязанных цепочек).
 */
import { writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { layoutProcess } from 'bpmn-auto-layout';

const outPath = join(dirname(fileURLToPath(import.meta.url)), '..', 'bpmn', 'lr4.bpmn');
const MODELER = 'Camunda Modeler';
const VERSION = '4.12.0';
const LANE_X = 160;
const LANE_H = 160;
const LANE_Y = { USER: 100, ADMIN: 290, SERVICE: 480 };
const SECTION_GAP = 120;

const sections = [
  {
    name: 'user-lk-order',
    x0: 280,
    xml: `
    <bpmn:startEvent id="lk_Start" name="USER: заказ в ЛК"><bpmn:outgoing>F_lk_1</bpmn:outgoing></bpmn:startEvent>
    <bpmn:userTask id="lk_EnterProduct" name="Выбор ID товара"><bpmn:incoming>F_lk_1</bpmn:incoming><bpmn:incoming>F_lk_loop</bpmn:incoming><bpmn:outgoing>F_lk_2</bpmn:outgoing></bpmn:userTask>
    <bpmn:serviceTask id="lk_AddToCart" name="Добавить в корзину"><bpmn:incoming>F_lk_2</bpmn:incoming><bpmn:outgoing>F_lk_3</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:userTask id="lk_AddMore" name="Добавить ещё?"><bpmn:incoming>F_lk_3</bpmn:incoming><bpmn:outgoing>F_lk_4</bpmn:outgoing></bpmn:userTask>
    <bpmn:exclusiveGateway id="lk_GwAddMore" name="Ещё?" default="F_lk_5"><bpmn:incoming>F_lk_4</bpmn:incoming><bpmn:outgoing>F_lk_loop</bpmn:outgoing><bpmn:outgoing>F_lk_5</bpmn:outgoing></bpmn:exclusiveGateway>
    <bpmn:serviceTask id="lk_ValidateCart" name="Проверка корзины"><bpmn:incoming>F_lk_5</bpmn:incoming><bpmn:outgoing>F_lk_6</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="lk_CreateOrder" name="Создание заказа"><bpmn:incoming>F_lk_6</bpmn:incoming><bpmn:outgoing>F_lk_7</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="lk_Reserve" name="Резерв"><bpmn:incoming>F_lk_7</bpmn:incoming><bpmn:outgoing>F_lk_8</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:userTask id="lk_EnterPayment" name="Оплата"><bpmn:incoming>F_lk_8</bpmn:incoming><bpmn:outgoing>F_lk_9</bpmn:outgoing></bpmn:userTask>
    <bpmn:serviceTask id="lk_InitiatePayment" name="Инициация платежа"><bpmn:incoming>F_lk_9</bpmn:incoming><bpmn:outgoing>F_lk_10</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:intermediateCatchEvent id="lk_BankCallback" name="Callback банка"><bpmn:incoming>F_lk_10</bpmn:incoming><bpmn:outgoing>F_lk_11</bpmn:outgoing><bpmn:messageEventDefinition messageRef="Message_BankCallback" /></bpmn:intermediateCatchEvent>
    <bpmn:exclusiveGateway id="lk_GwPayment" name="Оплата OK?" default="F_lk_fail"><bpmn:incoming>F_lk_11</bpmn:incoming><bpmn:outgoing>F_lk_ok</bpmn:outgoing><bpmn:outgoing>F_lk_fail</bpmn:outgoing></bpmn:exclusiveGateway>
    <bpmn:serviceTask id="lk_MarkPaid" name="Подтвердить оплату"><bpmn:incoming>F_lk_ok</bpmn:incoming><bpmn:outgoing>F_lk_12</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="lk_StartServer" name="→ server-order-processing"><bpmn:incoming>F_lk_12</bpmn:incoming><bpmn:outgoing>F_lk_13</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="lk_ReleaseFail" name="Снять резерв"><bpmn:incoming>F_lk_fail</bpmn:incoming><bpmn:outgoing>F_lk_14</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:endEvent id="lk_EndOk" name="Оформлен"><bpmn:incoming>F_lk_13</bpmn:incoming></bpmn:endEvent>
    <bpmn:endEvent id="lk_EndFail" name="Ошибка оплаты"><bpmn:incoming>F_lk_14</bpmn:incoming></bpmn:endEvent>
    <bpmn:sequenceFlow id="F_lk_1" sourceRef="lk_Start" targetRef="lk_EnterProduct" />
    <bpmn:sequenceFlow id="F_lk_2" sourceRef="lk_EnterProduct" targetRef="lk_AddToCart" />
    <bpmn:sequenceFlow id="F_lk_3" sourceRef="lk_AddToCart" targetRef="lk_AddMore" />
    <bpmn:sequenceFlow id="F_lk_4" sourceRef="lk_AddMore" targetRef="lk_GwAddMore" />
    <bpmn:sequenceFlow id="F_lk_loop" sourceRef="lk_GwAddMore" targetRef="lk_EnterProduct" />
    <bpmn:sequenceFlow id="F_lk_5" sourceRef="lk_GwAddMore" targetRef="lk_ValidateCart" />
    <bpmn:sequenceFlow id="F_lk_6" sourceRef="lk_ValidateCart" targetRef="lk_CreateOrder" />
    <bpmn:sequenceFlow id="F_lk_7" sourceRef="lk_CreateOrder" targetRef="lk_Reserve" />
    <bpmn:sequenceFlow id="F_lk_8" sourceRef="lk_Reserve" targetRef="lk_EnterPayment" />
    <bpmn:sequenceFlow id="F_lk_9" sourceRef="lk_EnterPayment" targetRef="lk_InitiatePayment" />
    <bpmn:sequenceFlow id="F_lk_10" sourceRef="lk_InitiatePayment" targetRef="lk_BankCallback" />
    <bpmn:sequenceFlow id="F_lk_11" sourceRef="lk_BankCallback" targetRef="lk_GwPayment" />
    <bpmn:sequenceFlow id="F_lk_ok" sourceRef="lk_GwPayment" targetRef="lk_MarkPaid" />
    <bpmn:sequenceFlow id="F_lk_fail" sourceRef="lk_GwPayment" targetRef="lk_ReleaseFail" />
    <bpmn:sequenceFlow id="F_lk_12" sourceRef="lk_MarkPaid" targetRef="lk_StartServer" />
    <bpmn:sequenceFlow id="F_lk_13" sourceRef="lk_StartServer" targetRef="lk_EndOk" />
    <bpmn:sequenceFlow id="F_lk_14" sourceRef="lk_ReleaseFail" targetRef="lk_EndFail" />`,
    lanes: {
      USER: ['lk_Start', 'lk_EnterProduct', 'lk_AddMore', 'lk_EnterPayment'],
      ADMIN: [],
      SERVICE: ['lk_AddToCart', 'lk_GwAddMore', 'lk_ValidateCart', 'lk_CreateOrder', 'lk_Reserve', 'lk_InitiatePayment', 'lk_BankCallback', 'lk_GwPayment', 'lk_MarkPaid', 'lk_StartServer', 'lk_ReleaseFail', 'lk_EndOk', 'lk_EndFail'],
    },
  },
  {
    name: 'user-order-cancel',
    x0: 0,
    xml: `
    <bpmn:startEvent id="cancel_Start" name="USER: отмена"><bpmn:outgoing>F_c_1</bpmn:outgoing></bpmn:startEvent>
    <bpmn:userTask id="cancel_EnterOrderId" name="Номер заказа"><bpmn:incoming>F_c_1</bpmn:incoming><bpmn:outgoing>F_c_2</bpmn:outgoing></bpmn:userTask>
    <bpmn:serviceTask id="cancel_Validate" name="Проверка"><bpmn:incoming>F_c_2</bpmn:incoming><bpmn:outgoing>F_c_3</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:userTask id="cancel_AdminConfirm" name="Подтверждение (админ)"><bpmn:incoming>F_c_3</bpmn:incoming><bpmn:outgoing>F_c_4</bpmn:outgoing></bpmn:userTask>
    <bpmn:exclusiveGateway id="cancel_GwApproved" name="Одобрено?" default="F_c_rej"><bpmn:incoming>F_c_4</bpmn:incoming><bpmn:outgoing>F_c_app</bpmn:outgoing><bpmn:outgoing>F_c_rej</bpmn:outgoing></bpmn:exclusiveGateway>
    <bpmn:serviceTask id="cancel_Release" name="Снятие резерва"><bpmn:incoming>F_c_app</bpmn:incoming><bpmn:outgoing>F_c_5</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="cancel_CancelOrder" name="Отмена"><bpmn:incoming>F_c_5</bpmn:incoming><bpmn:outgoing>F_c_6</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="cancel_Notify" name="Уведомление"><bpmn:incoming>F_c_6</bpmn:incoming><bpmn:outgoing>F_c_7</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:endEvent id="cancel_EndOk" name="Отменён"><bpmn:incoming>F_c_7</bpmn:incoming></bpmn:endEvent>
    <bpmn:endEvent id="cancel_EndRejected" name="Отклонено"><bpmn:incoming>F_c_rej</bpmn:incoming></bpmn:endEvent>
    <bpmn:sequenceFlow id="F_c_1" sourceRef="cancel_Start" targetRef="cancel_EnterOrderId" />
    <bpmn:sequenceFlow id="F_c_2" sourceRef="cancel_EnterOrderId" targetRef="cancel_Validate" />
    <bpmn:sequenceFlow id="F_c_3" sourceRef="cancel_Validate" targetRef="cancel_AdminConfirm" />
    <bpmn:sequenceFlow id="F_c_4" sourceRef="cancel_AdminConfirm" targetRef="cancel_GwApproved" />
    <bpmn:sequenceFlow id="F_c_app" sourceRef="cancel_GwApproved" targetRef="cancel_Release" />
    <bpmn:sequenceFlow id="F_c_rej" sourceRef="cancel_GwApproved" targetRef="cancel_EndRejected" />
    <bpmn:sequenceFlow id="F_c_5" sourceRef="cancel_Release" targetRef="cancel_CancelOrder" />
    <bpmn:sequenceFlow id="F_c_6" sourceRef="cancel_CancelOrder" targetRef="cancel_Notify" />
    <bpmn:sequenceFlow id="F_c_7" sourceRef="cancel_Notify" targetRef="cancel_EndOk" />`,
    lanes: {
      USER: ['cancel_Start', 'cancel_EnterOrderId'],
      ADMIN: ['cancel_AdminConfirm'],
      SERVICE: ['cancel_Validate', 'cancel_GwApproved', 'cancel_Release', 'cancel_CancelOrder', 'cancel_Notify', 'cancel_EndOk', 'cancel_EndRejected'],
    },
  },
  {
    name: 'admin-product-create',
    x0: 0,
    xml: `
    <bpmn:startEvent id="ac_Start" name="ADMIN: создать"><bpmn:outgoing>F_ac_1</bpmn:outgoing></bpmn:startEvent>
    <bpmn:userTask id="ac_Form" name="Данные товара"><bpmn:incoming>F_ac_1</bpmn:incoming><bpmn:outgoing>F_ac_2</bpmn:outgoing></bpmn:userTask>
    <bpmn:serviceTask id="ac_Service" name="Создать товар"><bpmn:incoming>F_ac_2</bpmn:incoming><bpmn:outgoing>F_ac_3</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:endEvent id="ac_End" name="Создан"><bpmn:incoming>F_ac_3</bpmn:incoming></bpmn:endEvent>
    <bpmn:sequenceFlow id="F_ac_1" sourceRef="ac_Start" targetRef="ac_Form" />
    <bpmn:sequenceFlow id="F_ac_2" sourceRef="ac_Form" targetRef="ac_Service" />
    <bpmn:sequenceFlow id="F_ac_3" sourceRef="ac_Service" targetRef="ac_End" />`,
    lanes: { USER: [], ADMIN: ['ac_Start', 'ac_Form'], SERVICE: ['ac_Service', 'ac_End'] },
  },
  {
    name: 'admin-product-update',
    x0: 0,
    xml: `
    <bpmn:startEvent id="au_Start" name="ADMIN: изменить"><bpmn:outgoing>F_au_1</bpmn:outgoing></bpmn:startEvent>
    <bpmn:userTask id="au_Form" name="Изменение товара"><bpmn:incoming>F_au_1</bpmn:incoming><bpmn:outgoing>F_au_2</bpmn:outgoing></bpmn:userTask>
    <bpmn:serviceTask id="au_Service" name="Обновить"><bpmn:incoming>F_au_2</bpmn:incoming><bpmn:outgoing>F_au_3</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:endEvent id="au_End" name="Обновлён"><bpmn:incoming>F_au_3</bpmn:incoming></bpmn:endEvent>
    <bpmn:sequenceFlow id="F_au_1" sourceRef="au_Start" targetRef="au_Form" />
    <bpmn:sequenceFlow id="F_au_2" sourceRef="au_Form" targetRef="au_Service" />
    <bpmn:sequenceFlow id="F_au_3" sourceRef="au_Service" targetRef="au_End" />`,
    lanes: { USER: [], ADMIN: ['au_Start', 'au_Form'], SERVICE: ['au_Service', 'au_End'] },
  },
  {
    name: 'server-order-processing',
    x0: 0,
    xml: `
    <bpmn:startEvent id="srv_Start" name="SERVER: обработка"><bpmn:outgoing>F_s_1</bpmn:outgoing></bpmn:startEvent>
    <bpmn:serviceTask id="srv_Deduct" name="Списание"><bpmn:incoming>F_s_1</bpmn:incoming><bpmn:outgoing>F_s_2</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="srv_Email" name="Email"><bpmn:incoming>F_s_2</bpmn:incoming><bpmn:outgoing>F_s_3</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="srv_Telegram" name="Telegram"><bpmn:incoming>F_s_3</bpmn:incoming><bpmn:outgoing>F_s_4</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="srv_Bitrix" name="Bitrix"><bpmn:incoming>F_s_4</bpmn:incoming><bpmn:outgoing>F_s_5</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:serviceTask id="srv_Complete" name="COMPLETED"><bpmn:incoming>F_s_5</bpmn:incoming><bpmn:outgoing>F_s_6</bpmn:outgoing></bpmn:serviceTask>
    <bpmn:endEvent id="srv_End" name="Готово"><bpmn:incoming>F_s_6</bpmn:incoming></bpmn:endEvent>
    <bpmn:sequenceFlow id="F_s_1" sourceRef="srv_Start" targetRef="srv_Deduct" />
    <bpmn:sequenceFlow id="F_s_2" sourceRef="srv_Deduct" targetRef="srv_Email" />
    <bpmn:sequenceFlow id="F_s_3" sourceRef="srv_Email" targetRef="srv_Telegram" />
    <bpmn:sequenceFlow id="F_s_4" sourceRef="srv_Telegram" targetRef="srv_Bitrix" />
    <bpmn:sequenceFlow id="F_s_5" sourceRef="srv_Bitrix" targetRef="srv_Complete" />
    <bpmn:sequenceFlow id="F_s_6" sourceRef="srv_Complete" targetRef="srv_End" />`,
    lanes: { USER: [], ADMIN: [], SERVICE: ['srv_Start', 'srv_Deduct', 'srv_Email', 'srv_Telegram', 'srv_Bitrix', 'srv_Complete', 'srv_End'] },
  },
];

const laneOf = new Map();
for (const sec of sections) {
  for (const [lane, ids] of Object.entries(sec.lanes)) ids.forEach((id) => laneOf.set(id, lane));
}

function wrapProcess(body) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" id="D" targetNamespace="http://mts-online-shop/bpmn">
  <bpmn:process id="p" isExecutable="false">${body}</bpmn:process>
</bpmn:definitions>`;
}

function parseLayout(xml) {
  const shapes = new Map();
  const re = /<bpmndi:BPMNShape[^>]*bpmnElement="([^"]+)"[^>]*>[\s\S]*?<dc:Bounds x="(\d+)" y="(\d+)" width="(\d+)" height="(\d+)"/g;
  let m;
  while ((m = re.exec(xml)) !== null) {
    shapes.set(m[1], { x: +m[2], y: +m[3], w: +m[4], h: +m[5], gateway: /Gateway|Gw/.test(m[1]), event: /Start|End|Callback/.test(m[1]) });
  }
  return shapes;
}

function parseFlows(xml) {
  const flows = [];
  const re = /<bpmn:sequenceFlow id="([^"]+)" sourceRef="([^"]+)" targetRef="([^"]+)"/g;
  let m;
  while ((m = re.exec(xml)) !== null) flows.push({ id: m[1], source: m[2], target: m[3] });
  return flows;
}

function placeInLane(shape, lane) {
  const cy = LANE_Y[lane] + LANE_H / 2;
  let y;
  if (shape.gateway) y = cy - 25;
  else if (shape.event) y = cy - 18;
  else y = cy - shape.h / 2;
  return { ...shape, y: Math.round(y) };
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
  if (Math.abs(tc.y - sc.y) < 12) {
    return [{ x: Math.round(sc.x), y: Math.round(sc.y) }, { x: Math.round(tc.x), y: Math.round(tc.y) }];
  }
  const midX = Math.round((sc.x + tc.x) / 2);
  return [
    { x: Math.round(sc.x), y: Math.round(sc.y) },
    { x: midX, y: Math.round(sc.y) },
    { x: midX, y: Math.round(tc.y) },
    { x: Math.round(tc.x), y: Math.round(tc.y) },
  ];
}

function overlaps(a, b) {
  const pad = 8;
  return a.x < b.x + b.w + pad && a.x + a.w + pad > b.x && a.y < b.y + b.h + pad && a.y + a.h + pad > b.y;
}

/** Развести исходящие ветки exclusive gateway по вертикали внутри дорожки */
function spreadGatewayBranches(bounds, flows) {
  const outgoing = new Map();
  for (const f of flows) {
    if (!outgoing.has(f.source)) outgoing.set(f.source, []);
    outgoing.get(f.source).push(f);
  }
  for (const [id, b] of bounds) {
    if (!b.gateway) continue;
    const outs = outgoing.get(id) || [];
    if (outs.length < 2) continue;
    const lane = laneOf.get(id) || 'SERVICE';
    const cy = LANE_Y[lane] + LANE_H / 2;
    const spread = lane === 'SERVICE' ? 55 : 45;
    const targets = outs.map((f) => f.target).filter((t) => bounds.has(t));
    targets.forEach((tid, i) => {
      const tb = bounds.get(tid);
      const offset = (i - (targets.length - 1) / 2) * spread;
      const newCy = cy + offset;
      let y;
      if (tb.gateway) y = newCy - 25;
      else if (tb.event) y = newCy - 18;
      else y = newCy - tb.h / 2;
      bounds.set(tid, { ...tb, y: Math.round(y) });
      // сдвинуть цепочку после ветки, если узлы накладываются
      shiftDownstream(tid, bounds, flows, 0);
    });
  }
}

function shiftDownstream(nodeId, bounds, flows, depth) {
  if (depth > 24) return;
  const outs = flows.filter((f) => f.source === nodeId);
  for (const f of outs) {
    const child = bounds.get(f.target);
    if (!child) continue;
    const parent = bounds.get(nodeId);
    if (overlaps(parent, child)) {
      const dy = parent.y + parent.h + 12 - child.y;
      bounds.set(f.target, { ...child, y: child.y + dy });
      shiftDownstream(f.target, bounds, flows, depth + 1);
    }
  }
}

function resolveRemainingOverlaps(bounds) {
  const ids = [...bounds.keys()];
  for (let pass = 0; pass < 6; pass++) {
    let moved = false;
    for (let i = 0; i < ids.length; i++) {
      for (let j = i + 1; j < ids.length; j++) {
        const a = bounds.get(ids[i]);
        const b = bounds.get(ids[j]);
        if (!overlaps(a, b)) continue;
        bounds.set(ids[j], { ...b, y: b.y + 70 });
        moved = true;
      }
    }
    if (!moved) break;
  }
}

const allBounds = new Map();
const allFlows = [];
let cursorX = 280;

for (const sec of sections) {
  const laidOut = await layoutProcess(wrapProcess(sec.xml));
  const shapes = parseLayout(laidOut);
  const flows = parseFlows(sec.xml);
  const minX = Math.min(...[...shapes.values()].map((s) => s.x));
  const maxX = Math.max(...[...shapes.values()].map((s) => s.x + s.w));
  const xOff = cursorX - minX;
  for (const [id, shape] of shapes) {
    const lane = laneOf.get(id) || 'SERVICE';
    allBounds.set(id, placeInLane({ ...shape, x: shape.x + xOff }, lane));
  }
  allFlows.push(...flows);
  cursorX = maxX + xOff + SECTION_GAP;
}

// link lk_StartServer -> srv_Start
allFlows.push({ id: 'F_lk_to_srv', source: 'lk_StartServer', target: 'srv_Start' });

spreadGatewayBranches(allBounds, allFlows);
resolveRemainingOverlaps(allBounds);

const maxRight = Math.max(...[...allBounds.values()].map((b) => b.x + b.w));
const laneW = maxRight - LANE_X + 160;
const poolH = LANE_Y.SERVICE + LANE_H + 60;

const processBody = sections.map((s) => s.xml).join('\n') + `
    <bpmn:sequenceFlow id="F_lk_to_srv" sourceRef="lk_StartServer" targetRef="srv_Start" />`;

const laneRefs = { USER: [], ADMIN: [], SERVICE: [] };
for (const [id, lane] of laneOf) laneRefs[lane].push(id);

const laneSetXml = ['USER', 'ADMIN', 'SERVICE']
  .filter((l) => laneRefs[l].length)
  .map((l) => {
    const laneId = `Lane_${l.charAt(0) + l.slice(1).toLowerCase()}`;
    const refs = laneRefs[l].map((id) => `        <bpmn:flowNodeRef>${id}</bpmn:flowNodeRef>`).join('\n');
    return `      <bpmn:lane id="${laneId}" name="${l}">\n${refs}\n      </bpmn:lane>`;
  })
  .join('\n');

const bankX = LANE_X + laneW + 80;

const xml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  id="Definitions_Lr4Overview"
  targetNamespace="http://mts-online-shop/bpmn/lr4-overview"
  exporter="${MODELER}" exporterVersion="${VERSION}">

  <bpmn:message id="Message_BankCallback" name="bank_payment_callback" />

  <bpmn:collaboration id="Collaboration_Lr4">
    <bpmn:participant id="Participant_Shop" name="MTS Online Shop — ЛР4 (5 процессов)" processRef="lr4-overview" />
    <bpmn:participant id="Participant_Bank" name="BANK" processRef="lr4-bank" />
    <bpmn:messageFlow id="MF_PaymentToBank" sourceRef="lk_InitiatePayment" targetRef="bank_Process" />
    <bpmn:messageFlow id="MF_BankCallback" sourceRef="bank_Callback" targetRef="lk_BankCallback" />
    <bpmn:textAnnotation id="Ann_Api">
      <bpmn:text>Триггеры API:
POST /api/cart/items · POST /api/orders/create · POST /api/orders/{id}/cancel
POST /api/admin/products · PUT /api/admin/products/{id}
POST /api/internal/bank/payment-callback</bpmn:text>
    </bpmn:textAnnotation>
    <bpmn:association id="Assoc_Api" sourceRef="Ann_Api" targetRef="lk_Start" />
  </bpmn:collaboration>

  <bpmn:process id="lr4-bank" isExecutable="false">
    <bpmn:startEvent id="bank_Start" name="Запрос"><bpmn:outgoing>F_b1</bpmn:outgoing></bpmn:startEvent>
    <bpmn:task id="bank_Process" name="Обработка платежа"><bpmn:incoming>F_b1</bpmn:incoming><bpmn:outgoing>F_b2</bpmn:outgoing></bpmn:task>
    <bpmn:intermediateThrowEvent id="bank_Callback" name="Callback"><bpmn:incoming>F_b2</bpmn:incoming><bpmn:messageEventDefinition messageRef="Message_BankCallback" /></bpmn:intermediateThrowEvent>
    <bpmn:sequenceFlow id="F_b1" sourceRef="bank_Start" targetRef="bank_Process" />
    <bpmn:sequenceFlow id="F_b2" sourceRef="bank_Process" targetRef="bank_Callback" />
  </bpmn:process>

  <bpmn:process id="lr4-overview" name="МТС Online Shop — обзор ЛР4" isExecutable="false">
    <bpmn:laneSet id="LaneSet_Lr4">
${laneSetXml}
    </bpmn:laneSet>
${processBody}
  </bpmn:process>

  <bpmndi:BPMNDiagram id="BPMNDiagram_Lr4">
    <bpmndi:BPMNPlane id="BPMNPlane_Lr4" bpmnElement="Collaboration_Lr4">
      <bpmndi:BPMNShape id="Participant_Shop_di" bpmnElement="Participant_Shop" isHorizontal="false">
        <dc:Bounds x="${LANE_X - 20}" y="60" width="${laneW + 40}" height="${poolH}" />
      </bpmndi:BPMNShape>
${['SERVICE', 'ADMIN', 'USER'].map((lane) => {
  const laneId = `Lane_${lane.charAt(0) + lane.slice(1).toLowerCase()}`;
  return `      <bpmndi:BPMNShape id="${laneId}_di" bpmnElement="${laneId}" isHorizontal="true">
        <dc:Bounds x="${LANE_X}" y="${LANE_Y[lane]}" width="${laneW}" height="${LANE_H}" />
        <bpmndi:BPMNLabel><dc:Bounds x="${LANE_X + 12}" y="${LANE_Y[lane] + 12}" width="60" height="14" /></bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>`;
}).join('\n')}
${allFlows.map((f) => {
  const pts = routeEdge(f.source, f.target, allBounds).map((p) => `<di:waypoint x="${p.x}" y="${p.y}" />`).join('\n        ');
  return `      <bpmndi:BPMNEdge id="${f.id}_di" bpmnElement="${f.id}">\n        ${pts}\n      </bpmndi:BPMNEdge>`;
}).join('\n')}
${[...allBounds.entries()].map(([id, b]) => {
  const gw = b.gateway ? ' isMarkerVisible="true"' : '';
  return `      <bpmndi:BPMNShape id="${id}_di" bpmnElement="${id}"${gw}>\n        <dc:Bounds x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" />\n      </bpmndi:BPMNShape>`;
}).join('\n')}
      <bpmndi:BPMNShape id="Participant_Bank_di" bpmnElement="Participant_Bank" isHorizontal="false">
        <dc:Bounds x="${bankX}" y="${LANE_Y.SERVICE}" width="260" height="180" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="bank_Start_di" bpmnElement="bank_Start"><dc:Bounds x="${bankX + 30}" y="${LANE_Y.SERVICE + 50}" width="36" height="36" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="bank_Process_di" bpmnElement="bank_Process"><dc:Bounds x="${bankX + 90}" y="${LANE_Y.SERVICE + 38}" width="100" height="60" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="bank_Callback_di" bpmnElement="bank_Callback"><dc:Bounds x="${bankX + 210}" y="${LANE_Y.SERVICE + 50}" width="36" height="36" /></bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="F_b1_di" bpmnElement="F_b1"><di:waypoint x="${bankX + 66}" y="${LANE_Y.SERVICE + 68}" /><di:waypoint x="${bankX + 90}" y="${LANE_Y.SERVICE + 68}" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="F_b2_di" bpmnElement="F_b2"><di:waypoint x="${bankX + 190}" y="${LANE_Y.SERVICE + 68}" /><di:waypoint x="${bankX + 210}" y="${LANE_Y.SERVICE + 68}" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNShape id="Ann_Api_di" bpmnElement="Ann_Api"><dc:Bounds x="${LANE_X}" y="8" width="380" height="72" /></bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Assoc_Api_di" bpmnElement="Assoc_Api"><di:waypoint x="${LANE_X + 190}" y="${80}" /><di:waypoint x="${allBounds.get('lk_Start').x + 18}" y="${LANE_Y.USER + 18}" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="MF_PaymentToBank_di" bpmnElement="MF_PaymentToBank">
        <di:waypoint x="${allBounds.get('lk_InitiatePayment').x + allBounds.get('lk_InitiatePayment').w}" y="${allBounds.get('lk_InitiatePayment').y + 40}" />
        <di:waypoint x="${bankX + 140}" y="${LANE_Y.SERVICE + 68}" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="MF_BankCallback_di" bpmnElement="MF_BankCallback">
        <di:waypoint x="${bankX + 228}" y="${LANE_Y.SERVICE + 68}" />
        <di:waypoint x="${allBounds.get('lk_BankCallback').x}" y="${allBounds.get('lk_BankCallback').y + 18}" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

writeFileSync(outPath, xml, 'utf8');
console.log('Written', outPath, `(${allBounds.size} nodes, width ~${laneW}px)`);
