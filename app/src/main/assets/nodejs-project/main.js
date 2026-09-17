// Set up process.argv so the server sees the CLI flags it expects
process.argv = [
  'node',
  'server-main.js',
  '--port', '8080',
  '--host', '127.0.0.1',
  '--without-connection-token'
];

// Dynamically import the server's ESM entry point
import('./out/server-main.js').catch((err) => {
  console.error('Server failed to start:', err);
});
