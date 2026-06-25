<?php
require_once __DIR__ . '/config.php';

// ── CORS ──────────────────────────────────────────────────────────────────────
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Content-Type: application/json');
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') exit(0);

// ── DB ────────────────────────────────────────────────────────────────────────
function db(): PDO {
    static $pdo = null;
    if ($pdo === null) {
        $pdo = new PDO(
            'mysql:host=' . DB_HOST . ';port=' . DB_PORT . ';dbname=' . DB_NAME . ';charset=utf8mb4',
            DB_USER, DB_PASS,
            [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
             PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC]
        );
    }
    return $pdo;
}

// ── Response helpers ──────────────────────────────────────────────────────────
function ok($data, string $msg = 'OK', int $code = 200): void {
    http_response_code($code);
    echo json_encode(['success' => true, 'message' => $msg, 'data' => $data]);
    exit;
}
function err(string $msg, int $code = 400): void {
    http_response_code($code);
    echo json_encode(['success' => false, 'error' => $msg]);
    exit;
}

// ── JWT ───────────────────────────────────────────────────────────────────────
function b64u(string $d): string { return rtrim(strtr(base64_encode($d), '+/', '-_'), '='); }
function b64d(string $d): string { return base64_decode(strtr($d, '-_', '+/') . str_repeat('=', 3 - (3 + strlen($d)) % 4)); }

function jwtEncode(array $payload): string {
    $h = b64u(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
    $p = b64u(json_encode($payload));
    $s = b64u(hash_hmac('sha256', "$h.$p", JWT_SECRET, true));
    return "$h.$p.$s";
}
function jwtDecode(string $token): ?array {
    $parts = explode('.', $token);
    if (count($parts) !== 3) return null;
    [$h, $p, $s] = $parts;
    if (!hash_equals(b64u(hash_hmac('sha256', "$h.$p", JWT_SECRET, true)), $s)) return null;
    $data = json_decode(b64d($p), true);
    if (isset($data['exp']) && $data['exp'] < time()) return null;
    return $data;
}

// ── Auth middleware ───────────────────────────────────────────────────────────
function auth(): array {
    $h = getallheaders()['Authorization'] ?? '';
    if (!preg_match('/^Bearer\s+(.+)$/', $h, $m)) err('Unauthorized', 401);
    $payload = jwtDecode($m[1]);
    if (!$payload) err('Invalid or expired token', 401);
    return $payload;
}

// ── ELO update ────────────────────────────────────────────────────────────────
function updateElo(array $game, string $result): void {
    if (!$game['black_player_id']) return;
    $db = db();
    $st = $db->prepare('SELECT id, elo FROM players WHERE id = ?');

    $st->execute([$game['white_player_id']]); $w = $st->fetch();
    $st->execute([$game['black_player_id']]);  $b = $st->fetch();

    $K = 32;
    $ew = 1 / (1 + pow(10, ($b['elo'] - $w['elo']) / 400));
    $sw = match($result) { 'white' => 1.0, 'draw' => 0.5, default => 0.0 };

    $st = $db->prepare('UPDATE players SET elo = ? WHERE id = ?');
    $st->execute([max(100, (int)round($w['elo'] + $K * ($sw - $ew))), $w['id']]);
    $st->execute([max(100, (int)round($b['elo'] + $K * ((1 - $sw) - (1 - $ew)))), $b['id']]);
}

// ── Router ────────────────────────────────────────────────────────────────────
$method = $_SERVER['REQUEST_METHOD'];
$uri    = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$uri    = '/' . trim(preg_replace('#.*?/api#', '', $uri, 1), '/');

if ($method === 'POST' && $uri === '/auth/register') {
    require __DIR__ . '/api/auth.php'; handleRegister();

} elseif ($method === 'POST' && $uri === '/auth/login') {
    require __DIR__ . '/api/auth.php'; handleLogin();

} elseif ($method === 'GET' && $uri === '/games') {
    require __DIR__ . '/api/games.php'; listGames();

} elseif ($method === 'POST' && $uri === '/games') {
    require __DIR__ . '/api/games.php'; createGame();

} elseif ($method === 'GET' && preg_match('#^/games/(\d+)$#', $uri, $m)) {
    require __DIR__ . '/api/games.php'; getGame((int)$m[1]);

} elseif ($method === 'POST' && preg_match('#^/games/(\d+)/join$#', $uri, $m)) {
    require __DIR__ . '/api/games.php'; joinGame((int)$m[1]);

} elseif ($method === 'POST' && preg_match('#^/games/(\d+)/moves$#', $uri, $m)) {
    require __DIR__ . '/api/games.php'; playMove((int)$m[1]);

} elseif ($method === 'POST' && preg_match('#^/games/(\d+)/resign$#', $uri, $m)) {
    require __DIR__ . '/api/games.php'; resign((int)$m[1]);

} elseif ($method === 'GET' && $uri === '/players') {
    require __DIR__ . '/api/players.php'; listPlayers();

} elseif ($method === 'GET' && $uri === '/players/me') {
    require __DIR__ . '/api/players.php'; getMe();

} elseif ($method === 'GET' && preg_match('#^/players/(\d+)/profile$#', $uri, $m)) {
    require __DIR__ . '/api/players.php'; getProfile((int)$m[1]);

} elseif ($method === 'GET' && preg_match('#^/players/(\d+)/history$#', $uri, $m)) {
    require __DIR__ . '/api/players.php'; getHistory((int)$m[1]);

} else {
    err('Not found', 404);
}
