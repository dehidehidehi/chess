package net.marvk.chess.core.bitboards;

import lombok.EqualsAndHashCode;
import net.marvk.chess.core.board.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Bitboard {
    // region Constants
    //     _____ ____  _   _  _____ _______       _   _ _______ _____
    //    / ____/ __ \| \ | |/ ____|__   __|/\   | \ | |__   __/ ____|
    //   | |   | |  | |  \| | (___    | |  /  \  |  \| |  | | | (___
    //   | |   | |  | | . ` |\___ \   | | / /\ \ | . ` |  | |  \___ \
    //   | |___| |__| | |\  |____) |  | |/ ____ \| |\  |  | |  ____) |
    //    \_____\____/|_| \_|_____/   |_/_/    \_\_| \_|  |_| |_____/

    private static final Square[] SQUARES;
    private static final long[] KNIGHT_ATTACKS;
    private static final long[] KING_ATTACKS;

    private static final long[] WHITE_PAWN_ATTACKS;
    private static final long[] BLACK_PAWN_ATTACKS;

    private static final int KING_VALUE = 0;
    private static final int QUEEN_VALUE = 900;
    private static final int ROOK_VALUE = 500;
    private static final int BISHOP_VALUE = 330;
    private static final int KNIGHT_VALUE = 320;
    private static final int PAWN_VALUE = 100;

    static {
        SQUARES = new Square[64];

        for (final Square square : Square.values()) {
            SQUARES[square.getBitboardIndex()] = square;
        }

        KNIGHT_ATTACKS = new long[64];

        for (final Square square : SQUARES) {
            KNIGHT_ATTACKS[square.getBitboardIndex()] = staticAttacks(Direction.KNIGHT_DIRECTIONS, square);
        }

        KING_ATTACKS = new long[64];

        for (final Square square : SQUARES) {
            KING_ATTACKS[square.getBitboardIndex()] = staticAttacks(Direction.CARDINAL_DIRECTIONS, square);
        }

        WHITE_PAWN_ATTACKS = new long[64];

        for (final Square square : SQUARES) {
            WHITE_PAWN_ATTACKS[square.getBitboardIndex()] = staticAttacks(List.of(Direction.NORTH_WEST, Direction.NORTH_EAST), square);
        }

        BLACK_PAWN_ATTACKS = new long[64];

        for (final Square square : SQUARES) {
            BLACK_PAWN_ATTACKS[square.getBitboardIndex()] = staticAttacks(List.of(Direction.SOUTH_WEST, Direction.SOUTH_EAST), square);
        }
    }

    private static final int[] BLACK_KING_TABLE_LATE = {
            -50, -40, -30, -20, -20, -30, -40, -50,
            -30, -20, -10, 0, 0, -10, -20, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -30, 0, 0, 0, 0, -30, -30,
            -50, -30, -30, -30, -30, -30, -30, -50,
    };

    private static final int[] BLACK_KING_TABLE_EARLY = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, 0, 0, 0, 0, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20,
    };

    private static final int[] BLACK_QUEEN_TABLE = {
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -5, 0, 5, 5, 5, 5, 0, -5,
            0, 0, 5, 5, 5, 5, 0, -5,
            -10, 5, 5, 5, 5, 5, 0, -10,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20,
    };

    private static final int[] BLACK_ROOK_TABLE = {
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            0, 0, 0, 5, 5, 0, 0, 0,
    };

    private static final int[] BLACK_BISHOP_TABLE = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20,
    };

    private static final int[] BLACK_KNIGHT_TABLE = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50,
    };

    private static final int[] BLACK_PAWN_TABLE = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0,
    };

    private static final int[] WHITE_KING_TABLE_LATE;
    private static final int[] WHITE_KING_TABLE_EARLY;
    private static final int[] WHITE_QUEEN_TABLE;
    private static final int[] WHITE_ROOK_TABLE;
    private static final int[] WHITE_BISHOP_TABLE;
    private static final int[] WHITE_KNIGHT_TABLE;
    private static final int[] WHITE_PAWN_TABLE;

    static {
        WHITE_KING_TABLE_LATE = mirror(BLACK_KING_TABLE_LATE);
        WHITE_KING_TABLE_EARLY = mirror(BLACK_KING_TABLE_EARLY);
        WHITE_QUEEN_TABLE = mirror(BLACK_QUEEN_TABLE);
        WHITE_ROOK_TABLE = mirror(BLACK_ROOK_TABLE);
        WHITE_BISHOP_TABLE = mirror(BLACK_BISHOP_TABLE);
        WHITE_KNIGHT_TABLE = mirror(BLACK_KNIGHT_TABLE);
        WHITE_PAWN_TABLE = mirror(BLACK_PAWN_TABLE);
    }

    private static int[] mirror(final int[] inputArray) {
        return IntStream.range(0, 8)
                        .flatMap(i -> IntStream.range(0, 8).map(j -> inputArray[8 * (8 - i - 1) + j]))
                        .map(i -> -i)
                        .toArray();
    }

    private static final long WHITE_QUEEN_SIDE_CASTLE_OCCUPANCY = bitwiseOr(Square.B1, Square.C1, Square.D1);
    private static final long WHITE_KING_SIDE_CASTLE_OCCUPANCY = bitwiseOr(Square.F1, Square.G1);
    private static final long BLACK_QUEEN_SIDE_CASTLE_OCCUPANCY = bitwiseOr(Square.B8, Square.C8, Square.D8);
    private static final long BLACK_KING_SIDE_CASTLE_OCCUPANCY = bitwiseOr(Square.F8, Square.G8);

    private static final long RANK_ONE_SQUARES = getRankSquares(Rank.RANK_1);
    private static final long RANK_TWO_SQUARES = getRankSquares(Rank.RANK_2);

    private static final long RANK_SEVEN_SQUARES = getRankSquares(Rank.RANK_7);
    private static final long RANK_EIGHT_SQUARES = getRankSquares(Rank.RANK_8);

    private static long bitwiseOr(final Square... squares) {
        return Arrays.stream(squares).mapToLong(Square::getOccupiedBitMask).reduce(0L, (l1, l2) -> l1 | l2);
    }

    private static long getRankSquares(final Rank rank) {
        return Arrays.stream(SQUARES)
                     .filter(s -> s.getRank() == rank)
                     .mapToLong(Square::getOccupiedBitMask)
                     .reduce(0L, (l1, l2) -> l1 | l2);
    }

    private static long staticAttacks(final Collection<Direction> directions, final Square square) {
        return directions.stream()
                         .map(square::translate)
                         .filter(Objects::nonNull)
                         .mapToLong(Square::getOccupiedBitMask)
                         .reduce(0L, (l1, l2) -> l1 | l2);
    }

    // endregion

    // region Initialization
    //    _____ _   _ _____ _______ _____          _      _____ ______      _______ _____ ____  _   _
    //   |_   _| \ | |_   _|__   __|_   _|   /\   | |    |_   _|___  /   /\|__   __|_   _/ __ \| \ | |
    //     | | |  \| | | |    | |    | |    /  \  | |      | |    / /   /  \  | |    | || |  | |  \| |
    //     | | | . ` | | |    | |    | |   / /\ \ | |      | |   / /   / /\ \ | |    | || |  | | . ` |
    //    _| |_| |\  |_| |_   | |   _| |_ / ____ \| |____ _| |_ / /__ / ____ \| |   _| || |__| | |\  |
    //   |_____|_| \_|_____|  |_|  |_____/_/    \_\______|_____/_____/_/    \_\_|  |_____\____/|_| \_|

    private final PlayerBoard black;
    private final PlayerBoard white;

    private Color turn;
    private long enPassant = 0L;

    private int fullmoveClock;
    private int halfmoveClock;

