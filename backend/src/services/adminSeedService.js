const bcrypt = require("bcryptjs");
const User = require("../models/User");
const { getOrCreateWallet } = require("./walletService");

async function ensureAdminUser() {
  const email = (process.env.ADMIN_SEED_EMAIL || "admin@yourway.demo").toLowerCase();
  const password = process.env.ADMIN_SEED_PASSWORD || "ChangeThisDemoPassword123";

  const existing = await User.findOne({ email });
  if (existing) return existing;

  const admin = await User.create({
    name: "YourWay Admin",
    email,
    passwordHash: await bcrypt.hash(password, 12),
    role: "admin"
  });
  await getOrCreateWallet(admin._id);
  console.log(`Seeded admin user: ${email}`);
  return admin;
}

module.exports = { ensureAdminUser };
