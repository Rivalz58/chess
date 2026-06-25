<?php

function fetchPlayerProfile(int $id): array {
    $st = db()->prepare('
        SELECT p.id, p.username, p.email, p.elo, p.created_at,
            (SELECT COUNT(*) FROM games
             WHERE (white_player_id = p.id OR black_player_id = p.id) AND status = "finished") AS total,
            (SELECT COUNT(*) FROM games
             WHERE ((white_player_id = p.id AND result = "white")
                 OR (black_player_id = p.id AND result = "black"))
               AND status = "finished") AS wins,
            (SELECT COUNT(*) FROM games
             WHERE (white_player_id = p.id OR black_player_id = p.id)
               AND result = "draw" AND status = "finished") AS draws
        FROM players p WHERE p.id = ?
    ');
    $st->execute([$id]);
    $p = $st->fetch();
    if (!$p) err('Player not found', 404);

    $total  = (int)$p['total'];
    $wins   = (int)$p['wins'];
    $draws  = (int)$p['draws'];
    return [
        'id'         => (int)$p['id'],
        'username'   => $p['username'],
        'elo'        => (int)$p['elo'],
        'created_at' => $p['created_at'],
        'stats'      => [
            'total'  => $total,
            'wins'   => $wins,
            'draws'  => $draws,
            'losses' => $total - $wins - $draws,
        ],
    ];
}

function listPlayers(): void {
    $st = db()->query('SELECT id, username, elo, created_at FROM players ORDER BY elo DESC LIMIT 200');
    ok(array_map(fn($p) => [
        'id'         => (int)$p['id'],
        'username'   => $p['username'],
        'elo'        => (int)$p['elo'],
        'created_at' => $p['created_at'],
    ], $st->fetchAll()));
}

function getMe(): void {
    $session = auth();
    ok(fetchPlayerProfile($session['sub']));
}

function getProfile(int $id): void {
    ok(fetchPlayerProfile($id));
}

function getHistory(int $id): void {
    $st = db()->prepare('SELECT id FROM players WHERE id = ?');
    $st->execute([$id]);
    if (!$st->fetch()) err('Player not found', 404);

    $st = db()->prepare('
        SELECT g.id, g.result, g.created_at, g.finished_at,
               wp.id AS white_id, wp.username AS white_username, wp.elo AS white_elo,
               bp.id AS black_id, bp.username AS black_username, bp.elo AS black_elo,
               (SELECT COUNT(*) FROM game_moves WHERE game_id = g.id) AS move_count
        FROM games g
        JOIN players wp ON g.white_player_id = wp.id
        LEFT JOIN players bp ON g.black_player_id = bp.id
        WHERE (g.white_player_id = ? OR g.black_player_id = ?) AND g.status = "finished"
        ORDER BY g.finished_at DESC
        LIMIT 50
    ');
    $st->execute([$id, $id]);

    ok(array_map(fn($g) => [
        'id'           => (int)$g['id'],
        'result'       => $g['result'],
        'created_at'   => $g['created_at'],
        'finished_at'  => $g['finished_at'],
        'move_count'   => (int)$g['move_count'],
        'white_player' => ['id' => (int)$g['white_id'], 'username' => $g['white_username'], 'elo' => (int)$g['white_elo']],
        'black_player' => $g['black_id']
            ? ['id' => (int)$g['black_id'], 'username' => $g['black_username'], 'elo' => (int)$g['black_elo']]
            : null,
    ], $st->fetchAll()));
}
