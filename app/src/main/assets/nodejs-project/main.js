const { fork } = require('child_process');
fork('./out/server-main.js', [
  '--port', '8080',
  '--host', '127.0.0.1',
  '--without-connection-token'
], { stdio: 'inherit' });
