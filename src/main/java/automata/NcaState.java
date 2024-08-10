package automata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements the state of a NCA.
 *
 * Stores information that is associated with a state of the NCA.
 */
public class NcaState {
    final Token token;
    final int id;
    final boolean isStart;
    final protected boolean isFinal;
    public final HashMap<String, Set<NcaTransition>> transitions = new HashMap<>();

    public NcaState(Token token, boolean isFinal, int id) {
        this.isFinal = isFinal;
        if (token.type == TokenType.START_TOKEN) {
            this.isStart = true;
            assert token.id == 0;
        } else {
            this.isStart = false;
        }
        this.token = token;
        this.id = id;
        assert id == token.id;
        assert (
                token.type == TokenType.CHAR_CLASS
                        || token.type == TokenType.CHAR
                        || token.type == TokenType.START_TOKEN
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NcaState)) {
            return false;
        }
        NcaState that = (NcaState) obj;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("(id=%d, symbol=%s)", id, token.symbol);
    }

    public void addTransition(String symbol, NcaTransition transition) {
        if (transitions.containsKey(symbol)) {
            Set<NcaTransition> transitionSet = transitions.get(symbol);
            transitionSet.add(transition);
        } else {
            Set<NcaTransition> transitionSet = new HashSet<>();
            transitionSet.add(transition);
            transitions.put(symbol, transitionSet);
        }
    }
}
