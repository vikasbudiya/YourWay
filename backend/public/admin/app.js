const state = {
  token: localStorage.getItem("yourway_admin_token") || "",
  users: [],
  supportMessages: []
};

const $ = (id) => document.getElementById(id);
const money = (value) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(value || 0);
const date = (value) => new Date(value).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" });
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  "\"": "&quot;",
  "'": "&#39;"
}[char]));

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: state.token ? `Bearer ${state.token}` : "",
      ...(options.headers || {})
    }
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.message || "Request failed");
  return payload;
}

function setLoggedIn(loggedIn) {
  $("loginPanel").classList.toggle("hidden", loggedIn);
  $("dashboard").classList.toggle("hidden", !loggedIn);
}

async function login(event) {
  event.preventDefault();
  $("loginMessage").textContent = "";
  try {
    const payload = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        email: $("email").value,
        password: $("password").value
      })
    });
    if (payload.user.role !== "admin") throw new Error("Admin role required.");
    state.token = payload.token;
    localStorage.setItem("yourway_admin_token", state.token);
    setLoggedIn(true);
    await loadDashboard();
  } catch (error) {
    $("loginMessage").textContent = error.message;
  }
}

function logout() {
  state.token = "";
  localStorage.removeItem("yourway_admin_token");
  setLoggedIn(false);
}

async function loadDashboard() {
  const [stats, users, support, sms, investments, withdrawals, transactions] = await Promise.all([
    api("/api/admin/stats"),
    api(`/api/admin/users?search=${encodeURIComponent($("userSearch").value || "")}`),
    api(`/api/admin/support?search=${encodeURIComponent($("supportSearch").value || "")}`),
    api(`/api/admin/sms?search=${encodeURIComponent($("smsSearch").value || "")}`),
    api("/api/admin/investments"),
    api("/api/admin/withdrawals"),
    api("/api/admin/transactions")
  ]);

  state.users = users.data || [];
  state.supportMessages = support.data || [];
  renderStats(stats.data);
  renderUsers(state.users);
  renderSupport(state.supportMessages);
  renderSms(sms.data || []);
  renderInvestments(investments.data || []);
  renderWithdrawals(withdrawals.data || []);
  renderTransactions(transactions.data || []);
  renderActivity(stats.data.activity || []);
}

function renderStats(stats) {
  const cards = [
    ["Users", stats.users],
    ["Support messages", stats.supportMessages],
    ["SMS logs", stats.smsLogs],
    ["Investments", stats.investments],
    ["Pending withdrawals", stats.pendingWithdrawals],
    ["Main wallet pool", money(stats.walletTotals.mainBalance)],
    ["Interest wallet pool", money(stats.walletTotals.interestBalance)]
  ];
  $("stats").innerHTML = cards.map(([label, value]) => `
    <article class="stat-card">
      <span class="muted">${label}</span>
      <strong>${value}</strong>
    </article>
  `).join("");
}

function supportThreads(messages) {
  const threads = new Map();
  messages.forEach((message) => {
    const key = message.userId || "unknown";
    if (!threads.has(key)) {
      threads.set(key, {
        userId: key,
        userName: message.userName || "Unknown user",
        userEmail: message.userEmail || "No email",
        messages: []
      });
    }
    threads.get(key).messages.push(message);
  });

  return [...threads.values()].map((thread) => ({
    ...thread,
    messages: thread.messages.sort((a, b) => a.createdAt - b.createdAt)
  }));
}

function renderSupport(messages) {
  const threads = supportThreads(messages);
  $("supportMessages").innerHTML = threads.map((thread) => {
    const latest = thread.messages[thread.messages.length - 1];
    const userName = escapeHtml(thread.userName);
    const userEmail = escapeHtml(thread.userEmail);
    return `
      <div class="support-thread">
        <div class="thread-head">
          <div>
            <strong>${userName}</strong>
            <p class="muted">${userEmail} | Last update ${date(latest.createdAt)}</p>
          </div>
          <span class="pill">${thread.messages.length} messages</span>
        </div>
        <div class="thread-messages">
          ${thread.messages.map((message) => `
            <div class="chat-line ${message.fromSupport ? "support" : "user"}">
              <span>${message.fromSupport ? "Support" : "User"}</span>
              <p>${escapeHtml(message.body)}</p>
              <small>${date(message.createdAt)}</small>
            </div>
          `).join("")}
        </div>
        <form class="reply-form" onsubmit="replySupport(event, '${thread.userId}')">
          <input name="body" type="text" placeholder="Reply to ${userName}" required maxlength="2000" />
          <button type="submit">Reply</button>
        </form>
      </div>
    `;
  }).join("") || `<p class="muted">No support messages yet.</p>`;
}

