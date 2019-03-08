package net.marvk.chess.board;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomPlayer extends Player {
    public RandomPlayer(final Color color) {
        super(color);
    }

    @Override
    public Move play(final MoveResult previousMove) {
        final List<MoveResult> validMoves = previousMove.getBoard().getValidMoves(getColor());

        final int index = ThreadLocalRandom.current().nextInt(validMoves.size());

        return validMoves.get(index).getMove();
    }
}
