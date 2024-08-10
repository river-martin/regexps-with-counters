package automata;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A container for a sequence of tokens.
 *
 * Used to concatenate tokens during Glushkov's construction algorithm.
 */
public class TokenString implements Comparable<TokenString> {
    protected final ArrayList<Token> tokens;
    // Used to determine which backward transitions belong to which counter/star.
    protected final Token transitionToken;

    /**
     * Used to construct strings of length 1.
     */
    public TokenString(Token token) {
        tokens = new ArrayList<>();
        tokens.add(token);
        transitionToken = null;
    }

    public TokenString(ArrayList<Token> tokens) {
        this.tokens = tokens;
        transitionToken = null;
    }

    public TokenString(ArrayList<Token> tokens, Token transitionToken) {
        this.tokens = tokens;
        this.transitionToken = transitionToken;
    }

    /**
     * Used to construct the empty string.
     */
    public TokenString() {
        tokens = new ArrayList<>();
        transitionToken = null;
    }

    public TokenString concatenate(TokenString suffix) {
        ArrayList<Token> concatenated = new ArrayList<>();
        concatenated.addAll(tokens);
        concatenated.addAll(suffix.tokens);
        return new TokenString(concatenated);
    }

    public TokenString concatenate(TokenString suffix, Token transitionToken) {
        ArrayList<Token> concatenated = new ArrayList<>();
        concatenated.addAll(tokens);
        concatenated.addAll(suffix.tokens);
        return new TokenString(concatenated, transitionToken);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TokenString)) {
            return false;
        }
        TokenString that = (TokenString) other;
        if (!Arrays.deepEquals(this.tokens.toArray(), that.tokens.toArray())) {
            return false;
        }
        if (this.transitionToken != null) {
            return this.transitionToken.equals(that.transitionToken);
        }
        return that.transitionToken == null;
    }

    @Override
    public int hashCode() {
        int hash = Arrays.deepHashCode(tokens.toArray());
        if (this.transitionToken != null) {
            hash += transitionToken.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        return Arrays.toString(tokens.toArray()) + " " + transitionToken;
    }

    @Override
    public int compareTo(TokenString other) {
        for (int i = 0; i < tokens.size(); i++) {
            if (other.tokens.size() <= i) {
                return 1;
            } else {
                int cmp = tokens.get(i).symbol.compareTo(other.tokens.get(i).symbol);
                if (cmp == 0) {
                    cmp = tokens.get(i).id - other.tokens.get(i).id;
                }
                if (cmp != 0) {
                    return cmp;
                }
            }
        }
        if (other.tokens.size() > tokens.size()) {
            return -1;
        } else {
            return 0;
        }
    }

}