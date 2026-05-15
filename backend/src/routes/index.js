const express = require("express");
const authRoutes = require("./authRoutes");
const smsRoutes = require("./smsRoutes");
const paintingRoutes = require("./paintingRoutes");
const walletRoutes = require("./walletRoutes");
const investmentRoutes = require("./investmentRoutes");
const withdrawalRoutes = require("./withdrawalRoutes");
const transactionRoutes = require("./transactionRoutes");
const notificationRoutes = require("./notificationRoutes");
const adminRoutes = require("./adminRoutes");
const supportRoutes = require("./supportRoutes");

const router = express.Router();

router.use("/auth", authRoutes);
router.use("/sms", smsRoutes);
router.use("/paintings", paintingRoutes);
router.use("/wallet", walletRoutes);
router.use("/investments", investmentRoutes);
router.use("/withdrawals", withdrawalRoutes);
router.use("/transactions", transactionRoutes);
router.use("/notifications", notificationRoutes);
router.use("/support", supportRoutes);
router.use("/admin", adminRoutes);

module.exports = router;
