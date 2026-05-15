const Notification = require("../models/Notification");
const Transaction = require("../models/Transaction");
const { getOrCreateWallet } = require("../services/walletService");
const { emit, notify } = require("../services/socketService");

function safeText(value, fallback = "") {
  return String(value || fallback).trim().slice(0, 80);
}

function referenceId(value) {
  return safeText(value, `YW${Math.random().toString(36).slice(2, 10).toUpperCase()}`);
}

async function getWallet(req, res, next) {
  try {
    const wallet = await getOrCreateWallet(req.user._id);
    res.json({ data: wallet });
  } catch (error) {
    next(error);
  }
}

async function addDemoMoney(req, res, next) {
  try {
    const amount = Number(req.body.amount);
    const method = safeText(req.body.method, "UPI");
    const provider = safeText(req.body.provider);
    const bank = safeText(req.body.bank);
    const cardNetwork = safeText(req.body.cardNetwork);
    const ref = referenceId(req.body.referenceId);
    if (!Number.isFinite(amount) || amount <= 0) {
      const error = new Error("Valid amount is required.");
      error.status = 400;
      throw error;
    }

    const wallet = await getOrCreateWallet(req.user._id);
    wallet.mainBalance += amount;
    await wallet.save();

    const transaction = await Transaction.create({
      user: req.user._id,
      title: `Added money via ${[method, provider || bank || cardNetwork].filter(Boolean).join(" | ")}`,
      type: "CREDIT",
      wallet: "MAIN",
      amount,
      paymentMethod: method,
      provider: provider || bank || cardNetwork,
      referenceId: ref,
      metadata: { method, provider, bank, cardNetwork, referenceId: ref, sandbox: true }
    });
    await Notification.create({
      user: req.user._id,
      title: "Payment successful",
      body: `₹${amount} added to your main wallet.`,
      type: "WALLET"
    });

    emit("wallet_updated", { user: req.user._id, wallet });
    emit("transaction_created", transaction);
    notify("Payment successful", `₹${amount} added via ${method}.`);
    res.json({ data: { wallet, transaction } });
  } catch (error) {
    next(error);
  }
}

async function creditProfit(req, res, next) {
  try {
    const amount = Number(req.body.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      const error = new Error("Valid profit amount is required.");
      error.status = 400;
      throw error;
    }

    const wallet = await getOrCreateWallet(req.user._id);
    wallet.interestBalance += amount;
    await wallet.save();

    const transaction = await Transaction.create({
      user: req.user._id,
      title: "Daily profit credited",
      type: "PROFIT",
      wallet: "INTEREST",
      amount,
      metadata: { sandbox: true }
    });

    emit("wallet_updated", { user: req.user._id, wallet });
    notify("Profit credited", `₹${amount} added to interest wallet.`);
    res.json({ data: { wallet, transaction } });
  } catch (error) {
    next(error);
  }
}

module.exports = { getWallet, addDemoMoney, creditProfit };
