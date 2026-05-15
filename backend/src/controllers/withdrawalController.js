const Notification = require("../models/Notification");
const Transaction = require("../models/Transaction");
const Withdrawal = require("../models/Withdrawal");
const { getOrCreateWallet } = require("../services/walletService");
const { emit, notify } = require("../services/socketService");

async function createWithdrawal(req, res, next) {
  try {
    const { name, bankAccount, ifsc, upiId } = req.body;
    const amount = Number(req.body.amount);
    if (!name || !bankAccount || !ifsc || !upiId || !Number.isFinite(amount) || amount <= 0) {
      const error = new Error("Complete withdrawal details and a valid amount are required.");
      error.status = 400;
      throw error;
    }

    const wallet = await getOrCreateWallet(req.user._id);
    if (wallet.interestBalance < amount) {
      const error = new Error("Withdrawals are allowed only from available interest wallet balance.");
      error.status = 400;
      throw error;
    }

    wallet.interestBalance -= amount;
    await wallet.save();

    const withdrawal = await Withdrawal.create({
      user: req.user._id,
      name,
      bankAccount,
      ifsc,
      upiId,
      amount
    });

    const transaction = await Transaction.create({
      user: req.user._id,
      title: "Withdrawal requested",
      type: "WITHDRAWAL",
      wallet: "INTEREST",
      amount,
      status: "PENDING",
      metadata: { withdrawal: withdrawal._id, sandbox: true }
    });

    await Notification.create({
      user: req.user._id,
      title: "Withdrawal pending",
      body: "Your withdrawal is awaiting admin review.",
      type: "WITHDRAWAL"
    });

    emit("wallet_updated", { user: req.user._id, wallet });
    emit("withdrawal_created", withdrawal);
    notify("Withdrawal pending", "A new withdrawal needs admin review.");
    res.status(201).json({ data: { withdrawal, transaction, wallet } });
  } catch (error) {
    next(error);
  }
}

async function listWithdrawals(req, res, next) {
  try {
    const query = req.user.role === "admin" ? {} : { user: req.user._id };
    const withdrawals = await Withdrawal.find(query)
      .populate("user", "name email")
      .sort({ createdAt: -1 });
    res.json({ data: withdrawals });
  } catch (error) {
    next(error);
  }
}

async function reviewWithdrawal(req, res, next) {
  try {
    const status = String(req.body.status || "").toUpperCase();
    if (!["APPROVED", "REJECTED"].includes(status)) {
      const error = new Error("status must be APPROVED or REJECTED.");
      error.status = 400;
      throw error;
    }

    const withdrawal = await Withdrawal.findById(req.params.id);
    if (!withdrawal) {
      const error = new Error("Withdrawal not found.");
      error.status = 404;
      throw error;
    }
    if (withdrawal.status !== "PENDING") {
      const error = new Error("Withdrawal has already been reviewed.");
      error.status = 400;
      throw error;
    }

    withdrawal.status = status;
    withdrawal.reviewNote = req.body.reviewNote || "";
    withdrawal.reviewedBy = req.user._id;
    withdrawal.reviewedAt = new Date();
    await withdrawal.save();

    await Transaction.updateMany(
      { "metadata.withdrawal": withdrawal._id },
      { $set: { status } }
    );

    if (status === "REJECTED") {
      const wallet = await getOrCreateWallet(withdrawal.user);
      wallet.interestBalance += withdrawal.amount;
      await wallet.save();
      emit("wallet_updated", { user: withdrawal.user, wallet });
    }

    await Notification.create({
      user: withdrawal.user,
      title: `Withdrawal ${status.toLowerCase()}`,
      body: req.body.reviewNote || `Your withdrawal was ${status.toLowerCase()}.`,
      type: "WITHDRAWAL"
    });

    emit("withdrawal_updated", withdrawal);
    notify("Withdrawal reviewed", `Withdrawal ${status.toLowerCase()}.`);
    res.json({ data: withdrawal });
  } catch (error) {
    next(error);
  }
}

module.exports = { createWithdrawal, listWithdrawals, reviewWithdrawal };
