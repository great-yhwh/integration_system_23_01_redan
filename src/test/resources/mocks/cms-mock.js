/**
 * Мок-сервер системы CMS
 *
 * GET /cms/spares?page=0&size=10
 *   page : номер страницы (начинается с 0, по умолчанию 0)
 *   size : элементов на странице (по умолчанию 10, максимум 10)
 *
 * BasicAuth
 *   логин: admin
 *   пароль: admin
 *
 * Ответ — просто массив.
 * Клиент должен итерировать страницы, пока не получит [].
 *
 * @endpoint GET /cms/spares
 */
const http = require("http");
const PORT = process.env.PORT || 4500;
const JSON_HEADER = { "Content-Type": "application/json" };

// ────────────────────────────── Настройка BasicAuth ──────────────────────────────────────
const AUTH_LOGIN    = "admin";
const AUTH_PASSWORD = "admin";
const EXPECTED_TOKEN = Buffer.from(`${AUTH_LOGIN}:${AUTH_PASSWORD}`).toString("base64");

function checkBasicAuth(req) {
    const header = req.headers["authorization"] || "";
    const match = header.match(/^Basic\s+(.+)$/i);
    if (!match) return false;
    return match[1] === EXPECTED_TOKEN;
}

// ──────────────────────────── Генерация каталога ────────────────────────
const SPARE_TYPES    = ["ENGINE", "RADIATOR", "BATTERY", "GEARBOX", "BRAKE", "FILTER", "SUSPENSION", "ELECTRIC", "BODY"];
const SPARE_STATUSES = ["AVAILABLE", "OUT_OF_STOCK", "DAMAGED", "IN_TRANSIT", "DISCONTINUED", "PRE_ORDER"];

function pseudoRandom(seed) {
    let x = Math.sin(seed) * 10000;
    return x - Math.floor(x);
}

function buildCatalog(total) {
    const now = "2026-09-28T12:35:25.";
    const list = [];
    for (let i = 1; i <= total; i++) {
        const r1 = pseudoRandom(i);
        const r2 = pseudoRandom(i + 100);
        const r3 = pseudoRandom(i + 200);
        const r4 = pseudoRandom(i + 300);
        const nanos = String(Math.floor(r4 * 1_000_000_000)).padStart(9, "0");

        list.push({
            spareCode:        `SPARE-${i}`,
            spareName:        `Spare Part ${i}`,
            spareDescription: `Description for spare part ${i}`,
            spareType:        SPARE_TYPES[Math.floor(r1 * SPARE_TYPES.length)],
            spareStatus:      SPARE_STATUSES[Math.floor(r2 * SPARE_STATUSES.length)],
            price:            String(Math.floor(r3 * 50)),
            quantity:         Math.floor(r1 * 50) + 1,
            updatedAt:        now + nanos,
        });
    }
    return list;
}

const CATALOG = buildCatalog(50);

// ──────────────────────────── Парсинг строки запроса ─────────────────────────────────────
function parseQuery(url) {
    const qIdx = url.indexOf("?");
    if (qIdx === -1) return {};
    const params = {};
    url.slice(qIdx + 1).split("&").forEach((pair) => {
        const [k, v] = pair.split("=");
        if (k) params[decodeURIComponent(k)] = decodeURIComponent(v || "");
    });
    return params;
}

// ──────────────────────────── Сервер ────────────────────────────────────────────────────
const server = http.createServer(async (req, res) => {
    const { method, url } = req;
    const basePath = url.split("?")[0];

    // Проверка BasicAuth
    if (!checkBasicAuth(req)) {
        res.writeHead(401, {
            ...JSON_HEADER,
            "WWW-Authenticate": 'Basic realm="CMS Spares"',
        });
        return res.end(JSON.stringify({ error: "Unauthorized" }));
    }

    // @endpoint GET /cms/spares
    if (method === "GET" && basePath === "/cms/spares") {
        const query = parseQuery(url);
        let page = parseInt(query.page, 10);
        if (isNaN(page) || page < 0) page = 0;
        let size = parseInt(query.size, 10);
        if (isNaN(size) || size < 1) size = 10;
        if (size > 10) size = 10;

        const start = page * size;
        const end   = start + size;
        const items = CATALOG.slice(start, end);

        res.writeHead(200, JSON_HEADER);
        return res.end(JSON.stringify(items, null, 2));
    }

    // Ответ для всех ненайденных ручек
    res.writeHead(404, JSON_HEADER);
    res.end(JSON.stringify({
        error: "Endpoint not defined",
        message: `Please add ${url} endpoint to this mock.`
    }));
});

server.listen(PORT, () => console.log(`CMS Mock started on port ${PORT}`));