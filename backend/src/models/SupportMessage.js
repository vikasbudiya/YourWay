const mongoose = require("mongoose");

const supportMessageSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true, index: true },
    body: { type: String, trim: true, required: true, maxlength: 2000 },
    fromSupport: { type: Boolean, default: false, index: true },
    readByAdmin: { type: Boolean, default: false },
    readByUser: { type: Boolean, default: false },
    repliedTo: { type: mongoose.Schema.Types.ObjectId, ref: "SupportMessage" }
  },
  { timestamps: true }
);

supportMessageSchema.index({ user: 1, createdAt: -1 });
supportMessageSchema.index({ body: "text" });

module.exports = mongoose.model("SupportMessage", supportMessageSchema);
