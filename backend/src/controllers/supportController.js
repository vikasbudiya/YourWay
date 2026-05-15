const SupportMessage = require("../models/SupportMessage");
const User = require("../models/User");
const { emit } = require("../services/socketService");

function serializeMessage(message) {
  const user = message.user && typeof message.user === "object" ? message.user : null;
  const userId = user?._id || message.user;
  return {
    id: message._id.toString(),
    userId: userId?.toString(),
    userName: user?.name || "",
    userEmail: user?.email || "",
    body: message.body,
    fromSupport: message.fromSupport,
    createdAt: message.createdAt.getTime(),
    createdAtIso: message.createdAt.toISOString()
  };
}

function validateBody(body) {
  const cleanBody = String(body || "").trim();
  if (!cleanBody) {
    const error = new Error("Message body is required.");
    error.status = 400;
    throw error;
  }
  if (cleanBody.length > 2000) {
    const error = new Error("Message body must be 2000 characters or fewer.");
    error.status = 400;
    throw error;
  }
  return cleanBody;
}

async function listUserMessages(req, res, next) {
  try {
    const data = await SupportMessage.find({ user: req.user._id })
      .sort({ createdAt: -1 })
      .limit(Number(req.query.limit || 80))
      .populate("user", "name email");

    res.json({ data: data.reverse().map(serializeMessage) });
  } catch (error) {
    next(error);
  }
}

async function createUserMessage(req, res, next) {
  try {
    const body = validateBody(req.body.body);
    const message = await SupportMessage.create({
      user: req.user._id,
      body,
      fromSupport: false,
      readByUser: true
    });
    await message.populate("user", "name email");

    const payload = serializeMessage(message);
    emit("support_message_created", payload);
    res.status(201).json({ data: payload });
  } catch (error) {
    next(error);
  }
}

async function listAdminMessages(req, res, next) {
  try {
    const query = {};
    if (req.query.search) {
      const users = await User.find({
        $or: [
          { name: new RegExp(req.query.search, "i") },
          { email: new RegExp(req.query.search, "i") }
        ]
      }).select("_id");
      query.$or = [
        { body: new RegExp(req.query.search, "i") },
        { user: { $in: users.map((user) => user._id) } }
      ];
    }

    const data = await SupportMessage.find(query)
      .populate("user", "name email")
      .sort({ createdAt: -1 })
      .limit(Number(req.query.limit || 150));

    res.json({ data: data.map(serializeMessage) });
  } catch (error) {
    next(error);
  }
}

async function replyToUser(req, res, next) {
  try {
    const body = validateBody(req.body.body);
    const user = await User.findById(req.params.userId);
    if (!user) {
      const error = new Error("User not found.");
      error.status = 404;
      throw error;
    }

    const latestUserMessage = await SupportMessage.findOne({
      user: user._id,
      fromSupport: false
    }).sort({ createdAt: -1 });

    const message = await SupportMessage.create({
      user: user._id,
      body,
      fromSupport: true,
      readByAdmin: true,
      repliedTo: latestUserMessage?._id
    });
    await message.populate("user", "name email");

    const payload = serializeMessage(message);
    emit("support_message_replied", payload);
    res.status(201).json({ data: payload });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  listUserMessages,
  createUserMessage,
  listAdminMessages,
  replyToUser
};
