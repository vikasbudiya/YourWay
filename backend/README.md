# YourWay Backend

Node.js, Express, MongoDB, and Socket.IO backend for the YourWay demo platform.

This backend is intentionally demo-only. It stores simulated wallet balances, simulated painting investments, SMS logs, withdrawal requests, transactions, and notifications. It does not collect real payment credentials, OTP secrets, UPI PINs, card PINs, or banking passwords.

## Run Locally

```bash
cd backend
npm install
cp .env.example .env
npm run dev
```

Admin dashboard:

```text
http://localhost:4000/admin
```

Legacy Android SMS endpoint remains compatible:

```text
POST /sms
```

Authenticated API routes live under:

```text
/api
```

Support chat endpoints:

```text
GET /api/support/messages
POST /api/support/messages
GET /api/admin/support
POST /api/admin/support/:userId/reply
```

Socket.IO emits `support_message_created` when a user sends a message and `support_message_replied` when admin replies.
