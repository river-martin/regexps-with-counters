package nca;
import java.util.*;

/**
 * Used to represent the numbered characters / character classes and the operation symbols (counter, *, |, +, etc..)
 *
 * See TokenType.java
 */
public class Token {
    // counterIDs
    // global hashmap of counterIDs to counters.
    final String symbol;
    final int id;
    final TokenType type;
    protected CounterRange counterRange;
    protected final List<CounterRange> countersIncrementedHere = new ArrayList<>();
    protected final List<CounterRange> countersInitializedHere = new ArrayList<>();
    protected final List<CounterRange> associatedCounterRanges = new ArrayList<>();
    protected final List<Token> starsEndingHere = new ArrayList<>();

    protected CounterRange getLastCounter() {
        return countersIncrementedHere.get(countersIncrementedHere.size() - 1);
    }

    protected Token getLastStar() {
        return starsEndingHere.get(starsEndingHere.size() - 1);
    }

    protected boolean lastQuantifierIsCounter() {
        // XXX: are these in the correct order? they should be?
        if (countersIncrementedHere.size() < 1) {
            return false;
        }
        if (starsEndingHere.size() < 1) {
            return true;
        }
        return getLastCounter().id > getLastStar().id;
    }

    protected boolean lastQuantifierIsStar() {
        // XXX: are these in the correct order? they should be?
        if (starsEndingHere.size() < 1) {
            return false;
        }
        if (countersIncrementedHere.size() < 1) {
            return true;
        }
        return getLastCounter().id < getLastStar().id;
    }

    public Token(String symbol, TokenType type, CounterRange counter) {
        this.counterRange = counter;
        this.symbol = symbol;
        this.type = type;
        id = -1;
    }

    public Token(String symbol, TokenType type) {
        this.symbol = symbol;
        id = -1;
        this.type = type;
    }

    public Token(String symbol, int id, TokenType type) {
        this.symbol = symbol;
        this.id = id;
        this.type = type;
    }

    @Override
    public String toString() {
        return "(" + symbol + ", " + id + ")";
    }

    @Override
    public int hashCode() {
        // XXX: implement counter's hashcode.
        int hash = symbol.hashCode() + Integer.valueOf(id).hashCode() + type.hashCode();
        if (counterRange != null) {
            hash += counterRange.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Token)) {
            return false;
        }
        Token that = (Token) other;
        return (
                   this.symbol.equals(that.symbol) && this.id == that.id && this.type == that.type
                   && this.counterRange.equals(that.counterRange)
               );
    }

}
