const blockedSecretKeys = [
  "cardnumber",
  "card_number",
  "cvv",
  "cvc",
  "otp",
  "pin",
  "upipin",
  "upi_pin",
  "password",
  "bankpassword",
  "bank_password"
];

function rejectUnsafePaymentFields(req, _res, next) {
  const unsafe = findUnsafeKey(req.body || {});
  if (unsafe) {
    const error = new Error("Sandbox payments must not include card numbers, CVV, OTPs, PINs, or banking passwords.");
    error.status = 400;
    return next(error);
  }
  next();
}

function findUnsafeKey(value) {
  if (!value || typeof value !== "object") return "";
  if (Array.isArray(value)) {
    for (const item of value) {
      const nested = findUnsafeKey(item);
      if (nested) return nested;
    }
    return "";
  }

  for (const [key, nestedValue] of Object.entries(value)) {
    const normalized = key.toLowerCase().replace(/[^a-z0-9_]/g, "");
    if (blockedSecretKeys.includes(normalized)) return key;
    const nested = findUnsafeKey(nestedValue);
    if (nested) return nested;
  }
  return "";
}

module.exports = { rejectUnsafePaymentFields };
