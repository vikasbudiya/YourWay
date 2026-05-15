const path = require("path");
const cors = require("cors");
const express = require("express");
const helmet = require("helmet");
const morgan = require("morgan");
const rateLimit = require("express-rate-limit");
const routes = require("./routes");
const smsController = require("./controllers/smsController");
const { errorHandler, notFoundHandler } = require("./middleware/errorMiddleware");

const app = express();

app.use(helmet({
  contentSecurityPolicy: false
}));
app.use(cors({ origin: process.env.CLIENT_ORIGIN || "*" }));
app.use(express.json({ limit: "1mb" }));
app.use(express.urlencoded({ extended: true }));
app.use(morgan("dev"));
app.use(rateLimit({
  windowMs: 60 * 1000,
  max: 240,
  standardHeaders: true,
  legacyHeaders: false
}));

app.get("/api/health", (_req, res) => {
  res.json({ ok: true, service: "yourway-backend", timestamp: new Date().toISOString() });
});

app.post("/sms", smsController.createSmsLog);
app.use("/api", routes);
app.use("/admin", express.static(path.join(__dirname, "..", "public", "admin")));
app.get("/", (_req, res) => res.redirect("/admin"));

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
