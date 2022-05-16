/* Skeleton code copyright (C) 2008, 2022 Paul N. Hilfinger and the
 * Regents of the University of California.  Do not distribute this or any
 * derivative work without permission. */

package ataxx;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Formatter;

import java.util.function.Consumer;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author V. Dabholkar
 */
class Board {

    /**
     * Number of squares on a side of the board.
     */
    static final int SIDE = Move.SIDE;

    /**
     * Length of a side + an artificial 2-deep border region.
     * This is unrelated to a move that is an "extend".
     */
    static final int EXTENDED_SIDE = Move.EXTENDED_SIDE;

    /**
     * Number of consecutive non-extending moves before game ends.
     */
    static final int JUMP_LIMIT = 25;

    /**
     * A new, cleared board in the initial configuration.
     */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        for (int i = '1' - 2; i <= '7' + 2; i++) {
            for (int x = 'a' - 2; x <= 'g' + 2; x++) {
                int location = index((char) x, (char) i);
                set(location, BLOCKED);
            }
        }
        setNotifier(NOP);
        clear();
    }

    /**
     * A board whose initial contents are copied from BOARD0, but whose
     * undo history is clear, and whose notifier does nothing.
     */
    Board(Board board0) {
        _board = board0._board.clone();
        _numPieces = board0._numPieces.clone();
        _allMoves = new ArrayList<>();
        _numJumps = 0;
        _whoseMove = board0.whoseMove();
        _undoSquares = new Stack<>();
        _undoPieces = new Stack<>();
        _winner = null;
        setNotifier(NOP);
    }

    /**
     * Return the linearized index of square COL ROW.
     */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /**
     * Return the linearized index of the square that is DC columns and DR
     * rows away from the square with index SQ.
     */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /**
     * Clear me to my starting state, with pieces in their initial
     * positions and no blocks.
     */
    void clear() {
        _numPieces = new int[BLUE.ordinal() + 1];
        for (int i = 0; i < numbers.length(); i++) {
            for (int x = 0; x < alphabet.length(); x++) {
                char y = alphabet.charAt(x);
                char z = numbers.charAt(i);
                set(index(y, z), EMPTY);
                _totalOpen++;
            }
        }
        set('g', '1', RED);
        set('a', '1', BLUE);
        set('a', '7', RED);
        set('g', '7', BLUE);
        _totalOpen += 4;

        incrPieces(RED, 2);
        incrPieces(BLUE, 2);

        _whoseMove = RED;
        _allMoves = new ArrayList<>();
        _numJumps = 0;
        _undoSquares = new Stack<>();
        _undoPieces = new Stack<>();
        _winner = null;
        announce();
    }

    /**
     * Return the winner, if there is one yet, and otherwise null.  Returns
     * EMPTY in the case of a draw, which can happen as a result of there
     * having been MAX_JUMPS consecutive jumps without intervening extends,
     * or if neither player can move and both have the same number of pieces.
     */
    PieceColor getWinner() {
        return _winner;
    }

    /**
     * Return number of red pieces on the board.
     */
    int redPieces() {
        return numPieces(RED);
    }

    /**
     * Return number of blue pieces on the board.
     */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /**
     * Return number of COLOR pieces on the board.
     */
    int numPieces(PieceColor color) {
        return _numPieces[color.ordinal()];
    }

    /**
     * Increment numPieces(COLOR) by K.
     */
    private void incrPieces(PieceColor color, int k) {
        _numPieces[color.ordinal()] += k;
    }

    /**
     * The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     * '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     * BLOCKED.  Returns the same value as get(index(C, R)).
     */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /**
     * Return the current contents of square with linearized index SQ.
     */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /**
     * Set get(C, R) to V, where 'a' <= C <= 'g', and
     * '1' <= R <= '7'. This operation is undoable.
     */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /**
     * Set square with linearized index SQ to V.  This operation is
     * undoable.
     */
    private void set(int sq, PieceColor v) {
        addUndo(sq);
        _board[sq] = v;
    }

    /**
     * Set square at C R to V (not undoable). This is used for changing
     * contents of the board without updating the undo stacks.
     */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /**
     * Set square at linearized index SQ to V (not undoable). This is used
     * for changing contents of the board without updating the undo stacks.
     */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /**
     * Return true iff MOVE is legal on the current board.
     */
    boolean legalMove(Move move) {
        String moveStr;
        if (move == null) {
            return false;
        } else {
            moveStr = move.toString();
        }
        if (move.isPass()) {
            if (canMove(whoseMove())
                    || _numPieces[whoseMove().ordinal()] == 0) {
                return false;
            }
            return true;
        } else if (getWinner() != null) {
            return false;
        } else if (moveStr.length() != 5) {
            return false;
        } else if (get(move.fromIndex()) != whoseMove()) {
            return false;
        } else if (get(moveStr.charAt(3), moveStr.charAt(4)) != EMPTY) {
            return false;
        } else if (get(moveStr.charAt(0), moveStr.charAt(1)) != _whoseMove) {
            return false;
        } else if (move.isPass() || move.isExtend() || move.isJump()) {
            return true;
        }
        return true;
    }

    /**
     * Return true iff C0 R0 - C1 R1 is legal on the current board.
     */
    boolean legalMove(char c0, char r0, char c1, char r1) {
        return legalMove(Move.move(c0, r0, c1, r1));
    }

    /**
     * Return true iff player WHO can move, ignoring whether it is
     * that player's move and whether the game is over.
     */
    boolean canMove(PieceColor who) {
        PieceColor old = _whoseMove;
        _whoseMove = who;
        for (char c = 'a'; c <= 'g'; c++) {
            for (char r = '1'; r <= '7'; r++) {
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        if (x == 0 && y == 0) {
                            continue;
                        }
                        if (legalMove(c, r, (char) (c + x), (char) (r + y))) {
                            _whoseMove = old;
                            return true;
                        }
                    }
                }
            }
        }
        _whoseMove = old;
        return false;
    }

    /**
     * Return the color of the player who has the next move.  The
     * value is arbitrary if the game is over.
     */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /**
     * Return total number of moves and passes since the last
     * clear or the creation of the board.
     */
    int numMoves() {
        return _allMoves.size();
    }

    /**
     * Return number of non-pass moves made in the current game since the
     * last extend move added a piece to the board (or since the
     * start of the game). Used to detect end-of-game.
     */
    int numJumps() {
        return _numJumps;
    }

    /**
     * Assuming MOVE has the format "-" or "C0R0-C1R1", make the denoted
     * move ("-" means "pass").
     */
    void makeMove(String move) {
        if (move.equals("-")) {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(move.charAt(0), move.charAt(1), move.charAt(3),
                    move.charAt(4)));
        }
    }

    /**
     * Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     * other than pass, assumes that legalMove(C0, R0, C1, R1).
     */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /**
     * Make the MOVE on this Board, assuming it is legal.
     */
    void makeMove(Move move) {
        if (!legalMove(move)) {
            throw error("Illegal move: %s", move);
        }

        _allMoves.add(move);
        startUndo();
        PieceColor opponent = _whoseMove.opposite();
        String moveStr = move.toString();
        if (moveStr.length() != 5) {
            throw error("Illegal move: %s", move);
        }
        _undoPieces.push(get(moveStr.charAt(3), moveStr.charAt(4)));
        set(moveStr.charAt(3), moveStr.charAt(4), _whoseMove);
        _undoSquares.push(index(moveStr.charAt(3), moveStr.charAt(4)));
        incrPieces(_whoseMove, 1);
        flipOpponents(move, opponent);
        if (move.isJump()) {
            set(moveStr.charAt(0), moveStr.charAt(1), EMPTY);
            incrPieces(_whoseMove, -1);
            _numJumps++;
        } else {
            _numJumps = 0;
        }
        gameOver();
        _whoseMove = opponent;

        announce();
    }

    void gameOver() {
        if (!canMove(_whoseMove.opposite())) {
            _winner = _whoseMove;
            if (!canMove(_whoseMove)) {
                if (redPieces() == bluePieces()) {
                    _winner = EMPTY;
                }
            }
        }
        if (redPieces() == 0) {
            _winner = BLUE;
        } else if (bluePieces() == 0) {
            _winner = RED;
        }
        if (_numJumps >= JUMP_LIMIT || _totalOpen == 0) {
            if (redPieces() > bluePieces()) {
                _winner = RED;
            } else if (redPieces() < bluePieces()) {
                _winner = BLUE;
            } else {
                _winner = EMPTY;
            }
        }
    }

    void flipOpponents(Move move, PieceColor opponent) {

        char colLetter = move.toString().charAt(3);
        char rowNumber = move.toString().charAt(4);
        int alphIndex = alphabet.indexOf(colLetter);
        int numIndex = numbers.indexOf(rowNumber);

        int[][] oppOptions = new int[8][2];
        oppOptions[0] = new int[]{alphIndex - 1, numIndex + 1};
        oppOptions[1] = new int[]{alphIndex, numIndex + 1};
        oppOptions[2] = new int[]{alphIndex + 1, numIndex + 1};
        oppOptions[3] = new int[]{alphIndex - 1, numIndex};
        oppOptions[4] = new int[]{alphIndex + 1, numIndex};
        oppOptions[5] = new int[]{alphIndex - 1, numIndex - 1};
        oppOptions[6] = new int[]{alphIndex, numIndex - 1};
        oppOptions[7] = new int[]{alphIndex + 1, numIndex - 1};

        for (int i = 0; i < oppOptions.length; i++) {
            int num = oppOptions[i][0];
            int num1 = oppOptions[i][1];
            if (!(num < 0) && !(num1 < 0)) {
                if (!(num >= 7) && !(num1 >= 7)) {
                    char x = alphabet.charAt(num);
                    char y = numbers.charAt(num1);
                    if (get(x, y) == opponent) {
                        _undoPieces.push(get(x, y));
                        _undoSquares.push(index(x, y));
                        set(x, y, _whoseMove);
                        incrPieces(_whoseMove, 1);
                        incrPieces(opponent, -1);
                    }
                }
            }
        }
    }

    /**
     * Update to indicate that the current player passes, assuming it
     * is legal to do so. Passing is undoable.
     */
    void pass() {
        assert !canMove(_whoseMove);
        _allMoves.add(null);
        startUndo();
        _whoseMove = _whoseMove.opposite();
        announce();
    }

    /**
     * Undo the last move.
     */
    void undo() {
        Move currMove = _allMoves.get(_allMoves.size() - 1);
        String currStr = currMove.toString();
        Integer latest = _undoSquares.pop();
        PieceColor latestPlayer = _undoPieces.pop();
        PieceColor last = get(latest);
        if (latestPlayer == EMPTY) {
            if (currMove.isJump()) {
                _numJumps--;
                set(currStr.charAt(0), currStr.charAt(1), last);
                set(latest, latestPlayer);
            } else {
                incrPieces(last, -1);
                set(latest, latestPlayer);
            }
        } else {
            incrPieces(latestPlayer, 1);
            incrPieces(last, -1);
            set(latest, latestPlayer);
            while (latestPlayer != EMPTY) {
                latest = _undoSquares.pop();
                latestPlayer = _undoPieces.pop();
                last = get(latest);
                incrPieces(latestPlayer, 1);
                if (latestPlayer == EMPTY && currMove.isJump()) {
                    if (currMove.isJump()) {
                        _numJumps--;
                        set(currStr.charAt(0), currStr.charAt(1), last);
                    } else {
                        incrPieces(last, -1);
                    }
                    set(latest, latestPlayer);
                } else {
                    incrPieces(last, -1);
                    set(latest, latestPlayer);
                }
            }

        }
        _allMoves.remove(_allMoves.size() - 1);
        _whoseMove = _whoseMove.opposite();
        _winner = null;
        announce();
    }

    /**
     * Indicate beginning of a move in the undo stack. See the
     * _undoSquares and _undoPieces instance variable comments for
     * details on how the beginning of moves are marked.
     */
    private void startUndo() {
    }

    /**
     * Add an undo action for changing SQ on current board.
     */
    private void addUndo(int sq) {
    }

    /**
     * Return true iff it is legal to place a block at C R.
     */
    boolean legalBlock(char c, char r) {
        if (get(c, r) == RED || get(c, r) == BLUE) {
            return false;
        }
        if (get(c, r) == BLOCKED) {
            return false;
        }
        if (!_allMoves.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Return true iff it is legal to place a block at CR.
     */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /**
     * Set a block on the square C R and its reflections across the middle
     * row and/or column, if that square is unoccupied and not
     * in one of the corners. Has no effect if any of the squares is
     * already occupied by a block.  It is an error to place a block on a
     * piece.
     */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }

        int j = alphabet.indexOf(c);
        int k = numbers.indexOf(r);

        ArrayList<ArrayList<Integer>> rList = reflectBlock(j, k);
        for (int i = 0; i < rList.size(); i++) {
            char x = alphabet.charAt(rList.get(i).get(0));
            char y = numbers.charAt(rList.get(i).get(1));
            set(x, y, BLOCKED);
            _totalOpen--;
        }
        if (!canMove(RED) && !canMove(BLUE)) {
            _winner = EMPTY;
        }
        announce();
    }

    ArrayList<ArrayList<Integer>> reflectBlock(int c, int r) {
        ArrayList<ArrayList<Integer>> numList = new ArrayList<>();
        int cAbs = Math.abs(c - 3) * 2;
        int rAbs = Math.abs(r - 3) * 2;
        if (c == 3 && r == 3) {
            numList.add(new ArrayList<>(Arrays.asList(c, r)));
            return numList;
        } else if (c == 3) {
            if (r < 3) {
                numList.add(new ArrayList<>(Arrays.asList(c, r)));
                numList.add(new ArrayList<>(Arrays.asList(c, r + rAbs)));
                return numList;
            } else {
                numList.add(new ArrayList<>(Arrays.asList(c, r)));
                numList.add(new ArrayList<>(Arrays.asList(c, r - rAbs)));
                return numList;
            }
        } else if (r == 3) {
            if (c < 3) {
                numList.add(new ArrayList<>(Arrays.asList(c, r)));
                numList.add(new ArrayList<>(Arrays.asList(c + cAbs, r)));
                return numList;
            } else {
                numList.add(new ArrayList<>(Arrays.asList(c, r)));
                numList.add(new ArrayList<>(Arrays.asList(c - cAbs, r)));
                return numList;
            }
        } else {
            if (c < 3 && r < 3) {
                numList.add(new ArrayList<>(Arrays.asList(c + cAbs, r + rAbs)));
                numList.add(new ArrayList<>(Arrays.asList(c + cAbs, r)));
                numList.add(new ArrayList<>(Arrays.asList(c, r + rAbs)));
                numList.add(new ArrayList<>(Arrays.asList(c, r)));
            } else if (c < 3 && r > 3) {
                numList.add(new ArrayList<>(Arrays.asList(c + cAbs, r)));
                numList.add(new ArrayList<>(Arrays.asList(c, r - rAbs)));
                numList.add(new ArrayList<>(Arrays.asList(c + cAbs, r - rAbs)));
                numList.add(new ArrayList<>(Arrays.asList(c, r)));
            } else if (c > 3 && r > 3) {
                numList.add(new ArrayList<>(Arrays.asList(c - cAbs, r - rAbs)));
                numList.add(new ArrayList<>(Arrays.asList(c - cAbs, r)));
                numList.add(new ArrayList<>(Arrays.asList(c, r - rAbs)));
                numList.add(new ArrayList<>(Arrays.asList(c, r)));
            } else if (c > 3 && r < 3) {
                numList.add(new ArrayList<>(Arrays.asList(c - cAbs, r)));
                numList.add(new ArrayList<>(Arrays.asList(c - cAbs, r + rAbs)));
                numList.add(new ArrayList<>(Arrays.asList(c, r + rAbs)));
                numList.add(new ArrayList<>(Arrays.asList(c, r)));
            }
        }
        return numList;
    }

    /**
     * Place a block at CR.
     */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /**
     * Return total number of unblocked squares.
     */
    int totalOpen() {
        return _totalOpen;
    }

    /**
     * Return a list of all moves made since the last clear (or start of
     * game).
     */

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        }
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /**
     * Return a text depiction of the board.  If LEGEND, supply row and
     * column numbers around the edges.
     */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        for (char r = '7'; r >= '1'; r -= 1) {
            if (legend) {
                out.format("%c", r);
            }
            out.format(" ");
            for (char c = 'a'; c <= 'g'; c += 1) {
                switch (get(c, r)) {
                case RED:
                    out.format(" r");
                    break;
                case BLUE:
                    out.format(" b");
                    break;
                case BLOCKED:
                    out.format(" X");
                    break;
                case EMPTY:
                    out.format(" -");
                    break;
                default:
                    break;
                }
            }
            out.format("%n");
        }
        if (legend) {
            out.format("   a b c d e f g");
        }
        return out.toString();
    }

    /**
     * Set my notifier to NOTIFY.
     */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /**
     * Take any action that has been set for a change in my state.
     */
    private void announce() {
        _notifier.accept(this);
    }

    /**
     * A notifier that does nothing.
     */
    private static final Consumer<Board> NOP = (s) -> {
    };

    /**
     * Use _notifier.accept(this) to announce changes to this board.
     */
    private Consumer<Board> _notifier;

    /**
     * For reasons of efficiency in copying the board,
     * we use a 1D array to represent it, using the usual access
     * algorithm: row r, column c => index(r, c).
     * <p>
     * Next, instead of using a 7x7 board, we use an 11x11 board in
     * which the outer two rows and columns are blocks, and
     * row 2, column 2 actually represents row 0, column 0
     * of the real board.  As a result of this trick, there is no
     * need to special-case being near the edge: we don't move
     * off the edge because it looks blocked.
     * <p>
     * Using characters as indices, it follows that if 'a' <= c <= 'g'
     * and '1' <= r <= '7', then row r, column c of the board corresponds
     * to _board[(c -'a' + 2) + 11 (r - '1' + 2) ].
     */
    private final PieceColor[] _board;

    /**
     * Player that is next to move.
     */
    private PieceColor _whoseMove;

    /**
     * Number of consecutive non-extending moves since the
     * last clear or the beginning of the game.
     */
    private int _numJumps;

    /**
     * Total number of unblocked squares.
     */
    private int _totalOpen;

    /**
     * Number of blue and red pieces, indexed by the ordinal positions of
     * enumerals BLUE and RED.
     */
    private int[] _numPieces = new int[BLUE.ordinal() + 1];

    /**
     * Set to winner when game ends (EMPTY if tie).  Otherwise is null.
     */
    private PieceColor _winner;

    /**
     * List of all (non-undone) moves since the last clear or beginning of
     * the game.
     */
    private ArrayList<Move> _allMoves;

    /* The undo stack. We keep a stack of squares that have changed and
     * their previous contents.  Any given move may involve several such
     * changes, so we mark the start of the changes for each move (including
     * passes) with a null. */

    /**
     * Stack of linearized indices of squares that have been modified and
     * not undone. Nulls mark the beginnings of full moves.
     */
    private Stack<Integer> _undoSquares;
    /**
     * Stack of pieces formally at corresponding squares in _UNDOSQUARES.
     */
    private Stack<PieceColor> _undoPieces;
    /**
     * Alphabet.
     */
    private String alphabet = "abcdefg";
    /**
     * Numbers.
     */
    private String numbers = "1234567";
}
