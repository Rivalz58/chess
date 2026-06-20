<?php

// ── Helpers ───────────────────────────────────────────────────────────────────

function fetchGame(int $id): array {
    $st = db()->prepare('
        SELECT g.id, g.white_player_id, g.black_player_id, g.status, g.result,
               g.created_at, g.finished_at,
               wp.username AS white_username, wp.elo AS white_elo,
               bp.username AS black_username, bp.elo AS black_elo
        FROM games g
        JOIN players wp ON g.white_player_id = wp.id
        LEFT JOIN players bp ON g.black_player_id = bp.id
        WHERE g.id = ?
    ');
    $st->execute([$id]);
    $g = $st->fetch();
    if (!$g) err('Game not found', 404);
    return $g;
}

function formatGame(array $g): array {
    return [
        'id'           => (int)$g['id'],
        'status'       => $g['status'],
        'result'       => $g['result'],
        'created_at'   => $g['created_at'],
        'finished_at'  => $g['finished_at'],
        'white_player' => ['id' => (int)$g['white_player_id'], 'username' => $g['white_username'], 'elo' => (int)$g['white_elo']],
        'black_player' => $g['black_player_id']
            ? ['id' => (int)$g['black_player_id'], 'username' => $g['black_username'], 'elo' => (int)$g['black_elo']]
            : null,
    ];
}

// ── Handlers ──────────────────────────────────────────────────────────────────

function listGames(): void {
    $status  = in_array($_GET['status'] ?? '', ['waiting','active','finished']) ? $_GET['status'] : 'waiting';
    $db      = db();
    $st      = $db->prepare('
        SELECT g.id, g.white_player_id, g.black_player_id, g.status, g.result,
               g.created_at, g.finished_at,
               wp.username AS white_username, wp.elo AS white_elo,
               bp.username AS black_username, bp.elo AS black_elo
        FROM games g
        JOIN players wp ON g.white_player_id = wp.id
        LEFT JOIN players bp ON g.black_player_id = bp.id
        WHERE g.status = ?
        ORDER BY g.created_at DESC
        LIMIT 50
    ');
    $st->execute([$status]);
    ok(array_map('formatGame', $st->fetchAll()));
}

function createGame(): void {
    $session = auth();
    $db      = db();
    $st      = $db->prepare('INSERT INTO games (white_player_id) VALUES (?)');
    $st->execute([$session['sub']]);
    $game = fetchGame((int)$db->lastInsertId());
    ok(formatGame($game), 'Game created', 201);
}

function getGame(int $id): void {
    $game  = fetchGame($id);
    $since = isset($_GET['since']) ? (int)$_GET['since'] : 0;

    $st = db()->prepare('
        SELECT move_number, from_row, from_col, to_row, to_col, promotion, played_at
        FROM game_moves
        WHERE game_id = ? AND move_number > ?
        ORDER BY move_number ASC
    ');
    $st->execute([$id, $since]);
    $moves = array_map(fn($m) => [
        'move_number' => (int)$m['move_number'],
        'from_row'    => (int)$m['from_row'],
        'from_col'    => (int)$m['from_col'],
        'to_row'      => (int)$m['to_row'],
        'to_col'      => (int)$m['to_col'],
        'promotion'   => $m['promotion'] !== null ? (int)$m['promotion'] : null,
        'played_at'   => $m['played_at'],
    ], $st->fetchAll());

    ok(array_merge(formatGame($game), ['moves' => $moves]));
}

function joinGame(int $id): void {
    $session = auth();
    $game    = fetchGame($id);

    if ($game['status'] !== 'waiting')                     err('Game is not available to join');
    if ((int)$game['white_player_id'] === $session['sub']) err('You cannot join your own game');

    $st = db()->prepare('UPDATE games SET black_player_id = ?, status = "active" WHERE id = ?');
    $st->execute([$session['sub'], $id]);
    ok(['game_id' => $id], 'Joined game');
}

function playMove(int $id): void {
    $session = auth();
    $body    = json_decode(file_get_contents('php://input'), true) ?? [];
    $game    = fetchGame($id);

    if ($game['status'] !== 'active') err('Game is not active');

    // Determine whose turn it is
    $st = db()->prepare('SELECT COUNT(*) AS cnt FROM game_moves WHERE game_id = ?');
    $st->execute([$id]);
    $cnt      = (int)$st->fetch()['cnt'];
    $isWhite  = ($cnt % 2 === 0);
    $expected = $isWhite ? (int)$game['white_player_id'] : (int)$game['black_player_id'];

    if ($session['sub'] !== $expected) err('Not your turn');

    $fromRow   = isset($body['from_row'])  ? (int)$body['from_row']  : null;
    $fromCol   = isset($body['from_col'])  ? (int)$body['from_col']  : null;
    $toRow     = isset($body['to_row'])    ? (int)$body['to_row']    : null;
    $toCol     = isset($body['to_col'])    ? (int)$body['to_col']    : null;
    $promotion = isset($body['promotion']) ? (int)$body['promotion'] : null;

    if ($fromRow === null || $fromCol === null || $toRow === null || $toCol === null) {
        err('Missing fields: from_row, from_col, to_row, to_col');
    }
    foreach ([$fromRow, $fromCol, $toRow, $toCol] as $v) {
        if ($v < 0 || $v > 7) err('Board coordinates must be 0–7');
    }

    $moveNumber = $cnt + 1;
    $st = db()->prepare('
        INSERT INTO game_moves (game_id, move_number, from_row, from_col, to_row, to_col, promotion)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    ');
    $st->execute([$id, $moveNumber, $fromRow, $fromCol, $toRow, $toCol, $promotion]);

    // End the game if the client reports checkmate or draw (stalemate)
    $result = $body['result'] ?? null; // 'white' | 'black' | 'draw'
    if ($result && in_array($result, ['white', 'black', 'draw'])) {
        $st = db()->prepare('UPDATE games SET status = "finished", result = ?, finished_at = NOW() WHERE id = ?');
        $st->execute([$result, $id]);
        updateElo($game, $result);
    }

    ok(['move_number' => $moveNumber], 'Move played');
}

function resign(int $id): void {
    $session = auth();
    $game    = fetchGame($id);

    if ($game['status'] !== 'active') err('Game is not active');

    $isWhite = ((int)$game['white_player_id'] === $session['sub']);
    $isBlack = ((int)$game['black_player_id'] === $session['sub']);
    if (!$isWhite && !$isBlack) err('You are not a player in this game', 403);

    $result = $isWhite ? 'black' : 'white';
    $st     = db()->prepare('UPDATE games SET status = "finished", result = ?, finished_at = NOW() WHERE id = ?');
    $st->execute([$result, $id]);
    updateElo($game, $result);
    ok(null, 'Resigned');
}
