const Investment = require("../models/Investment");
const Notification = require("../models/Notification");
const Painting = require("../models/Painting");
const Transaction = require("../models/Transaction");
const mongoose = require("mongoose");
const { getOrCreateWallet } = require("../services/walletService");
const { emit, notify } = require("../services/socketService");

async function listInvestments(req, res, next) {
  try {
    const query = req.user.role === "admin" ? {} : { user: req.user._id };
    const investments = await Investment.find(query)
      .populate("painting")
      .sort({ createdAt: -1 });
    res.json({ data: investments });
  } catch (error) {
    next(error);
  }
}

async function buyPainting(req, res, next) {
  try {
    const quantity = Math.max(Number(req.body.quantity || 1), 1);
    const planKey = String(req.body.paintingId || "");
    const planQuery = mongoose.Types.ObjectId.isValid(planKey)
      ? { $or: [{ _id: planKey }, { slug: planKey }], isActive: true }
      : { slug: planKey, isActive: true };
    const painting = await Painting.findOne(planQuery);
    if (!painting) {
      const error = new Error("Painting plan not found.");
      error.status = 404;
      throw error;
    }

    const total = painting.price * quantity;
    const wallet = await getOrCreateWallet(req.user._id);
    if (wallet.mainBalance < total) {
      const error = new Error("Insufficient main wallet balance.");
      error.status = 400;
      throw error;
    }

    wallet.mainBalance -= total;
    await wallet.save();

    const investment = await Investment.create({
      user: req.user._id,
      painting: painting._id,
      planName: painting.name,
      price: painting.price,
      dailyProfit: painting.dailyProfit,
      quantity,
      durationDays: painting.durationDays
    });

    const transaction = await Transaction.create({
      user: req.user._id,
      title: `Purchased ${painting.name} x${quantity}`,
      type: "DEBIT",
      wallet: "MAIN",
      amount: total,
      metadata: { painting: painting._id, quantity, sandbox: true }
    });

    await Notification.create({
      user: req.user._id,
      title: "Investment activated",
      body: `${painting.name} x${quantity} is active.`,
      type: "INVESTMENT"
    });

    emit("wallet_updated", { user: req.user._id, wallet });
    emit("investment_created", investment);
    notify("Investment activated", `${painting.name} x${quantity} is active.`);
    res.status(201).json({ data: { investment, wallet, transaction } });
  } catch (error) {
    next(error);
  }
}

module.exports = { listInvestments, buyPainting };
