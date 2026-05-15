const { Server } = require("socket.io");

let io;

function initSocket(httpServer) {
  io = new Server(httpServer, {
    cors: {
      origin: process.env.CLIENT_ORIGIN || "*",
      methods: ["GET", "POST", "PATCH", "DELETE"]
    }
  });

  io.on("connection", (socket) => {
    socket.emit("notification", "YourWay", "Realtime channel connected.");
  });

  return io;
}

function emit(event, payload) {
  if (io) io.emit(event, payload);
}

function notify(title, body, payload = {}) {
  if (io) io.emit("notification", title, body, payload);
}

module.exports = { initSocket, emit, notify };
