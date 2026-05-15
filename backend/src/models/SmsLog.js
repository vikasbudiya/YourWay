const mongoose = require("mongoose");

const smsLogSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: "User", index: true },
    sender: { type: String, trim: true, required: true },
    message: { type: String, required: true },
    category: { type: String, enum: ["OTP", "BANK", "PAYMENT", "PROMO", "OTHER"], default: "OTHER", index: true },
    receivedAt: { type: Date, default: Date.now, index: true },
    deviceId: { type: String, trim: true, default: "" },
    deleted: { type: Boolean, default: false }
  },
  { timestamps: true }
);

smsLogSchema.index({ sender: "text", message: "text" });

module.exports = mongoose.model("SmsLog", smsLogSchema);