//    private long zobristHash;

    /**
     * Copy constructor
     *
     * @param previous the board to be copied
     */
    private Bitboard(final Bitboard previous) {
        this.white = new PlayerBoard(previous.white);
        this.black = new PlayerBoard(previous.black);

        this.turn = previous.turn;
        this.enPassant = previous.enPassant;

        this.fullmoveClock = previous.fullmoveClock;
        this.halfmoveClock = previous.halfmoveClock;

//        this.zobristHash = zobristHash();
    }

    public Bitboard(final Fen fen) {
        this.white = new PlayerBoard();
        this.black = new PlayerBoard();

        turn = Color.getColorFromFen(fen.getActiveColor());
        halfmoveClock = Integer.parseInt(fen.getHalfmoveClock());
        fullmoveClock = Integer.parseInt(fen.getFullmoveClock());

        fen.getCastlingAvailability().chars().forEach(e -> {
            if (e == 'K') {
                white.kingSideCastle = true;
            } else if (e == 'Q') {
                white.queenSideCastle = true;
            } else if (e == 'k') {
                black.kingSideCastle = true;
            } else if (e == 'q') {
                black.queenSideCastle = true;
            }
        });

        final Square squareFromFen = Square.getSquareFromFen(fen.getEnPassantTargetSquare());

        if (squareFromFen != null) {
            enPassant = squareFromFen.getOccupiedBitMask();
        }

        loadFen(fen);

//        this.zobristHash = zobristHash();
    }

    private void loadFen(final Fen fen) {
        final String[] split = fen.getPiecePlacement().split("/");

        for (int i = 0; i < split.length; i++) {
            final String line = split[8 - i - 1];

            for (int j = 0, lineIndex = 0; j < line.length(); j++) {
                final char c = line.charAt(j);

                if (Character.isDigit(c)) {
                    lineIndex += c - '0';
                    continue;
                }

                final int index = i * 8 + lineIndex;

                final long shift = 1L << index;

                switch (c) {
                    case 'k':
                        black.kings |= shift;
                        break;
                    case 'K':
                        white.kings |= shift;
                        break;
                    case 'q':
                        black.queens |= shift;
                        break;
                    case 'Q':
                        white.queens |= shift;
                        break;
                    case 'r':
                        black.rooks |= shift;
                        break;
                    case 'R':
                        white.rooks |= shift;
                        break;
                    case 'b':
                        black.bishops |= shift;
                        break;
                    case 'B':
                        white.bishops |= shift;
                        break;
                    case 'n':
                        black.knights |= shift;
                        break;
                    case 'N':
                        white.knights |= shift;
                        break;
                    case 'p':
                        black.pawns |= shift;
                        break;
                    case 'P':
                        white.pawns |= shift;
                        break;
                }

                lineIndex++;
            }
        }
    }

    // endregion

    // region State Accessors
    //     _____ _______    _______ ______            _____ _____ ______  _____ _____  ____  _____   _____
    //    / ____|__   __|/\|__   __|  ____|     /\   / ____/ ____|  ____|/ ____/ ____|/ __ \|  __ \ / ____|
    //   | (___    | |  /  \  | |  | |__       /  \ | |   | |    | |__  | (___| (___ | |  | | |__) | (___
    //    \___ \   | | / /\ \ | |  |  __|     / /\ \| |   | |    |  __|  \___ \\___ \| |  | |  _  / \___ \
    //    ____) |  | |/ ____ \| |  | |____   / ____ \ |___| |____| |____ ____) |___) | |__| | | \ \ ____) |
    //   |_____/   |_/_/    \_\_|  |______| /_/    \_\_____\_____|______|_____/_____/ \____/|_|  \_\_____/

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveClock() {
        return fullmoveClock;
    }

    public Color getActivePlayer() {
        return turn;
    }

    public boolean canCastleKingSide(final Color color) {
        Objects.requireNonNull(color);

        return color == Color.WHITE ? white.kingSideCastle : black.kingSideCastle;
    }

    public boolean canCastleQueenSide(final Color color) {
        Objects.requireNonNull(color);

        return color == Color.WHITE ? white.queenSideCastle : black.queenSideCastle;
    }

    public Square getEnPassant() {
        return enPassant == 0L ? null : SQUARES[Long.numberOfTrailingZeros(enPassant)];
    }

    // endregion

    // region Move Generator
    //    __  __  ______      ________    _____ ______ _   _ ______ _____         _______ ____  _____
    //   |  \/  |/ __ \ \    / /  ____|  / ____|  ____| \ | |  ____|  __ \     /\|__   __/ __ \|  __ \
    //   | \  / | |  | \ \  / /| |__    | |  __| |__  |  \| | |__  | |__) |   /  \  | | | |  | | |__) |
    //   | |\/| | |  | |\ \/ / |  __|   | | |_ |  __| | . ` |  __| |  _  /   / /\ \ | | | |  | |  _  /
    //   | |  | | |__| | \  /  | |____  | |__| | |____| |\  | |____| | \ \  / ____ \| | | |__| | | \ \
    //   |_|  |_|\____/   \/   |______|  \_____|______|_| \_|______|_|  \_\/_/    \_\_|  \____/|_|  \_\

    public List<MoveResult> getValidMoves() {
        return getPseudoLegalMoves()
                .stream()
                .map(this::copyMake)
                .collect(Collectors.toList());
    }

    private MoveResult copyMake(final BBMove move) {
        final Bitboard copy = new Bitboard(this);

        copy.make(move);

        final Piece promotePiece = move.promote;

        final ColoredPiece promoteTo = promotePiece == null ? null : promotePiece.ofColor(turn.opposite());

        return new MoveResult(copy, new Move(SQUARES[Long.numberOfTrailingZeros(move.sourceSquare)], SQUARES[Long.numberOfTrailingZeros(move.targetSquare)], move.pieceMoved
                .ofColor(turn.opposite()), promoteTo, move.castle, move.enPassantAttack, false));
    }

    public static boolean hasAnyLegalMoves(final Bitboard board, final Collection<BBMove> moves) {
        for (final BBMove move : moves) {
            board.make(move);

            if (!board.isInvalidPosition()) {
                board.unmake(move);

                return true;
            }

            board.unmake(move);
        }

        return false;
    }

    public static boolean hasAnyAttackMoves(final Collection<BBMove> moves) {
        for (final BBMove pseudoLegalMove : moves) {
            if (pseudoLegalMove.isAttack()) {
                return true;
            }
        }

        return false;
    }

    public List<BBMove> getPseudoLegalMoves() {
        final PlayerBoard self;
        final long selfOccupancy;
        final long opponentOccupancy;

        if (turn == Color.WHITE) {
            self = white;
            selfOccupancy = white.occupancy();
            opponentOccupancy = black.occupancy();
        } else {
            self = black;
            selfOccupancy = black.occupancy();
            opponentOccupancy = white.occupancy();
        }

        final long occupancy = selfOccupancy | opponentOccupancy;

        final List<BBMove> result = new ArrayList<>();

        slidingAttacks(result, self.queens, occupancy, selfOccupancy, MagicBitboard.ROOK, Piece.QUEEN, turn);
        slidingAttacks(result, self.rooks, occupancy, selfOccupancy, MagicBitboard.ROOK, Piece.ROOK, turn);
        slidingAttacks(result, self.queens, occupancy, selfOccupancy, MagicBitboard.BISHOP, Piece.QUEEN, turn);
        slidingAttacks(result, self.bishops, occupancy, selfOccupancy, MagicBitboard.BISHOP, Piece.BISHOP, turn);
        singleAttacks(result, self.knights, selfOccupancy, KNIGHT_ATTACKS, Piece.KNIGHT, turn);
        singleAttacks(result, self.kings, selfOccupancy, KING_ATTACKS, Piece.KING, turn);
        pawnAttacks(result, self.pawns, selfOccupancy, opponentOccupancy, turn);
        pawnMoves(result, self.pawns, occupancy, turn);
        castleMoves(result, self, occupancy, turn);

        return result;
    }

    private void castleMoves(
            final List<BBMove> result,
            final PlayerBoard self,
            final long occupancy,
            final Color color
    ) {
        if (color == Color.WHITE && !isInCheck(color, Square.E1.getOccupiedBitMask(), black, occupancy)) {
            if (self.queenSideCastle
                    && (WHITE_QUEEN_SIDE_CASTLE_OCCUPANCY & occupancy) == 0L
                    && !isInCheck(color, Square.C1.getOccupiedBitMask(), black, occupancy)
                    && !isInCheck(color, Square.D1.getOccupiedBitMask(), black, occupancy)
            ) {
                result.add(makeCastleMove(Square.E1, Square.C1));
            }

            if (self.kingSideCastle
                    && (WHITE_KING_SIDE_CASTLE_OCCUPANCY & occupancy) == 0L
                    && !isInCheck(color, Square.F1.getOccupiedBitMask(), black, occupancy)
                    && !isInCheck(color, Square.G1.getOccupiedBitMask(), black, occupancy)
            ) {
                result.add(makeCastleMove(Square.E1, Square.G1));
            }
        } else if (color == Color.BLACK && !isInCheck(color, Square.E8.getOccupiedBitMask(), white, occupancy)) {
            if (self.queenSideCastle
                    && (BLACK_QUEEN_SIDE_CASTLE_OCCUPANCY & occupancy) == 0L
                    && !isInCheck(color, Square.C8.getOccupiedBitMask(), white, occupancy)
                    && !isInCheck(color, Square.D8.getOccupiedBitMask(), white, occupancy)
            ) {
                result.add(makeCastleMove(Square.E8, Square.C8));
            }

            if (self.kingSideCastle
                    && (BLACK_KING_SIDE_CASTLE_OCCUPANCY & occupancy) == 0L
                    && !isInCheck(color, Square.F8.getOccupiedBitMask(), white, occupancy)
                    && !isInCheck(color, Square.G8.getOccupiedBitMask(), white, occupancy)
            ) {
                result.add(makeCastleMove(Square.E8, Square.G8));
            }
        }
    }

    private BBMove makeCastleMove(final Square kingSource, final Square kingTarget) {
        return makeBbMove(kingSource.getOccupiedBitMask(), kingTarget.getOccupiedBitMask(), Piece.KING, true, false, null, 0L);
    }

    private void pawnMoves(
            final List<BBMove> result,
            final long pawns,
            final long fullOccupancy,
            final Color color
    ) {
        long remainingPawns = pawns;

        while (remainingPawns != 0L) {
            final long source = Long.highestOneBit(remainingPawns);
            remainingPawns &= ~source;

            final long singleMoveTarget;
            final long promoteRank;

            if (color == Color.WHITE) {
                singleMoveTarget = source << 8;
                promoteRank = RANK_EIGHT_SQUARES;
            } else {
                singleMoveTarget = source >> 8;
                promoteRank = RANK_ONE_SQUARES;
            }

            if ((singleMoveTarget & fullOccupancy) == 0L) {
                if ((singleMoveTarget & promoteRank) == 0L) {
                    //no promotion moves

                    result.add(makeBbMove(source, singleMoveTarget, Piece.PAWN, false, false, null, 0L));

                    final long doubleMoveTarget;
                    final long doubleMoveSourceRank;

                    if (color == Color.WHITE) {
                        doubleMoveTarget = singleMoveTarget << 8;
                        doubleMoveSourceRank = RANK_TWO_SQUARES;
                    } else {
                        doubleMoveTarget = singleMoveTarget >> 8;
                        doubleMoveSourceRank = RANK_SEVEN_SQUARES;
                    }

                    if ((source & doubleMoveSourceRank) != 0L && (doubleMoveTarget & fullOccupancy) == 0L) {
                        //is in starting rank and free double move target square

                        result.add(makeBbMove(source, doubleMoveTarget, Piece.PAWN, false, false, null, singleMoveTarget));
                    }
                } else {
                    pawnPromotions(result, source, singleMoveTarget, color);
                }
            }
        }
    }

    private void pawnPromotions(
            final List<BBMove> result,
            final long source,
            final long target,
            final Color color
    ) {
        if (color == Color.WHITE) {
            pawnPromotion(result, source, target, ColoredPiece.WHITE_KNIGHT);
            pawnPromotion(result, source, target, ColoredPiece.WHITE_BISHOP);
            pawnPromotion(result, source, target, ColoredPiece.WHITE_ROOK);
            pawnPromotion(result, source, target, ColoredPiece.WHITE_QUEEN);
        } else {
            pawnPromotion(result, source, target, ColoredPiece.BLACK_KNIGHT);
            pawnPromotion(result, source, target, ColoredPiece.BLACK_BISHOP);
            pawnPromotion(result, source, target, ColoredPiece.BLACK_ROOK);
            pawnPromotion(result, source, target, ColoredPiece.BLACK_QUEEN);
        }
    }

    private void pawnPromotion(
            final List<BBMove> result,
            final long source,
            final long target,
            final ColoredPiece promotionPiece
    ) {
        result.add(makeBbMove(source, target, Piece.PAWN, false, false, promotionPiece.getPiece(), 0L));
    }

    private void pawnAttacks(
            final List<BBMove> result,
            final long pawns,
            final long selfOccupancy,
            final long opponentOccupancy,
            final Color color
    ) {
        long remainingPawns = pawns;

        final long[] pawnAttacks = color == Color.WHITE ? WHITE_PAWN_ATTACKS : BLACK_PAWN_ATTACKS;

        while (remainingPawns != 0L) {
            final long source = Long.highestOneBit(remainingPawns);
            remainingPawns &= ~source;

            final long attacks = pawnAttacks[Long.numberOfTrailingZeros(source)] & (opponentOccupancy | enPassant) & ~selfOccupancy;

            generateAttacks(result, source, attacks, Piece.PAWN, color);
        }
    }

    private void singleAttacks(
            final List<BBMove> result,
            final long pieces,
            final long selfOccupancy,
            final long[] attacksArray,
            final Piece piece,
            final Color color
    ) {
        long remainingPieces = pieces;

        while (remainingPieces != 0L) {
            final long source = Long.highestOneBit(remainingPieces);
            remainingPieces &= ~source;

            final long attacks = attacksArray[Long.numberOfTrailingZeros(source)] & ~selfOccupancy;

            generateAttacks(result, source, attacks, piece, color);
        }
    }

    private void slidingAttacks(
            final List<BBMove> result,
            final long pieces,
            final long fullOccupancy,
            final long selfOccupancy,
            final MagicBitboard bitboard,
            final Piece piece,
            final Color color
    ) {
        long remainingPieces = pieces;

        while (remainingPieces != 0L) {
            final long source = Long.highestOneBit(remainingPieces);
            remainingPieces &= ~source;

            final long attacks = bitboard.attacks(fullOccupancy, Long.numberOfTrailingZeros(source)) & ~selfOccupancy;

            generateAttacks(result, source, attacks, piece, color);
        }
    }

    private void generateAttacks(
            final List<BBMove> result,
            final long source,
            final long attacks,
            final Piece piece,
            final Color color
    ) {
        long remainingAttacks = attacks;

        while (remainingAttacks != 0L) {
            final long attack = Long.highestOneBit(remainingAttacks);
            remainingAttacks &= ~attack;

            if (piece == Piece.PAWN) {
                if (color == Color.WHITE && (attack & RANK_EIGHT_SQUARES) != 0L) {
                    pawnPromotions(result, source, attack, Color.WHITE);
                    continue;
                } else if (color == Color.BLACK && (attack & RANK_ONE_SQUARES) != 0L) {
                    pawnPromotions(result, source, attack, Color.BLACK);
                    continue;
                }
            }

            if (piece == Piece.PAWN) {
                result.add(makeBbMove(source, attack, Piece.PAWN, false, attack == enPassant, null, 0L));
            } else {
                result.add(makeBbMove(source, attack, piece, false, false, null, 0L));
            }
        }
    }

    private BBMove makeBbMove(final long sourceSquare, final long targetSquare, final Piece pieceMoved, final boolean castle, final boolean enPassantAttack, final Piece promote, final long enPassantOpportunity) {
        final long attackSquare;

        if (enPassantAttack) {
            attackSquare = turn == Color.WHITE ? targetSquare >> 8L : targetSquare << 8L;
        } else {
            attackSquare = targetSquare;
        }

        final Piece pieceAttacked = turn == Color.WHITE ? black.getPiece(attackSquare) : white.getPiece(attackSquare);

        final int squareDiff = pieceSquareValue(pieceMoved, turn, Long.numberOfTrailingZeros(targetSquare)) -
                pieceSquareValue(pieceMoved, turn, Long.numberOfTrailingZeros(sourceSquare));

        final BBMove move = new BBMove(sourceSquare, targetSquare, pieceMoved, pieceAttacked, castle, enPassantAttack, promote, squareDiff);

        if (turn == Color.BLACK) {
            if (white.queenSideCastle && move.targetSquare == Square.A1.getOccupiedBitMask()) {
                move.opponentLostQueenSideCastle = true;
            } else if (white.kingSideCastle && move.targetSquare == Square.H1.getOccupiedBitMask()) {
                move.opponentLostKingSideCastle = true;
            }

            if (black.queenSideCastle && (move.sourceSquare == Square.A8.getOccupiedBitMask() || move.sourceSquare == Square.E8
                    .getOccupiedBitMask())) {
                move.selfLostQueenSideCastle = true;
            }

            if (black.kingSideCastle && (move.sourceSquare == Square.H8.getOccupiedBitMask() || move.sourceSquare == Square.E8
                    .getOccupiedBitMask())) {
                move.selfLostKingSideCastle = true;
            }
        } else {
            if (black.queenSideCastle && move.targetSquare == Square.A8.getOccupiedBitMask()) {
                move.opponentLostQueenSideCastle = true;
            } else if (black.kingSideCastle && move.targetSquare == Square.H8.getOccupiedBitMask()) {
                move.opponentLostKingSideCastle = true;
            }

            if (white.queenSideCastle && (move.sourceSquare == Square.A1.getOccupiedBitMask() || move.sourceSquare == Square.E1
                    .getOccupiedBitMask())) {
                move.selfLostQueenSideCastle = true;
            }

            if (white.kingSideCastle && (move.sourceSquare == Square.H1.getOccupiedBitMask() || move.sourceSquare == Square.E1
                    .getOccupiedBitMask())) {
                move.selfLostKingSideCastle = true;
            }
        }

        if (move.pieceMoved == Piece.PAWN || move.pieceAttacked != null) {
            move.nextHalfmove = 0;
        } else {
            move.nextHalfmove = halfmoveClock + 1;
        }

        move.previousEnPassantSquare = enPassant;
        move.nextEnPassantSquare = enPassantOpportunity;

        move.previousHalfmove = halfmoveClock;

//        long zobristHashToggle = 0;

        return move;
    }

    // endregion

    // region Piece Getters
    //    _____ _____ ______ _____ ______    _____ ______ _______ ______ _____   _____
    //   |  __ \_   _|  ____/ ____|  ____|  / ____|  ____|__   __|__   __|  ____|  __ \ / ____|
    //   | |__) || | | |__ | |    | |__    | |  __| |__     | |     | |  | |__  | |__) | (___
    //   |  ___/ | | |  __|| |    |  __|   | | |_ |  __|    | |     | |  |  __| |  _  / \___ \
    //   | |    _| |_| |___| |____| |____  | |__| | |____   | |     | |  | |____| | \ \ ____) |
    //   |_|   |_____|______\_____|______|  \_____|______|  |_|     |_|  |______|_|  \_\_____/

    public ColoredPiece getPiece(final Square square) {
        return getPiece(square.getOccupiedBitMask());
    }

    private ColoredPiece getPiece(final long square) {
        if ((white.kings & square) != 0L) {
            return ColoredPiece.WHITE_KING;
        }

        if ((white.queens & square) != 0L) {
            return ColoredPiece.WHITE_QUEEN;
        }

        if ((white.rooks & square) != 0L) {
            return ColoredPiece.WHITE_ROOK;
        }

        if ((white.bishops & square) != 0L) {
            return ColoredPiece.WHITE_BISHOP;
        }

        if ((white.knights & square) != 0L) {
            return ColoredPiece.WHITE_KNIGHT;
        }

        if ((white.pawns & square) != 0L) {
            return ColoredPiece.WHITE_PAWN;
        }

        if ((black.kings & square) != 0L) {
            return ColoredPiece.BLACK_KING;
        }

        if ((black.queens & square) != 0L) {
            return ColoredPiece.BLACK_QUEEN;
        }

        if ((black.rooks & square) != 0L) {
            return ColoredPiece.BLACK_ROOK;
        }

        if ((black.bishops & square) != 0L) {
            return ColoredPiece.BLACK_BISHOP;
        }

        if ((black.knights & square) != 0L) {
            return ColoredPiece.BLACK_KNIGHT;
        }

        if ((black.pawns & square) != 0L) {
            return ColoredPiece.BLACK_PAWN;
        }

        return null;
    }

    private static boolean isOccupied(final long board, final long square) {
        return (board & square) != 0L;
    }

    // endregion

    // region Heuristics
    //    _    _ ______ _    _ _____  _____  _____ _______ _____ _____  _____
    //   | |  | |  ____| |  | |  __ \|_   _|/ ____|__   __|_   _/ ____|/ ____|
    //   | |__| | |__  | |  | | |__) | | | | (___    | |    | || |    | (___
    //   |  __  |  __| | |  | |  _  /  | |  \___ \   | |    | || |     \___ \
    //   | |  | | |____| |__| | | \ \ _| |_ ____) |  | |   _| || |____ ____) |
    //   |_|  |_|______|\____/|_|  \_\_____|_____/   |_|  |_____\_____|_____/

    public int computeScore(final Color color) {
        Objects.requireNonNull(color);

        return color == Color.WHITE ? white.score() : black.score();
    }

    public int pieceSquareValue(final Color color) {
        final boolean lateGame = isLateGame();

        final int[] whiteKingTable = lateGame ? WHITE_KING_TABLE_LATE : WHITE_KING_TABLE_EARLY;
        final int[] blackKingTable = lateGame ? BLACK_KING_TABLE_LATE : BLACK_KING_TABLE_EARLY;

        final int whiteSum = sum(white.pawns, WHITE_PAWN_TABLE)
                + sum(white.knights, WHITE_KNIGHT_TABLE)
                + sum(white.bishops, WHITE_BISHOP_TABLE)
                + sum(white.rooks, WHITE_ROOK_TABLE)
                + sum(white.queens, WHITE_QUEEN_TABLE)
                + sum(white.kings, whiteKingTable);

        final int blackSum = sum(black.pawns, BLACK_PAWN_TABLE)
                + sum(black.knights, BLACK_KNIGHT_TABLE)
                + sum(black.bishops, BLACK_BISHOP_TABLE)
                + sum(black.rooks, BLACK_ROOK_TABLE)
                + sum(black.queens, BLACK_QUEEN_TABLE)
                + sum(black.kings, blackKingTable);

        final int sum = whiteSum + blackSum;

        return color == Color.WHITE ? -sum : sum;
    }

    private boolean isLateGame() {
        return (white.queens | black.queens) == 0L;
    }

    private static int sum(final long board, final int[] valueTable) {
        long occupancy = board;

        int sum = 0;

        while (occupancy != 0L) {
            final long current = Long.highestOneBit(occupancy);
            occupancy &= ~current;
            sum += valueTable[Long.numberOfTrailingZeros(current)];
        }

        return sum;
    }

    private static int mvvLva(final Piece source, final Piece target) {
        if (target == null || target == Piece.KING) {
            return 0;
        }

        final int sourceValue = source == Piece.KING ? QUEEN_VALUE + 1 : pieceValue(source);

        return (pieceValue(target) << 8) - sourceValue;
    }

    private int pieceSquareValue(final Piece piece, final Color color, final int square) {
        if (color == Color.WHITE) {
            switch (piece) {
                case KING:
                    return isLateGame() ? WHITE_KING_TABLE_LATE[square] : WHITE_KING_TABLE_EARLY[square];
                case QUEEN:
                    return WHITE_QUEEN_TABLE[square];
                case ROOK:
                    return WHITE_ROOK_TABLE[square];
                case BISHOP:
                    return WHITE_BISHOP_TABLE[square];
                case KNIGHT:
                    return WHITE_KNIGHT_TABLE[square];
                case PAWN:
                    return WHITE_PAWN_TABLE[square];
            }
        } else {
            switch (piece) {
                case KING:
                    return isLateGame() ? BLACK_KING_TABLE_LATE[square] : BLACK_KING_TABLE_EARLY[square];
                case QUEEN:
                    return BLACK_QUEEN_TABLE[square];
                case ROOK:
                    return BLACK_ROOK_TABLE[square];
                case BISHOP:
                    return BLACK_BISHOP_TABLE[square];
                case KNIGHT:
                    return BLACK_KNIGHT_TABLE[square];
                case PAWN:
                    return BLACK_PAWN_TABLE[square];
            }
        }

        throw new IllegalStateException();
    }

    public long zobristHash() {
        long occupancy = white.occupancy() | black.occupancy();

        long hash = 0L;

        while (occupancy != 0L) {
            final long current = Long.highestOneBit(occupancy);
            occupancy &= ~current;

            final int squareIndex = Long.numberOfTrailingZeros(current);

            final ColoredPiece piece = getPiece(current);

            hash ^= ZobristHashing.hashPieceSquare(piece, squareIndex);
        }

        if (enPassant != 0L) {
            hash ^= ZobristHashing.hashEnPassant(Long.numberOfTrailingZeros(enPassant));
        }

        if (white.kingSideCastle) {
            hash ^= ZobristHashing.whiteKingCastleHash();
        }
        if (white.queenSideCastle) {
            hash ^= ZobristHashing.whiteQueenCastleHash();
        }
        if (black.kingSideCastle) {
            hash ^= ZobristHashing.blackKingCastleHash();
        }
        if (black.queenSideCastle) {
            hash ^= ZobristHashing.blackQueenCastleHash();
        }

        if (turn == Color.BLACK) {
            hash ^= ZobristHashing.getBlacksTurnHash();
        }

        return hash;
    }

    public boolean zobristEquals(final Bitboard bitboard) {
        return white.equals(bitboard.white) && black.equals(bitboard.black) && enPassant == bitboard.enPassant;
    }

    // endregion

    // region Checks
    //     _____ _    _ ______ _____ _  __ _____
    //    / ____| |  | |  ____/ ____| |/ // ____|
    //   | |    | |__| | |__ | |    | ' /| (___
    //   | |    |  __  |  __|| |    |  <  \___ \
    //   | |____| |  | | |___| |____| . \ ____) |
    //    \_____|_|  |_|______\_____|_|\_\_____/

    public boolean isInvalidPosition() {
        return isInCheck(turn.opposite());
    }

    public boolean isInCheck() {
        return isInCheck(turn);
    }

    public boolean isInCheck(final Color color) {
        Objects.requireNonNull(color);

        if (color == Color.WHITE) {
            return isInCheck(Color.WHITE, black);
        }

        return isInCheck(Color.BLACK, white);
    }

    public boolean isInCheck(final Color color, final Square square) {
        Objects.requireNonNull(color);

        final long occupancy = white.occupancy() | black.occupancy();
        if (color == Color.WHITE) {
            return isInCheck(Color.WHITE, square.getOccupiedBitMask(), black, occupancy);
        }

        return isInCheck(Color.BLACK, square.getOccupiedBitMask(), white, occupancy);
    }

    private boolean isInCheck(final Color color, final PlayerBoard opponent) {
        long selfKings;
        final long occupancy = white.occupancy() | black.occupancy();

        if (color == Color.WHITE) {
            selfKings = white.kings;
        } else {
            selfKings = black.kings;
        }

        while (selfKings != 0L) {
            final long king = Long.highestOneBit(selfKings);
            selfKings &= ~king;

            if (isInCheck(color, king, opponent, occupancy)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isInCheck(final Color color, final long square, final PlayerBoard opponent, final long occupancy) {
        final int index = Long.numberOfTrailingZeros(square);

        final long rookAttacks = MagicBitboard.ROOK.attacks(occupancy, index);

        if ((rookAttacks & (opponent.rooks | opponent.queens)) != 0L) {
            return true;
        }

        final long bishopAttacks = MagicBitboard.BISHOP.attacks(occupancy, index);

        if ((bishopAttacks & (opponent.bishops | opponent.queens)) != 0L) {
            return true;
        }

        final long knightAttacks = KNIGHT_ATTACKS[index];

        if ((knightAttacks & opponent.knights) != 0L) {
            return true;
        }

        final long pawnAttacks;

        if (color == Color.WHITE && (square & RANK_EIGHT_SQUARES) == 0) {
            pawnAttacks = WHITE_PAWN_ATTACKS[index];
        } else if (color == Color.BLACK && (square & RANK_ONE_SQUARES) == 0) {
            pawnAttacks = BLACK_PAWN_ATTACKS[index];
        } else {
            pawnAttacks = 0L;
        }

        if ((pawnAttacks & opponent.pawns) != 0L) {
            return true;
        }

        final long kingAttacks = KING_ATTACKS[index];

        if ((kingAttacks & opponent.kings) != 0L) {
            return true;
        }

        return false;
    }

    // endregion

    // region String Generation
    //     _____ _______ _____  _____ _   _  _____    _____ ______ _   _ ______ _____         _______ _____ ____  _   _
    //    / ____|__   __|  __ \|_   _| \ | |/ ____|  / ____|  ____| \ | |  ____|  __ \     /\|__   __|_   _/ __ \| \ | |
    //   | (___    | |  | |__) | | | |  \| | |  __  | |  __| |__  |  \| | |__  | |__) |   /  \  | |    | || |  | |  \| |
    //    \___ \   | |  |  _  /  | | | . ` | | |_ | | | |_ |  __| | . ` |  __| |  _  /   / /\ \ | |    | || |  | | . ` |
    //    ____) |  | |  | | \ \ _| |_| |\  | |__| | | |__| | |____| |\  | |____| | \ \  / ____ \| |   _| || |__| | |\  |
    //   |_____/   |_|  |_|  \_\_____|_| \_|\_____|  \_____|______|_| \_|______|_|  \_\/_/    \_\_|  |_____\____/|_| \_|

    public String fen() {
        final StringBuilder stringBuilder = new StringBuilder("................................................................");

        for (final Square square : SQUARES) {
            final ColoredPiece piece = getPiece(square);

            if (piece == null) {
                continue;
            }

            final int index = (8 - square.getRank().getIndex() - 1) * 8 + square.getFile().getIndex();

            stringBuilder.setCharAt(index, piece.getSan());
        }

        String result = stringBuilder.toString()
                                     .replaceAll("(?<=\\G.{8})", "/")
                                     .replaceFirst("/$", "");

        while (true) {
            final Pattern compile = Pattern.compile("^[^.]*(\\.+).*$");

            final Matcher matcher = compile.matcher(result);

            if (!matcher.matches()) {
                final StringBuilder castle = new StringBuilder();

                if (white.kingSideCastle) {
                    castle.append("K");
                }

                if (white.queenSideCastle) {
                    castle.append("Q");
                }

                if (black.kingSideCastle) {
                    castle.append("k");
                }

                if (black.queenSideCastle) {
                    castle.append("q");
                }

                if (castle.length() == 0) {
                    castle.append("-");
                }

                final String enPassantString = enPassant == 0L ? "-" : SQUARES[Long.numberOfTrailingZeros(enPassant)].getFen();

                return result + " " + turn.getFen() + " " + castle + " " + enPassantString + " " + halfmoveClock + " " + fullmoveClock;
            }

            final String group = matcher.group(1);

            result = result.replaceFirst(Pattern.quote(group), Integer.toString(group.length()));
        }
    }

    @Override
    public String toString() {
        final StringJoiner resultJoiner = new StringJoiner("\n");

        resultJoiner.add("╔═══╦═══╤═══╤═══╤═══╤═══╤═══╤═══╤═══╗");

        final StringJoiner lineJoiner = new StringJoiner("\n║   ╟───┼───┼───┼───┼───┼───┼───┼───╢\n");

        for (int i = 8 - 1; i >= 0; i--) {
            final StringJoiner squareJoiner = new StringJoiner(" │ ");
            for (int j = 0; j < 8; j++) {
                final ColoredPiece piece = getPiece(Square.get(j, i));
                squareJoiner.add(piece == null ? " " : Character.toString(piece.getSan()));
            }
            lineJoiner.add("║ " + (i + 1) + " ║ " + squareJoiner.toString() + " ║");
        }

        resultJoiner.add(lineJoiner.toString());
        resultJoiner.add("║   ╚═══╧═══╧═══╧═══╧═══╧═══╧═══╧═══╣");
        resultJoiner.add("║     A   B   C   D   E   F   G   H ║");
        resultJoiner.add("╠═══════════════════════════════════╣");
        addLine(resultJoiner, "turn", turn.toString());
        addLine(resultJoiner, "halfmove clock", Integer.toString(halfmoveClock));
        addLine(resultJoiner, "fullmove clock", Integer.toString(fullmoveClock));
        addLine(resultJoiner, "castle", Fen.parse(fen()).getCastlingAvailability());
        addLine(resultJoiner, "enPassant", enPassant == 0L ? "-" : SQUARES[Long.numberOfTrailingZeros(enPassant)].toString());
        resultJoiner.add("╚═══════════════════════════════════╝");

        return resultJoiner.toString();
    }

    private static void addLine(final StringJoiner resultJoiner, final String name, final String value) {
        final String enPassantString = name + padLeft(value, 33 - name.length());
        resultJoiner.add("║ " + enPassantString + " ║");
    }

    private static String padRight(final String s, final int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String padLeft(final String s, final int n) {
        return String.format("%" + n + "s", s);
    }

    // endregion

    // region Move Do/Undo
    //    __  __  ______      ________   _____   ____     ___    _ _   _ _____   ____
    //   |  \/  |/ __ \ \    / /  ____| |  __ \ / __ \   / / |  | | \ | |  __ \ / __ \
    //   | \  / | |  | \ \  / /| |__    | |  | | |  | | / /| |  | |  \| | |  | | |  | |
    //   | |\/| | |  | |\ \/ / |  __|   | |  | | |  | |/ / | |  | | . ` | |  | | |  | |
    //   | |  | | |__| | \  /  | |____  | |__| | |__| / /  | |__| | |\  | |__| | |__| |
    //   |_|  |_|\____/   \/   |______| |_____/ \____/_/    \____/|_| \_|_____/ \____/

    public void make(final BBMove bbMove) {
        final PlayerBoard self;
        final PlayerBoard opponent;

        final boolean whiteTurn = turn == Color.WHITE;

        if (whiteTurn) {
            self = white;
            opponent = black;
        } else {
            self = black;
            opponent = white;

            fullmoveClock += 1;
        }

        if (bbMove.selfLostKingSideCastle) {
            self.kingSideCastle = false;
        }

        if (bbMove.selfLostQueenSideCastle) {
            self.queenSideCastle = false;
        }

        if (bbMove.opponentLostKingSideCastle) {
            opponent.kingSideCastle = false;
        }

        if (bbMove.opponentLostQueenSideCastle) {
            opponent.queenSideCastle = false;
        }

        if (bbMove.castle) {
            if (bbMove.targetSquare == Square.C1.getOccupiedBitMask()) {
                doCastle(self, Square.A1, Square.E1, Square.D1, Square.C1);
            } else if (bbMove.targetSquare == Square.G1.getOccupiedBitMask()) {
                doCastle(self, Square.H1, Square.E1, Square.F1, Square.G1);
            } else if (bbMove.targetSquare == Square.C8.getOccupiedBitMask()) {
                doCastle(self, Square.A8, Square.E8, Square.D8, Square.C8);
            } else if (bbMove.targetSquare == Square.G8.getOccupiedBitMask()) {
                doCastle(self, Square.H8, Square.E8, Square.F8, Square.G8);
            }
        } else if (bbMove.enPassantAttack) {
            self.pawns &= ~bbMove.sourceSquare;
            self.pawns |= bbMove.targetSquare;

            if (whiteTurn) {
                opponent.unsetAll(bbMove.targetSquare >> 8);
            } else {
                opponent.unsetAll(bbMove.targetSquare << 8);
            }
        } else {
            switch (bbMove.pieceMoved) {
                case KING:
                    self.kings = (self.kings & ~bbMove.sourceSquare) | bbMove.targetSquare;
                    break;
                case QUEEN:
                    self.queens = (self.queens & ~bbMove.sourceSquare) | bbMove.targetSquare;
                    break;
                case ROOK:
                    self.rooks = (self.rooks & ~bbMove.sourceSquare) | bbMove.targetSquare;
                    break;
                case BISHOP:
                    self.bishops = (self.bishops & ~bbMove.sourceSquare) | bbMove.targetSquare;
                    break;
                case KNIGHT:
                    self.knights = (self.knights & ~bbMove.sourceSquare) | bbMove.targetSquare;
                    break;
                case PAWN:
                    if (bbMove.promote == null) {
                        self.pawns = (self.pawns & ~bbMove.sourceSquare) | bbMove.targetSquare;
                    } else {
                        self.pawns &= ~bbMove.sourceSquare;
                        switch (bbMove.promote) {
                            case QUEEN:
                                self.queens |= bbMove.targetSquare;
                                break;
                            case ROOK:
                                self.rooks |= bbMove.targetSquare;
                                break;
                            case BISHOP:
                                self.bishops |= bbMove.targetSquare;
                                break;
                            case KNIGHT:
                                self.knights |= bbMove.targetSquare;
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                    }
            }

            opponent.unsetAll(bbMove.targetSquare);
        }

        enPassant = bbMove.nextEnPassantSquare;
        halfmoveClock = bbMove.nextHalfmove;

        turn = turn.opposite();
    }

    public void unmake(final BBMove bbMove) {
        turn = turn.opposite();
        halfmoveClock = bbMove.previousHalfmove;
        enPassant = bbMove.previousEnPassantSquare;

        final PlayerBoard self;
        final PlayerBoard opponent;

        final boolean whiteTurn = turn == Color.WHITE;
        if (whiteTurn) {
            self = white;
            opponent = black;
        } else {
            self = black;
            opponent = white;

            fullmoveClock -= 1;
        }

        if (bbMove.selfLostKingSideCastle) {
            self.kingSideCastle = true;
        }

        if (bbMove.selfLostQueenSideCastle) {
            self.queenSideCastle = true;
        }

        if (bbMove.opponentLostKingSideCastle) {
            opponent.kingSideCastle = true;
        }

        if (bbMove.opponentLostQueenSideCastle) {
            opponent.queenSideCastle = true;
        }

        if (bbMove.castle) {
            if (bbMove.targetSquare == Square.C1.getOccupiedBitMask()) {
                undoCastle(self, Square.A1, Square.E1, Square.D1, Square.C1);
            } else if (bbMove.targetSquare == Square.G1.getOccupiedBitMask()) {
                undoCastle(self, Square.H1, Square.E1, Square.F1, Square.G1);
            } else if (bbMove.targetSquare == Square.C8.getOccupiedBitMask()) {
                undoCastle(self, Square.A8, Square.E8, Square.D8, Square.C8);
            } else if (bbMove.targetSquare == Square.G8.getOccupiedBitMask()) {
                undoCastle(self, Square.H8, Square.E8, Square.F8, Square.G8);
            }
        } else if (bbMove.enPassantAttack) {
            self.pawns |= bbMove.sourceSquare;
            self.pawns &= ~bbMove.targetSquare;

            final long epAttackTarget;

            if (whiteTurn) {
                epAttackTarget = bbMove.targetSquare >> 8;
            } else {
                epAttackTarget = bbMove.targetSquare << 8;
            }

            switch (bbMove.pieceAttacked) {
                case KING:
                    opponent.kings |= epAttackTarget;
                    break;
                case QUEEN:
                    opponent.queens |= epAttackTarget;
                    break;
                case ROOK:
                    opponent.rooks |= epAttackTarget;
                    break;
                case BISHOP:
                    opponent.bishops |= epAttackTarget;
                    break;
                case KNIGHT:
                    opponent.knights |= epAttackTarget;
                    break;
                case PAWN:
                    opponent.pawns |= epAttackTarget;
                    break;
            }
        } else {
            if (bbMove.pieceAttacked != null) {
                switch (bbMove.pieceAttacked) {
                    case KING:
                        opponent.kings |= bbMove.targetSquare;
                        break;
                    case QUEEN:
                        opponent.queens |= bbMove.targetSquare;
                        break;
                    case ROOK:
                        opponent.rooks |= bbMove.targetSquare;
                        break;
                    case BISHOP:
                        opponent.bishops |= bbMove.targetSquare;
                        break;
                    case KNIGHT:
                        opponent.knights |= bbMove.targetSquare;
                        break;
                    case PAWN:
                        opponent.pawns |= bbMove.targetSquare;
                        break;
                }
            }

            switch (bbMove.pieceMoved) {
                case KING:
                    self.kings |= bbMove.sourceSquare;
                    break;
                case QUEEN:
                    self.queens |= bbMove.sourceSquare;
                    break;
                case ROOK:
                    self.rooks |= bbMove.sourceSquare;
                    break;
                case BISHOP:
                    self.bishops |= bbMove.sourceSquare;
                    break;
                case KNIGHT:
                    self.knights |= bbMove.sourceSquare;
                    break;
                case PAWN:
                    self.pawns |= bbMove.sourceSquare;
                    break;
            }

            self.unsetAll(bbMove.targetSquare);
        }
    }

    private static void doCastle(
            final PlayerBoard self,
            final Square rookSource,
            final Square kingSource,
            final Square rookTarget,
            final Square kingTarget
    ) {
        self.rooks &= ~rookSource.getOccupiedBitMask();
        self.kings &= ~kingSource.getOccupiedBitMask();

        self.rooks |= rookTarget.getOccupiedBitMask();
        self.kings |= kingTarget.getOccupiedBitMask();
    }

    private static void undoCastle(
            final PlayerBoard self,
            final Square rookSource,
            final Square kingSource,
            final Square rookTarget,
            final Square kingTarget
    ) {
        self.rooks |= rookSource.getOccupiedBitMask();
        self.kings |= kingSource.getOccupiedBitMask();

        self.rooks &= ~rookTarget.getOccupiedBitMask();
        self.kings &= ~kingTarget.getOccupiedBitMask();
    }

    private static int pieceValue(final Piece piece) {
        if (piece == null) {
            return 0;
        }

        switch (piece) {
            case KING:
                return KING_VALUE;
            case QUEEN:
                return QUEEN_VALUE;
            case ROOK:
                return ROOK_VALUE;
            case BISHOP:
                return BISHOP_VALUE;
            case KNIGHT:
                return KNIGHT_VALUE;
            case PAWN:
                return PAWN_VALUE;
            default:
                throw new AssertionError();
        }
    }

    public static class BBMove {
        private final boolean castle;
        private final boolean enPassantAttack;
        private final int mvvLva;

        private boolean selfLostQueenSideCastle;
        private boolean selfLostKingSideCastle;
        private boolean opponentLostQueenSideCastle;
        private boolean opponentLostKingSideCastle;

        private long previousEnPassantSquare;
        private long nextEnPassantSquare;

        private final long sourceSquare;
        private final long targetSquare;

        private final Piece pieceMoved;
        private final Piece pieceAttacked;
        private final Piece promote;

        private int previousHalfmove;
        private int nextHalfmove;

        private final int moveOrderValue;

//        private long zobristHashToggle;

        BBMove(final long sourceSquare,
               final long targetSquare,
               final Piece pieceMoved,
               final Piece pieceAttacked,
               final boolean castle,
               final boolean enPassantAttack,
               final Piece promote,
               final int squareDiff
        ) {
            this.sourceSquare = sourceSquare;
            this.targetSquare = targetSquare;
            this.pieceMoved = pieceMoved;
            this.pieceAttacked = pieceAttacked;
            this.castle = castle;
            this.enPassantAttack = enPassantAttack;
            this.promote = promote;

            this.mvvLva = mvvLva(pieceMoved, pieceAttacked);
            this.moveOrderValue = mvvLva + squareDiff;
        }

        @Override
        public String toString() {
            return "BBMove{" +
                    "castle=" + castle +
                    ", enPassantAttack=" + enPassantAttack +
                    ", selfLostQueenSideCastle=" + selfLostQueenSideCastle +
                    ", selfLostKingSideCastle=" + selfLostKingSideCastle +
                    ", opponentLostQueenSideCastle=" + opponentLostQueenSideCastle +
                    ", opponentLostKingSideCastle=" + opponentLostKingSideCastle +
                    ", previousEnPassantSquare=" + previousEnPassantSquare +
                    ", nextEnPassantSquare=" + nextEnPassantSquare +
                    ", sourceSquare=" + sourceSquare +
                    ", targetSquare=" + targetSquare +
                    ", pieceMoved=" + pieceMoved +
                    ", pieceAttacked=" + pieceAttacked +
                    ", promote=" + promote +
                    ", previousHalfmove=" + previousHalfmove +
                    ", nextHalfmove=" + nextHalfmove +
                    '}';
        }

        public UciMove asUciMove() {
            final Square source = SQUARES[Long.numberOfTrailingZeros(sourceSquare)];
            final Square target = SQUARES[Long.numberOfTrailingZeros(targetSquare)];

            if (promote != null) {
                return new UciMove(source, target, promote);
            } else {
                return new UciMove(source, target, null);
            }
        }

        public int getMvvLvaSquarePieceDifferenceValue() {
            return moveOrderValue;
        }

        public int getMvvLvaValue() {
            return mvvLva;
        }

        public boolean isRepetitionOf(final BBMove other) {
            return other != null
                    && pieceAttacked == null
                    && other.pieceAttacked == null
                    && sourceSquare == other.sourceSquare
                    && targetSquare == other.targetSquare
                    && pieceMoved == other.pieceMoved;
        }

        public boolean isAttack() {
            return pieceAttacked != null;
        }
    }

    // endregion

    @EqualsAndHashCode
    private static class PlayerBoard {
        private long kings;
        private long queens;
        private long rooks;
        private long bishops;
        private long knights;
        private long pawns;

        private boolean queenSideCastle;
        private boolean kingSideCastle;

        PlayerBoard() {
        }

        PlayerBoard(final PlayerBoard other) {
            this.kings = other.kings;
            this.queens = other.queens;
            this.rooks = other.rooks;
            this.bishops = other.bishops;
            this.knights = other.knights;
            this.pawns = other.pawns;

            this.kingSideCastle = other.kingSideCastle;
            this.queenSideCastle = other.queenSideCastle;
        }

        long occupancy() {
            return kings | queens | rooks | bishops | knights | pawns;
        }

        @Override
        public String toString() {
            final StringJoiner stringJoiner = new StringJoiner("\n");

            stringJoiner.add("***********************");
            stringJoiner.add("KINGS:");
            stringJoiner.add(BitboardUtil.toBoardString(kings));
            stringJoiner.add("QUEENS:");
            stringJoiner.add(BitboardUtil.toBoardString(queens));
            stringJoiner.add("ROOKS:");
            stringJoiner.add(BitboardUtil.toBoardString(rooks));
            stringJoiner.add("BISHOPS:");
            stringJoiner.add(BitboardUtil.toBoardString(bishops));
            stringJoiner.add("KNIGHTS:");
            stringJoiner.add(BitboardUtil.toBoardString(knights));
            stringJoiner.add("PAWNS:");
            stringJoiner.add(BitboardUtil.toBoardString(pawns));
            stringJoiner.add("queenSideCastle = " + queenSideCastle);
            stringJoiner.add("kingSideCastle = " + kingSideCastle);
            stringJoiner.add("***********************");

            return stringJoiner.toString();
        }

        void unsetAll(final long l) {
            final long notL = ~l;

            kings &= notL;
            queens &= notL;
            rooks &= notL;
            bishops &= notL;
            knights &= notL;
            pawns &= notL;
        }

        int score() {
            return Long.bitCount(kings) * KING_VALUE
                    + Long.bitCount(queens) * QUEEN_VALUE
                    + Long.bitCount(rooks) * ROOK_VALUE
                    + Long.bitCount(bishops) * BISHOP_VALUE
                    + Long.bitCount(knights) * KNIGHT_VALUE
                    + Long.bitCount(pawns) * PAWN_VALUE;
        }

        Piece getPiece(final long square) {
            if ((pawns & square) != 0L) {
                return Piece.PAWN;
            }
            if ((knights & square) != 0L) {
                return Piece.KNIGHT;
            }
            if ((bishops & square) != 0L) {
                return Piece.BISHOP;
            }
            if ((rooks & square) != 0L) {
                return Piece.ROOK;
            }
            if ((queens & square) != 0L) {
                return Piece.QUEEN;
            }
            if ((kings & square) != 0L) {
                return Piece.KING;
            }
            return null;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Bitboard bitboard = (Bitboard) o;

        if (enPassant != bitboard.enPassant) return false;
        if (fullmoveClock != bitboard.fullmoveClock) return false;
        if (halfmoveClock != bitboard.halfmoveClock) return false;
        if (black != null ? !black.equals(bitboard.black) : bitboard.black != null) return false;
        if (white != null ? !white.equals(bitboard.white) : bitboard.white != null) return false;
        return turn == bitboard.turn;

    }

    @Override
    public int hashCode() {
        int result = black != null ? black.hashCode() : 0;
        result = 31 * result + (white != null ? white.hashCode() : 0);
        result = 31 * result + (turn != null ? turn.hashCode() : 0);
        result = 31 * result + (int) (enPassant ^ (enPassant >>> 32));
        result = 31 * result + fullmoveClock;
        result = 31 * result + halfmoveClock;
        return result;
    }
}
