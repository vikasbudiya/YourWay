const bcrypt = require("bcryptjs");
const User = require("../models/User");
const { getOrCreateWallet } = require("../services/walletService");
const { signToken } = require("../middleware/authMiddleware");

function publicUser(user) {
  return {
    id: user._id,
    name: user.name,
    email: user.email,
    phone: user.phone,
    role: user.role
  };
}

async function signup(req, res, next) {
  try {
    const { name, email, password } = req.body;
    if (!email || !password || password.length < 6) {
      const error = new Error("Email and a 6+ character password are required.");
      error.status = 400;
      throw error;
    }

    const existing = await User.findOne({ email: email.toLowerCase() });
    if (existing) {
      const error = new Error("Email is already registered.");
      error.status = 409;
      throw error;
    }

    const user = await User.create({
      name: name || email.split("@")[0],
      email,
      passwordHash: await bcrypt.hash(password, 12)
    });
    await getOrCreateWallet(user._id);

    res.status(201).json({ token: signToken(user), user: publicUser(user) });
  } catch (error) {
    next(error);
  }
}

async function login(req, res, next) {
  try {
    const { email, password } = req.body;
    const user = await User.findOne({ email: String(email || "").toLowerCase() }).select("+passwordHash");
    if (!user || !(await bcrypt.compare(password || "", user.passwordHash))) {
      const error = new Error("Invalid email or password.");
      error.status = 401;
      throw error;
    }

    user.lastLoginAt = new Date();
    await user.save();
    await getOrCreateWallet(user._id);

    res.json({ token: signToken(user), user: publicUser(user) });
  } catch (error) {
    next(error);
  }
}

async function me(req, res) {
  res.json({ user: publicUser(req.user) });
}

module.exports = { signup, login, me, publicUser };
