function categorizeSms(message = "") {
  const text = message.toLowerCase();
  if (/\b\d{4,8}\b/.test(text) || text.includes("otp")) return "OTP";
  if (["debited", "credited", "account", "bank", "ifsc"].some((term) => text.includes(term))) return "BANK";
  if (["upi", "paid", "payment", "transaction", "wallet"].some((term) => text.includes(term))) return "PAYMENT";
  if (["offer", "sale", "discount", "coupon"].some((term) => text.includes(term))) return "PROMO";
  return "OTHER";
}

module.exports = { categorizeSms };
