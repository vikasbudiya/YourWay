const express = require("express");
const paintingController = require("../controllers/paintingController");
const { authenticate, requireAdmin } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/", paintingController.listPaintings);
router.post("/", authenticate, requireAdmin, paintingController.createPainting);

module.exports = router;
