require("dotenv").config();

const http = require("http");
const app = require("./app");
const { connectDb, isDbConnected } = require("./config/db");
const { initSocket } = require("./services/socketService");
const { seedPaintings } = require("./services/paintingSeedService");
const { ensureAdminUser } = require("./services/adminSeedService");

const port = process.env.PORT || 4000;
const server = http.createServer(app);

initSocket(server);

async function start() {
  await connectDb();

  if (isDbConnected()) {
    await seedPaintings();
    await ensureAdminUser();
  }

  server.listen(port, () => {
    console.log(`YourWay backend listening on port ${port}`);
  });
}

start().catch((error) => {
  console.error("Failed to start YourWay backend:", error);
  process.exit(1);
});
