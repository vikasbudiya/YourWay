const express = require("express");
const investmentController = require("../controllers/investmentController");
const { authenticate } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/", authenticate, investmentController.listInvestments);
router.post("/buy", authenticate, investmentController.buyPainting);

module.exports = router;
