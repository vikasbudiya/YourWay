const express = require("express");
const notificationController = require("../controllers/notificationController");
const { authenticate } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/", authenticate, notificationController.listNotifications);
router.patch("/:id/read", authenticate, notificationController.markRead);

module.exports = router;
