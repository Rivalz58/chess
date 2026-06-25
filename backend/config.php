<?php
// ── Database ──────────────────────────────────────────────────────────────────
define('DB_HOST', '127.0.0.1');
define('DB_PORT', '3307');
define('DB_NAME', 'chess_db');
define('DB_USER', 'root');
define('DB_PASS', '');          // change for production

// ── JWT ───────────────────────────────────────────────────────────────────────
define('JWT_SECRET', 'change-this-secret-in-production-32chars+');
define('JWT_EXPIRY', 86400 * 7); // 7 days
