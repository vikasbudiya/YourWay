const Wallet = require("../models/Wallet");

async function getOrCreateWallet(userId) {
  let wallet = await Wallet.findOne({ user: userId });
  if (!wallet) {
    wallet = await Wallet.create({ user: userId, mainBalance: 0, interestBalance: 0 });
  }
  return wallet;
}

module.exports = { getOrCreateWallet };
