const fs = require("fs");

const LOG_FILE = __dirname + "/main-log.txt";

function log(msg) {
  try {
    fs.appendFileSync(LOG_FILE, "[main.js] " + msg + "\n");
  } catch (e) {}
}

log("script started");
log("__dirname = " + __dirname);

process.argv = [
  "node",
  "server-main.js",
  "--port", "8080",
  "--host", "127.0.0.1",
  "--without-connection-token"
];

log("about to import server-main.js");

(async () => {
  try {
    log("inside async IIFE");
    const serverPath = __dirname + "/out/server-main.js";
    log("importing " + serverPath);
    await import(serverPath);
    log("server-main.js imported successfully");
  } catch (err) {
    log("IMPORT FAILED");
    log("message: " + (err && err.message));
    log("stack: " + (err && err.stack));
  }
})();

log("script end reached");