function renderUsers(users) {
  $("users").innerHTML = users.map((user) => `
    <div class="row">
      <div>
        <strong>${user.name}</strong>
        <p class="muted">${user.email} | ${user.role}</p>
      </div>
      <form class="row-actions" onsubmit="addBalance(event, '${user._id}')">
        <input name="amount" type="number" min="1" placeholder="Amount" required />
        <button type="submit">Add balance</button>
      </form>
    </div>
  `).join("") || `<p class="muted">No users found.</p>`;
}

function renderSms(logs) {
  $("smsLogs").innerHTML = logs.map((sms) => `
    <div class="row">
      <div>
        <span class="pill">${sms.category}</span>
        <strong>${sms.sender}</strong>
        <p>${sms.message}</p>
        <p class="muted">${date(sms.receivedAt)} ${sms.user ? `| ${sms.user.email}` : "| legacy sync"}</p>
      </div>
    </div>
  `).join("") || `<p class="muted">No SMS logs.</p>`;
}

function renderInvestments(items) {
  $("investments").innerHTML = items.map((item) => `
    <div class="row">
      <div>
        <strong>${item.planName} x${item.quantity}</strong>
        <p class="muted">${item.user?.email || "Unknown user"} | ${item.status}</p>
        <p>${money(item.price * item.quantity)} invested | ${money(item.dailyProfit * item.quantity)} daily</p>
      </div>
    </div>
  `).join("") || `<p class="muted">No investments yet.</p>`;
}

function renderWithdrawals(items) {
  $("withdrawals").innerHTML = items.map((item) => `
    <div class="row">
      <div>
        <span class="pill">${item.status}</span>
        <strong>${item.name} | ${money(item.amount)}</strong>
        <p class="muted">${item.user?.email || "Unknown user"} | ${item.bankAccountMasked || "****"} | ${item.ifsc} | ${item.upiId}</p>
        ${item.status === "PENDING" ? `
          <div class="row-actions">
            <button onclick="reviewWithdrawal('${item._id}', 'APPROVED')">Approve</button>
            <button class="danger" onclick="reviewWithdrawal('${item._id}', 'REJECTED')">Reject</button>
          </div>
        ` : ""}
      </div>
    </div>
  `).join("") || `<p class="muted">No withdrawals.</p>`;
}

function renderTransactions(items) {
  $("transactions").innerHTML = items.map((item) => `
    <div class="row">
      <div>
        <span class="pill">${item.status || "SUCCESS"}</span>
        <strong>${item.title} | ${money(item.amount)}</strong>
        <p class="muted">${item.user?.email || "System"} | ${item.paymentMethod || item.type} ${item.provider ? `| ${item.provider}` : ""} ${item.referenceId ? `| ${item.referenceId}` : ""}</p>
        <p class="muted">${date(item.createdAt)}</p>
      </div>
    </div>
  `).join("") || `<p class="muted">No transactions yet.</p>`;
}

function renderActivity(items) {
  $("activity").innerHTML = items.map((item) => `
    <div class="activity-item">
      <strong>${item.title}</strong>
      <p class="muted">${item.user?.email || "System"} | ${item.type} | ${money(item.amount)} | ${date(item.createdAt)}</p>
    </div>
  `).join("") || `<p class="muted">No recent activity.</p>`;
}

async function addBalance(event, userId) {
  event.preventDefault();
  const amount = Number(new FormData(event.currentTarget).get("amount"));
  await api(`/api/admin/users/${userId}/add-balance`, {
    method: "POST",
    body: JSON.stringify({ amount })
  });
  event.currentTarget.reset();
  await loadDashboard();
}

async function reviewWithdrawal(id, status) {
  await api(`/api/admin/withdrawals/${id}/review`, {
    method: "PATCH",
    body: JSON.stringify({ status })
  });
  await loadDashboard();
}

async function replySupport(event, userId) {
  event.preventDefault();
  const body = new FormData(event.currentTarget).get("body");
  await api(`/api/admin/support/${userId}/reply`, {
    method: "POST",
    body: JSON.stringify({ body })
  });
  event.currentTarget.reset();
  await loadDashboard();
}

$("loginForm").addEventListener("submit", login);
$("logoutButton").addEventListener("click", logout);
$("userSearch").addEventListener("input", () => loadDashboard().catch(console.error));
$("supportSearch").addEventListener("input", () => loadDashboard().catch(console.error));
$("smsSearch").addEventListener("input", () => loadDashboard().catch(console.error));

window.addBalance = addBalance;
window.reviewWithdrawal = reviewWithdrawal;
window.replySupport = replySupport;

if (state.token) {
  setLoggedIn(true);
  loadDashboard().catch(() => logout());
} else {
  setLoggedIn(false);
}

if (window.io) {
  const socket = io();
  ["sms_created", "support_message_created", "support_message_replied", "wallet_updated", "withdrawal_created", "withdrawal_updated", "investment_created", "transaction_created"].forEach((eventName) => {
    socket.on(eventName, () => loadDashboard().catch(console.error));
  });
}
