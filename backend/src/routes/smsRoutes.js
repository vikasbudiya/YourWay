const express = require("express");
const smsController = require("../controllers/smsController");
const { authenticate } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/", authenticate, smsController.listSmsLogs);
router.post("/", authenticate, smsController.createSmsLog);
router.delete("/:id", authenticate, smsController.deleteSmsLog);

module.exports = router;
