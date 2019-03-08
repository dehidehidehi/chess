package net.marvk.chess.board;

import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import net.marvk.chess.util.Stopwatch;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public abstract class AlphaBetaPlayer extends Player {
    private static final int MAX_DEPTH = 5;

    public AlphaBetaPlayer(final Color color) {
        super(color);
    }

    @Override
    public Move play(final MoveResult previousMove) {
        final AtomicReference<Move> move = new AtomicReference<>();
        final Duration duration = Stopwatch.time(() -> move.set(startExploration(previousMove)));

        log.info("Player calculated move in " + duration);

        return move.get();
    }

    private Move startExploration(final MoveResult current) {
        return alphaBeta(current, Integer.MIN_VALUE, Integer.MAX_VALUE, 0).moveResult.getMove();
    }

    private Pair alphaBeta(final MoveResult current, int alpha, int beta, final int depth) {
        if (depth == MAX_DEPTH) {
            return new Pair(current);
        }

        final List<MoveResult> validMoves = current.getBoard().getValidMoves(getColor());

        if (validMoves.isEmpty()) {
            return new Pair(current);
        }

        final boolean maximise = current.getBoard().getState().getActivePlayer() == getColor();

        int value = maximise ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        MoveResult best = null;

        for (final MoveResult move : validMoves) {
            final Pair pair = alphaBeta(move, alpha, beta, depth + 1);

            if (maximise) {
                if (pair.score > value) {
                    value = pair.score;
                    best = move;
                }

                alpha = Math.max(alpha, value);
            } else {
                if (pair.score < value) {
                    value = pair.score;
                    best = move;
                }

                beta = Math.min(beta, value);
            }

            if (beta <= alpha) {
                break;
            }
        }

        return new Pair(best, value);
    }

    @ToString
    private class Pair {
        private final MoveResult moveResult;
        private final int score;

        Pair(final MoveResult moveResult) {
            this(moveResult, heuristic(moveResult.getBoard()));
        }

        Pair(final MoveResult moveResult, final int score) {
            this.moveResult = moveResult;
            this.score = score;
        }
    }

    protected abstract int heuristic(final Board board);
}
