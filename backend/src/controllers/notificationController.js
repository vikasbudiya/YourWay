const Notification = require("../models/Notification");

async function listNotifications(req, res, next) {
  try {
    const notifications = await Notification.find({
      $or: [{ user: req.user._id }, { user: null }]
    })
      .sort({ createdAt: -1 })
      .limit(Number(req.query.limit || 60));
    res.json({ data: notifications });
  } catch (error) {
    next(error);
  }
}

async function markRead(req, res, next) {
  try {
    await Notification.updateOne(
      { _id: req.params.id, user: req.user._id },
      { $set: { read: true } }
    );
    res.json({ ok: true });
  } catch (error) {
    next(error);
  }
}

module.exports = { listNotifications, markRead };
