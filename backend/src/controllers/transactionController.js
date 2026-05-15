const Transaction = require("../models/Transaction");

async function listTransactions(req, res, next) {
  try {
    const query = req.user.role === "admin" ? {} : { user: req.user._id };
    const transactions = await Transaction.find(query)
      .sort({ createdAt: -1 })
      .limit(Number(req.query.limit || 100));
    res.json({ data: transactions });
  } catch (error) {
    next(error);
  }
}

module.exports = { listTransactions };
