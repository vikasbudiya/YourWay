const express = require("express");
const transactionController = require("../controllers/transactionController");
const { authenticate } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/", authenticate, transactionController.listTransactions);

module.exports = router;
