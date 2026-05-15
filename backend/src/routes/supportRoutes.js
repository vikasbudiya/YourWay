const express = require("express");
const supportController = require("../controllers/supportController");
const { authenticate } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/messages", authenticate, supportController.listUserMessages);
router.post("/messages", authenticate, supportController.createUserMessage);

module.exports = router;
