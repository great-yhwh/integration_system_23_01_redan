const http = require("http");
const PORT = process.env.PORT || 4501;

let lastUpload = null;

const server = http.createServer((req, res) => {
    const { method, url } = req;
    const basePath = url.split("?")[0];

    // Приём CSV
    if (method === "POST" && basePath === "/report/upload") {
        let body = "";
        req.on("data", chunk => body += chunk);
        req.on("end", () => {
            lastUpload = {
                timestamp: new Date().toISOString(),
                contentType: req.headers["content-type"],
                bodyLength: body.length,
                body: body
            };
            console.log(`[Report Mock] Получен CSV: ${body.length} байт`);
            res.writeHead(200, { "Content-Type": "application/json" });
            res.end(JSON.stringify({ status: "ok", bytes: body.length }));
        });
        return;
    }

    // Эндпоинт для проверки из теста
    if (method === "GET" && basePath === "/report/last") {
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify(lastUpload || { status: "no uploads" }));
        return;
    }

    res.writeHead(404);
    res.end();
});

server.listen(PORT, () => console.log(`Report Mock started on port ${PORT}`));