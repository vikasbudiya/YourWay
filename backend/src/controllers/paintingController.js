const Painting = require("../models/Painting");

async function listPaintings(_req, res, next) {
  try {
    const paintings = await Painting.find({ isActive: true }).sort({ price: 1 });
    res.json({ data: paintings });
  } catch (error) {
    next(error);
  }
}

async function createPainting(req, res, next) {
  try {
    const painting = await Painting.create(req.body);
    res.status(201).json({ data: painting });
  } catch (error) {
    next(error);
  }
}

module.exports = { listPaintings, createPainting };
