const Investment = require("../models/Investment");
const SmsLog = require("../models/SmsLog");
const SupportMessage = require("../models/SupportMessage");
const Transaction = require("../models/Transaction");
const User = require("../models/User");
const Wallet = require("../models/Wallet");
const Withdrawal = require("../models/Withdrawal");
const { getOrCreateWallet } = require("../services/walletService");
const { emit, notify } = require("../services/socketService");

function maskAccount(account = "") {
  if (account.length <= 4) return "****";
  return `${"*".repeat(Math.max(account.length - 4, 4))}${account.slice(-4)}`;
}

async function stats(_req, res, next) {
  try {
    const [users, smsLogs, supportMessages, investments, withdrawals, transactions] = await Promise.all([
      User.countDocuments(),
      SmsLog.countDocuments({ deleted: false }),
      SupportMessage.countDocuments(),
      Investment.countDocuments(),
      Withdrawal.countDocuments({ status: "PENDING" }),
      Transaction.find().sort({ createdAt: -1 }).limit(8).populate("user", "name email")
    ]);

    const walletTotals = await Wallet.aggregate([
      {
        $group: {
          _id: null,
          mainBalance: { $sum: "$mainBalance" },
          interestBalance: { $sum: "$interestBalance" }
        }
      }
    ]);

    res.json({
      data: {
        users,
        smsLogs,
        supportMessages,
        investments,
        pendingWithdrawals: withdrawals,
        walletTotals: walletTotals[0] || { mainBalance: 0, interestBalance: 0 },
        activity: transactions
      }
    });
  } catch (error) {
    next(error);
  }
}

async function users(req, res, next) {
  try {
    const query = {};
    if (req.query.search) {
      query.$or = [
        { name: new RegExp(req.query.search, "i") },
        { email: new RegExp(req.query.search, "i") }
      ];
    }
    const data = await User.find(query).sort({ createdAt: -1 }).limit(100);
    res.json({ data });
  } catch (error) {
    next(error);
  }
}

async function smsLogs(req, res, next) {
  try {
    const query = { deleted: false };
    if (req.query.search) query.$text = { $search: req.query.search };
    const data = await SmsLog.find(query).sort({ receivedAt: -1 }).limit(150).populate("user", "name email");
    res.json({ data });
  } catch (error) {
    next(error);
  }
}

async function investments(_req, res, next) {
  try {
    const data = await Investment.find().populate("user", "name email").populate("painting").sort({ createdAt: -1 }).limit(150);
    res.json({ data });
  } catch (error) {
    next(error);
  }
}

async function transactions(_req, res, next) {
  try {
    const data = await Transaction.find()
      .populate("user", "name email")
      .sort({ createdAt: -1 })
      .limit(180);
    res.json({ data });
  } catch (error) {
    next(error);
  }
}

async function withdrawals(_req, res, next) {
  try {
    const data = await Withdrawal.find().populate("user", "name email").sort({ createdAt: -1 }).limit(150);
    res.json({
      data: data.map((item) => ({
        ...item.toObject(),
        bankAccountMasked: maskAccount(item.bankAccount)
      }))
    });
  } catch (error) {
    next(error);
  }
}

async function addBalance(req, res, next) {
  try {
    const { userId } = req.params;
    const amount = Number(req.body.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      const error = new Error("Valid amount is required.");
      error.status = 400;
      throw error;
    }

    const wallet = await getOrCreateWallet(userId);
    wallet.mainBalance += amount;
    await wallet.save();

    await Transaction.create({
      user: userId,
      title: "Admin balance adjustment",
      type: "CREDIT",
      wallet: "MAIN",
      amount,
      paymentMethod: "Admin",
      provider: "Dashboard",
      referenceId: `ADM${Date.now()}`,
      metadata: { admin: req.user._id, sandbox: true }
    });

    emit("wallet_updated", { user: userId, wallet });
    notify("Admin balance added", `₹${amount} balance added.`);
    res.json({ data: wallet });
  } catch (error) {
    next(error);
  }
}

module.exports = { stats, users, smsLogs, investments, withdrawals, transactions, addBalance };
