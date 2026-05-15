const mongoose = require("mongoose");

const paintingSchema = new mongoose.Schema(
  {
    slug: { type: String, trim: true, lowercase: true, required: true, unique: true },
    name: { type: String, trim: true, required: true },
    price: { type: Number, required: true, min: 0 },
    dailyProfit: { type: Number, required: true, min: 0 },
    durationDays: { type: Number, default: 30, min: 1 },
    imageUrl: { type: String, trim: true, default: "" },
    isActive: { type: Boolean, default: true }
  },
  { timestamps: true }
);

module.exports = mongoose.model("Painting", paintingSchema);
