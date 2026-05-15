require("dotenv").config();

const { connectDb } = require("../config/db");
const { seedPaintings } = require("../services/paintingSeedService");
const { ensureAdminUser } = require("../services/adminSeedService");

async function run() {
  await connectDb();
  await seedPaintings();
  await ensureAdminUser();
  console.log("Seed complete.");
  process.exit(0);
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
