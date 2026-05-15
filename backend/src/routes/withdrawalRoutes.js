const express = require("express");
const withdrawalController = require("../controllers/withdrawalController");
const { authenticate, requireAdmin } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/", authenticate, withdrawalController.listWithdrawals);
router.post("/", authenticate, withdrawalController.createWithdrawal);
router.patch("/:id/review", authenticate, requireAdmin, withdrawalController.reviewWithdrawal);

module.exports = router;
