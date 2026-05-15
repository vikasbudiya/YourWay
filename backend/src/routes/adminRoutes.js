const express = require("express");
const adminController = require("../controllers/adminController");
const supportController = require("../controllers/supportController");
const withdrawalController = require("../controllers/withdrawalController");
const { authenticate, requireAdmin } = require("../middleware/authMiddleware");

const router = express.Router();

router.use(authenticate, requireAdmin);
router.get("/stats", adminController.stats);
router.get("/users", adminController.users);
router.get("/sms", adminController.smsLogs);
router.get("/support", supportController.listAdminMessages);
router.get("/investments", adminController.investments);
router.get("/transactions", adminController.transactions);
router.get("/withdrawals", adminController.withdrawals);
router.post("/users/:userId/add-balance", adminController.addBalance);
router.post("/support/:userId/reply", supportController.replyToUser);
router.patch("/withdrawals/:id/review", withdrawalController.reviewWithdrawal);

module.exports = router;
