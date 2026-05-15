const Painting = require("../models/Painting");

const defaultPaintings = [
  { slug: "starter", name: "Starter Painting", price: 1199, dailyProfit: 99, durationDays: 30 },
  { slug: "bronze", name: "Bronze Painting", price: 5999, dailyProfit: 499, durationDays: 30 },
  { slug: "silver", name: "Silver Painting", price: 12999, dailyProfit: 999, durationDays: 30 },
  { slug: "gold", name: "Gold Painting", price: 25999, dailyProfit: 1999, durationDays: 30 }
];

async function seedPaintings() {
  for (const painting of defaultPaintings) {
    await Painting.updateOne(
      { slug: painting.slug },
      { $setOnInsert: painting },
      { upsert: true }
    );
  }
}

module.exports = { seedPaintings, defaultPaintings };
