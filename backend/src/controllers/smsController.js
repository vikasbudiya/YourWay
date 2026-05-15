const SmsLog = require("../models/SmsLog");
const { categorizeSms } = require("../services/smsService");
const { emit, notify } = require("../services/socketService");

async function createSmsLog(req, res, next) {
  try {
    const { sender, message, receivedAt, deviceId } = req.body;
    if (!sender || !message) {
      const error = new Error("sender and message are required.");
      error.status = 400;
      throw error;
    }

    const smsLog = await SmsLog.create({
      user: req.user?._id || req.body.userId || undefined,
      sender,
      message,
      category: req.body.category || categorizeSms(message),
      receivedAt: receivedAt ? new Date(receivedAt) : new Date(),
      deviceId
    });

    emit("sms_created", smsLog);
    notify("SMS synced", `New ${smsLog.category.toLowerCase()} message from ${sender}`, { smsId: smsLog._id });

    res.status(201).json({ data: smsLog });
  } catch (error) {
    next(error);
  }
}

async function listSmsLogs(req, res, next) {
  try {
    const query = { deleted: false };
    if (req.user.role !== "admin") query.user = req.user._id;
    if (req.query.category) query.category = req.query.category;
    if (req.query.search) query.$text = { $search: req.query.search };

    const logs = await SmsLog.find(query)
      .sort({ receivedAt: -1 })
      .limit(Number(req.query.limit || 100));

    res.json({ data: logs });
  } catch (error) {
    next(error);
  }
}

async function deleteSmsLog(req, res, next) {
  try {
    const query = { _id: req.params.id };
    if (req.user.role !== "admin") query.user = req.user._id;
    await SmsLog.updateOne(query, { $set: { deleted: true } });
    emit("sms_deleted", { id: req.params.id });
    res.json({ ok: true });
  } catch (error) {
    next(error);
  }
}

module.exports = { createSmsLog, listSmsLogs, deleteSmsLog };
