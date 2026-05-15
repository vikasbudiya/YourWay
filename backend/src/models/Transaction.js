const mongoose = require("mongoose");

const transactionSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true, index: true },
    title: { type: String, required: true },
    type: { type: String, enum: ["CREDIT", "DEBIT", "PROFIT", "WITHDRAWAL"], required: true, index: true },
    wallet: { type: String, enum: ["MAIN", "INTEREST"], required: true },
    amount: { type: Number, required: true, min: 0 },
    status: { type: String, default: "SUCCESS", index: true },
    paymentMethod: { type: String, trim: true, default: "", index: true },
    provider: { type: String, trim: true, default: "" },
    referenceId: { type: String, trim: true, default: "", index: true },
    metadata: { type: Object, default: {} }
  },
  { timestamps: true }
);

module.exports = mongoose.model("Transaction", transactionSchema);
