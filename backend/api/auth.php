<?php

function handleRegister(): void {
    $body     = json_decode(file_get_contents('php://input'), true) ?? [];
    $username = trim($body['username'] ?? '');
    $email    = trim($body['email'] ?? '');
    $password = $body['password'] ?? '';

    if (strlen($username) < 3 || strlen($username) > 50) err('Username must be 3–50 characters');
    if (!filter_var($email, FILTER_VALIDATE_EMAIL))      err('Invalid email address');
    if (strlen($password) < 6)                           err('Password must be at least 6 characters');

    $db = db();
    $st = $db->prepare('SELECT id FROM players WHERE username = ? OR email = ?');
    $st->execute([$username, $email]);
    if ($st->fetch()) err('Username or email already taken');

    $st = $db->prepare('INSERT INTO players (username, email, password_hash) VALUES (?, ?, ?)');
    $st->execute([$username, $email, password_hash($password, PASSWORD_BCRYPT)]);
    $id = (int)$db->lastInsertId();

    $token = jwtEncode(['sub' => $id, 'username' => $username, 'iat' => time(), 'exp' => time() + JWT_EXPIRY]);
    ok(['token' => $token, 'player' => ['id' => $id, 'username' => $username, 'email' => $email, 'elo' => 1200]], 'Registered', 201);
}

function handleLogin(): void {
    $body     = json_decode(file_get_contents('php://input'), true) ?? [];
    $email    = trim($body['email'] ?? '');
    $password = $body['password'] ?? '';

    if (!$email || !$password) err('Email and password are required');

    $db = db();
    $st = $db->prepare('SELECT id, username, email, password_hash, elo FROM players WHERE email = ?');
    $st->execute([$email]);
    $player = $st->fetch();

    if (!$player || !password_verify($password, $player['password_hash'])) err('Invalid credentials', 401);

    $token = jwtEncode([
        'sub'      => (int)$player['id'],
        'username' => $player['username'],
        'iat'      => time(),
        'exp'      => time() + JWT_EXPIRY,
    ]);
    ok([
        'token'  => $token,
        'player' => ['id' => (int)$player['id'], 'username' => $player['username'], 'email' => $player['email'], 'elo' => (int)$player['elo']],
    ], 'Login successful');
}
