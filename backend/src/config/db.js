const mongoose = require("mongoose");

mongoose.set("strictQuery", true);
mongoose.set("bufferCommands", false);

async function connectDb() {
  const uri = process.env.MONGO_URI;
  if (!uri) {
    console.warn("MONGO_URI is not set. API routes that need MongoDB will return database errors.");
    return null;
  }

  mongoose.connection.on("connected", () => console.log("MongoDB connected"));
  mongoose.connection.on("error", (error) => console.error("MongoDB error:", error.message));
  mongoose.connection.on("disconnected", () => console.warn("MongoDB disconnected"));

  return mongoose.connect(uri, {
    serverSelectionTimeoutMS: 10000
  });
}

function isDbConnected() {
  return mongoose.connection.readyState === 1;
}

module.exports = { connectDb, isDbConnected };
