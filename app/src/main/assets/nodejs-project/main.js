const fs = require("fs");

const LOG_FILE = globalThis.__projectDir + "/main-log.txt";

function log(msg) {
  try {
    fs.appendFileSync(LOG_FILE, "[main.js] " + msg + "\n");
  } catch (e) {
    // nothing we can do
  }
}

log("script started");
log("project dir = " + globalThis.__projectDir);

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
    const serverPath = globalThis.__projectDir + "/out/server-main.js";
    const serverUrl = "file://" + serverPath;
    log("importing " + serverUrl);
    await import(serverUrl);
    log("server-main.js imported successfully");
  } catch (err) {
    log("IMPORT FAILED");
    log("message: " + (err && err.message));
    log("stack: " + (err && err.stack));
  }
})();

log("script end reached");
