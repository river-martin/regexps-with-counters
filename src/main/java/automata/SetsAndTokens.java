package automata;
import java.util.*;

/**
 * A container for the sets computed by Glushkov's construction algorithm and a list of Tokens.
 *
 * I wrote this so that I could return both of these from the computeSetsAndGetTokens function in App.java
 */
public class SetsAndTokens {
    final Sets sets;
    final List<Token> stateTokens;
    public SetsAndTokens(Sets sets, List<Token> stateTokens) {
        this.stateTokens = stateTokens;
        this.sets = sets;
    }
}
