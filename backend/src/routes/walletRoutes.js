const express = require("express");
const walletController = require("../controllers/walletController");
const { authenticate } = require("../middleware/authMiddleware");
const { rejectUnsafePaymentFields } = require("../middleware/validateDemoSafety");

const router = express.Router();

router.get("/", authenticate, walletController.getWallet);
router.post("/demo-add", authenticate, rejectUnsafePaymentFields, walletController.addDemoMoney);
router.post("/credit-profit", authenticate, walletController.creditProfit);

module.exports = router;
