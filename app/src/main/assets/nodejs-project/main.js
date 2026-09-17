console.log("main.js: script started");

process.argv = [
  "node",
  "server-main.js",
  "--port", "8080",
  "--host", "127.0.0.1",
  "--without-connection-token"
];

(async () => {
  try {
    console.log("main.js: importing server-main.js...");
    await import("./out/server-main.js");
    console.log("main.js: server-main.js loaded successfully");
  } catch (err) {
    console.error("main.js: import failed - " + err.message);
    console.error("main.js: stack - " + err.stack);
  }
})();
