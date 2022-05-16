
/* Skeleton code copyright (C) 2008, 2022 Paul N. Hilfinger and the
 * Regents of the University of California.  Do not distribute this or any
 * derivative work without permission. */

package ataxx;
import java.util.ArrayList;
import java.util.Random;
import static ataxx.PieceColor.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

/** A Player that computes its own moves.
 *  @author V. Dabholkar
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 4;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. SEED is used to initialize
     *  a random-number generator for use in move computations.  Identical
     *  seeds produce identical behaviour. */
    AI(Game game, PieceColor myColor, long seed) {
        super(game, myColor);
        _random = new Random(seed);
    }

    @Override
    boolean isAuto() {
        return true;
    }

    @Override
    String getMove() {
        if (!getBoard().canMove(myColor())) {
            game().reportMove(Move.pass(), myColor());
            return "-";
        }
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();
        game().reportMove(move, myColor());
        return move.toString();
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(getBoard());
        _lastFoundMove = null;
        if (myColor() == RED) {
            minMax(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
            System.out.println();
        } else {
            minMax(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to the findMove method
     *  above. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove, int sense,
                       int alpha, int beta) {
        if (depth == 0 || board.getWinner() != null) {
            return staticScore(board, WINNING_VALUE + depth);
        }
        Move best = null;
        int bestScore;
        bestScore = (sense == 1) ? -INFTY : INFTY;

        ArrayList<Move> validValues = helper(board);

        for (Move move : validValues) {
            if (board.legalMove(move)) {
                board.makeMove(move);
                int score = minMax(board, depth - 1,
                        false, -sense, alpha, beta);
                board.undo();
                if (sense == 1) {
                    if (score > bestScore) {
                        bestScore = score;
                        best = move;
                        alpha = max(alpha, bestScore);
                    }
                } else if (sense == -1) {
                    if (score < bestScore) {
                        bestScore = score;
                        best = move;
                        beta = min(beta, bestScore);
                    }
                }
                if (alpha >= beta) {
                    best = move;
                    break;
                }
            }
        }
        if (saveMove) {
            _lastFoundMove = best;
        }
        return bestScore;
    }
    private ArrayList<Move> helper(Board board) {
        ArrayList<Move> validValues = new ArrayList<>();
        for (char boardRow = 'a'; boardRow <= 'g'; boardRow++) {
            for (char boardCol = '1'; boardCol <= '7'; boardCol++) {
                if (board.get(boardRow, boardCol) == board.whoseMove()) {
                    for (int rOff = -2; rOff <= 2; rOff++) {
                        for (int cOff = -2; cOff <= 2; cOff++) {
                            if (board.legalMove(boardRow, boardCol, (char)
                                            (boardRow + rOff),
                                    (char) (boardCol + cOff))) {
                                validValues.add(Move.move(boardRow, boardCol,
                                        (char) (boardRow + rOff), (char)
                                                (boardCol + cOff)));
                            }
                        }
                    }
                }
            }
        }
        if (board.legalMove(Move.PASS)) {
            validValues.add(Move.pass());
        }
        return validValues;
    }


    /** Return a heuristic value for BOARD.  This value is +- WINNINGVALUE in
     *  won positions, and 0 for ties. */
    private int staticScore(Board board, int winningValue) {
        PieceColor winner = board.getWinner();
        if (winner != null) {
            return switch (winner) {
            case RED -> winningValue;
            case BLUE -> -winningValue;
            default -> 0;
            };
        }
        return board.numPieces(RED) - board.numPieces(BLUE);
    }

    /** Pseudo-random number generator for move computation. */
    private Random _random = new Random();
}
