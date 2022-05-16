package enigma;

import java.util.ArrayList;
import java.util.Collection;

import static enigma.EnigmaException.*;

/** Class that represents a complete enigma machine.
 *  @author V.Dabholkar
 */
class Machine {

    /** A new Enigma machine with alphabet ALPHA, 1 < NUMROTORS rotor slots,
     *  and 0 <= PAWLS < NUMROTORS pawls.  ALLROTORS contains all the
     *  available rotors. */
    Machine(Alphabet alpha, int numRotors, int pawls,
            Collection<Rotor> allRotors) {
        _alphabet = alpha;
        rotorNums = numRotors;
        _pawls = pawls;
        _perms = new Permutation(_alphabet.chars(), _alphabet);
        _allRotors = allRotors.toArray(new Rotor[0]);
        _allRotornames = new ArrayList<>();
        for (Rotor rotor: allRotors) {
            _allRotornames.add(rotor.name());
        }
        _rotors = new ArrayList<>();
    }

    /** Return the number of rotor slots I have. */
    int numRotors() {
        return rotorNums;
    }

    /** Return the number pawls (and thus rotating rotors) I have. */
    int numPawls() {
        return _pawls;
    }

    /** Return Rotor #K, where Rotor #0 is the reflector, and Rotor
     *  #(numRotors()-1) is the fast Rotor.  Modifying this Rotor has
     *  undefined results. */
    Rotor getRotor(int k) {
        if (k < _allRotornames.size() && k >= 0) {
            return _rotors.get(k);
        }
        throw enigma.EnigmaException.error("index out of range");
    }

    Alphabet alphabet() {
        return _alphabet;
    }

    /** Set my rotor slots to the rotors named ROTORS from my set of
     *  available rotors (ROTORS[0] names the reflector).
     *  Initially, all rotors are set at their 0 setting. */
    void insertRotors(String[] rotors) {
        if (_rotors != null) {
            _rotors = new ArrayList<>();
        }
        for (String rotor: rotors) {
            for (int i = 0; i < _allRotornames.size(); i++) {
                if (rotor.equals(_allRotornames.get(i))) {
                    _rotors.add(_allRotors[i]);
                }
            }
        }
    }

    /** Set my rotors according to SETTING, which must be a string of
     *  numRotors()-1 characters in my alphabet. The first letter refers
     *  to the leftmost rotor setting (not counting the reflector).  */
    void setRotors(String setting) {
        if (setting.length() != rotorNums - 1) {
            throw enigma.EnigmaException.error("not in alphabet");
        }
        for (int i = 1; i < numRotors(); i++) {
            if (setting.length() < i - 1) {
                throw enigma.EnigmaException.error("not enough rotors");
            }
            char x = setting.charAt(i - 1);
            if (!_alphabet.contains(x)) {
                throw enigma.EnigmaException.error("not in alphabet");
            }
            _rotors.get(i).set(x);
        }
    }

    /** Return the current plugboard's permutation. */
    Permutation plugboard() {
        return _plugboard;
    }

    /** Set the plugboard to PLUGBOARD. */
    void setPlugboard(Permutation plugboard) {
        _plugboard = plugboard;
    }

    /** Returns the result of converting the input character C (as an
     *  index in the range 0..alphabet size - 1), after first advancing
     *  the machine. */
    int convert(int c) {
        advanceRotors();
        if (Main.verbose()) {
            System.err.printf("[");
            for (int r = 1; r < numRotors(); r += 1) {
                System.err.printf("%c",
                        alphabet().toChar(getRotor(r).setting()));
            }
            System.err.printf("] %c -> ", alphabet().toChar(c));
        }
        if (!_plugboard.cycles().equals("")) {
            c = plugboard().permute(c);
        }
        if (Main.verbose()) {
            System.err.printf("%c -> ", alphabet().toChar(c));
        }
        c = applyRotors(c);
        if (!_plugboard.cycles().equals("")) {
            c = plugboard().invert(c);
        }
        if (Main.verbose()) {
            System.err.printf("%c%n", alphabet().toChar(c));
        }
        return c;
    }

    /** Advance all rotors to their next position. */
    private void advanceRotors() {
        boolean[] movable = new boolean[rotorNums];
        for (int i = 0; i < rotorNums; i += 1) {
            Rotor rotable = _rotors.get(i);
            if ((i == rotorNums - 1)
                    || (rotable.rotates() && _rotors.get(i + 1).atNotch())) {
                movable[i] = true;
            }
        }
        for (int i = 0; i < rotorNums; i++) {
            if (movable[i]) {
                _rotors.get(i).advance();
                if (i < rotorNums - 1) {
                    _rotors.get(i + 1).advance();
                }
                i++;
            }
        }
    }


    /** Return the result of applying the rotors to the character C (as an
     *  index in the range 0..alphabet size - 1). */
    private int applyRotors(int c) {
        for (int i = numRotors() - 1; i >= 0; i--) {
            c = _rotors.get(i).convertForward(c);
        }
        for (int i = 1; i < numRotors(); i++) {
            c = _rotors.get(i).convertBackward(c);
        }
        return c;
    }

    /** Returns the encoding/decoding of MSG, updating the state of
     *  the rotors accordingly. */
    String convert(String msg) {
        String msgStr = "";
        for (int i = 0; i < msg.length(); i++) {
            msgStr += _alphabet.toChar(convert(_alphabet.toInt(msg.charAt(i))));
        }
        return msgStr;
    }

    ArrayList<Rotor> getRotors() {
        return _rotors;
    }

    void ringSetting(String ringSetting) {
        for (int i = 1; i < rotorNums; i += 1) {
            char alphaSet = ringSetting.charAt(i - 1);
            _rotors.get(i).ringAlphabet(alphaSet);
        }
    }

    /** Common alphabet of my rotors. */
    private final Alphabet _alphabet;

    /** Number of rotors and pawls of my rotors. */
    private int rotorNums, _pawls;

    /** Permutations of my rotors. */
    private Permutation _perms;

    /** All my rotors. */
    private Rotor[] _allRotors;

    /** Rotors used in this example. */
    private ArrayList<Rotor> _rotors;

    /** Has intro perms if any. */
    private Permutation _plugboard;

    /** My rotor names. */
    private ArrayList<String> _allRotornames;


}
