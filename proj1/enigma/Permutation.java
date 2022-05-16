package enigma;

import static enigma.EnigmaException.*;

import java.util.ArrayList;

/** Represents a permutation of a range of integers starting at 0 corresponding
 *  to the characters of an alphabet.
 *  @author V.Dabholkar
 */
class Permutation {

    /** Set this Permutation to that specified by CYCLES, a string in the
     *  form "(cccc) (cc) ..." where the c's are characters in ALPHABET, which
     *  is interpreted as a permutation in cycle notation.  Characters in the
     *  alphabet that are not included in any cycle map to themselves.
     *  Whitespace is ignored. */
    Permutation(String cycles, Alphabet alphabet) {
        cycle = cycles;
        _alphabet = alphabet;
        _cycles = new ArrayList<>();
        cycles = cycles.replace("(", " ");
        cycles = cycles.replace(")", "");
        String[] cyclesStr = cycles.split(" ");
        for (int i = 1; i < cyclesStr.length; i++) {
            _cycles.add(cyclesStr[i]);
        }
    }

    void changeAlphabet(String alphabet) {
        _alphabet = new Alphabet(alphabet);
    }

    /** Return the value of P modulo the size of this permutation. */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /** Returns the size of the alphabet I permute. */
    int size() {
        return _alphabet.size();
    }

    /** Return the result of applying this permutation to P modulo the
     *  alphabet size. */
    int permute(int p) {
        char letter = _alphabet.toChar(wrap(p));
        for (int i = 0; i < _cycles.size(); i++) {
            for (int x = 0; x < _cycles.get(i).length(); x++) {
                if (_cycles.get(i).charAt(x) == letter) {
                    int cycleLength = _cycles.get(i).length();
                    letter = _cycles.get(i).charAt((x + 1) % cycleLength);
                    break;
                }
            }
        }
        return _alphabet.toInt(letter);
    }

    /** Return the result of applying the inverse of this permutation
     *  to  C modulo the alphabet size. */
    int invert(int c) {
        char letter = _alphabet.toChar(wrap(c));
        for (int i = 0; i < _cycles.size(); i++) {
            for (int x = 0; x < _cycles.get(i).length(); x++) {
                if (_cycles.get(i).charAt(x) == letter) {
                    int num = x - 1;
                    if (num == -1) {
                        num = _cycles.get(i).length() - 1;
                    }
                    letter = _cycles.get(i).charAt(num);
                    break;
                }
            }
        }
        return _alphabet.toInt(letter);
    }

    /** Return the result of applying this permutation to the index of P
     *  in ALPHABET, and converting the result to a character of ALPHABET. */
    char permute(char p) {
        if (_alphabet.toInt(p) == -1) {
            throw enigma.EnigmaException.error("character not in alphabet");
        }
        int num = _alphabet.toInt(p);
        return _alphabet.toChar(permute(num));
    }

    /** Return the result of applying the inverse of this permutation to C. */
    char invert(char c) {
        if (_alphabet.toInt(c) == -1) {
            throw enigma.EnigmaException.error("character not in alphabet");
        }
        int num = _alphabet.toInt(c);
        return _alphabet.toChar(invert(num));
    }

    /** Return the alphabet used to initialize this Permutation. */
    Alphabet alphabet() {
        return _alphabet;
    }
    String cycles() {
        return cycle;
    }

    /** Return true iff this permutation is a derangement (i.e., a
     *  permutation for which no value maps to itself). */
    boolean derangement() {
        int counter = 0;
        for (int i = 0; i < _cycles.size(); i++) {
            counter += _cycles.get(i).length();
        }
        if (counter == _alphabet.size()) {
            return true;
        }
        return false;
    }

    /** Alphabet of this permutation. */
    private Alphabet _alphabet;
    /** Cycles of this permutation. */
    private ArrayList<String> _cycles;
    /** Certain cycle of this permutation. */
    private String cycle;

    /** Current setting. */
    private int _setting;

}
