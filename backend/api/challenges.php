<?php

require_once __DIR__ . '/games.php';

function fetchChallenge(int $id): array {
    $st = db()->prepare('
        SELECT c.id, c.challenger_id, c.challenged_id, c.status, c.game_id, c.created_at,
               cp.username AS challenger_username, cp.elo AS challenger_elo
        FROM challenges c
        JOIN players cp ON c.challenger_id = cp.id
        WHERE c.id = ?
    ');
    $st->execute([$id]);
    $c = $st->fetch();
    if (!$c) err('Challenge not found', 404);
    return $c;
}

function formatChallenge(array $c): array {
    return [
        'id'                  => (int)$c['id'],
        'challenger_id'       => (int)$c['challenger_id'],
        'challenged_id'       => (int)$c['challenged_id'],
        'status'              => $c['status'],
        'game_id'             => $c['game_id'] !== null ? (int)$c['game_id'] : null,
        'created_at'          => $c['created_at'],
        'challenger_username' => $c['challenger_username'],
        'challenger_elo'      => (int)$c['challenger_elo'],
    ];
}

function createChallenge(): void {
    $session = auth();
    $body    = json_decode(file_get_contents('php://input'), true) ?? [];
    $challengedId = isset($body['challenged_id']) ? (int)$body['challenged_id'] : null;

    if (!$challengedId) err('Missing field: challenged_id');
    if ($challengedId === $session['sub']) err('You cannot challenge yourself');

    $db = db();
    $st = $db->prepare('SELECT id FROM players WHERE id = ?');
    $st->execute([$challengedId]);
    if (!$st->fetch()) err('Player not found', 404);

    $st = $db->prepare("SELECT id FROM challenges WHERE status = 'pending'
                         AND ((challenger_id = ? AND challenged_id = ?) OR (challenger_id = ? AND challenged_id = ?))");
    $st->execute([$session['sub'], $challengedId, $challengedId, $session['sub']]);
    if ($st->fetch()) err('A challenge is already pending between you two');

    $st = $db->prepare('INSERT INTO challenges (challenger_id, challenged_id) VALUES (?, ?)');
    $st->execute([$session['sub'], $challengedId]);

    ok(formatChallenge(fetchChallenge((int)$db->lastInsertId())), 'Challenge sent', 201);
}

function listIncoming(): void {
    $session = auth();
    $st = db()->prepare('
        SELECT c.id, c.challenger_id, c.challenged_id, c.status, c.game_id, c.created_at,
               cp.username AS challenger_username, cp.elo AS challenger_elo
        FROM challenges c
        JOIN players cp ON c.challenger_id = cp.id
        WHERE c.challenged_id = ? AND c.status = "pending"
        ORDER BY c.created_at DESC
    ');
    $st->execute([$session['sub']]);
    ok(array_map('formatChallenge', $st->fetchAll()));
}

function getChallenge(int $id): void {
    $session = auth();
    $c = fetchChallenge($id);
    if ($session['sub'] !== (int)$c['challenger_id'] && $session['sub'] !== (int)$c['challenged_id']) {
        err('You are not part of this challenge', 403);
    }
    ok(formatChallenge($c));
}

function acceptChallenge(int $id): void {
    $session = auth();
    $db = db();
    $db->beginTransaction();

    $st = $db->prepare("UPDATE challenges SET status = 'accepted' WHERE id = ? AND challenged_id = ? AND status = 'pending'");
    $st->execute([$id, $session['sub']]);
    if ($st->rowCount() !== 1) {
        $db->rollBack();
        err('Challenge is no longer pending', 409);
    }

    $st = $db->prepare('SELECT challenger_id, challenged_id FROM challenges WHERE id = ?');
    $st->execute([$id]);
    $c = $st->fetch();

    $white = mt_rand(0, 1) ? $c['challenger_id'] : $c['challenged_id'];
    $black = $white == $c['challenger_id'] ? $c['challenged_id'] : $c['challenger_id'];

    $st = $db->prepare("INSERT INTO games (white_player_id, black_player_id, status) VALUES (?, ?, 'active')");
    $st->execute([$white, $black]);
    $gameId = (int)$db->lastInsertId();

    $db->prepare('UPDATE challenges SET game_id = ? WHERE id = ?')->execute([$gameId, $id]);
    $db->commit();

    ok(['game' => formatGame(fetchGame($gameId)), 'challenge_id' => $id], 'Challenge accepted');
}

function declineChallenge(int $id): void {
    $session = auth();
    $db = db();

    $st = $db->prepare("UPDATE challenges SET status = 'declined' WHERE id = ? AND challenged_id = ? AND status = 'pending'");
    $st->execute([$id, $session['sub']]);
    if ($st->rowCount() !== 1) err('Challenge is no longer pending', 409);

    ok(null, 'Challenge declined');
}
