const jwt = require("jsonwebtoken");
const User = require("../models/User");

async function authenticate(req, _res, next) {
  try {
    const header = req.headers.authorization || "";
    const token = header.startsWith("Bearer ") ? header.slice(7) : "";
    if (!token) {
      const error = new Error("Authentication required");
      error.status = 401;
      throw error;
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET || "yourway-demo-secret");
    const user = await User.findById(decoded.sub);
    if (!user || !user.isActive) {
      const error = new Error("Invalid session");
      error.status = 401;
      throw error;
    }

    req.user = user;
    next();
  } catch (error) {
    error.status = error.status || 401;
    next(error);
  }
}

async function optionalAuthenticate(req, _res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!token) return next();

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET || "yourway-demo-secret");
    req.user = await User.findById(decoded.sub);
  } catch (_error) {
    req.user = null;
  }
  next();
}

function requireAdmin(req, _res, next) {
  if (req.user?.role === "admin") return next();
  const error = new Error("Admin access required");
  error.status = 403;
  next(error);
}

function signToken(user) {
  return jwt.sign(
    { sub: user._id.toString(), role: user.role },
    process.env.JWT_SECRET || "yourway-demo-secret",
    { expiresIn: process.env.JWT_EXPIRES_IN || "7d" }
  );
}

module.exports = { authenticate, optionalAuthenticate, requireAdmin, signToken };
