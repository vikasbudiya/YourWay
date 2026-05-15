const mongoose = require("mongoose");

const investmentSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true, index: true },
    painting: { type: mongoose.Schema.Types.ObjectId, ref: "Painting", required: true },
    planName: { type: String, required: true },
    price: { type: Number, required: true, min: 0 },
    dailyProfit: { type: Number, required: true, min: 0 },
    quantity: { type: Number, default: 1, min: 1 },
    durationDays: { type: Number, default: 30, min: 1 },
    startedAt: { type: Date, default: Date.now },
    status: { type: String, enum: ["ACTIVE", "COMPLETED", "CANCELLED"], default: "ACTIVE", index: true }
  },
  {
    timestamps: true,
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
  }
);

investmentSchema.virtual("totalReturns").get(function totalReturns() {
  return this.dailyProfit * this.quantity * this.durationDays;
});

module.exports = mongoose.model("Investment", investmentSchema);
