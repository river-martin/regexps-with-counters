package automata;

import java.util.*;

import regexlang.QuantExprRewriteVisitor;

/**
 * Implements a nondeterministic counter automata.
 */
public class NCA {
    private final NcaState[] states;
    public final String regex;

    @Override
    public String toString() {
        StringBuilder associatedCounterRanges = new StringBuilder();
        StringBuilder transitions = new StringBuilder();
        StringBuilder startStates = new StringBuilder();
        StringBuilder finalConfigs = new StringBuilder();
        for (NcaState s : states) {
            associatedCounterRanges.append(s.id).append(":\n");
            if (s.token.associatedCounterRanges.size() > 0) {
                for (CounterRange r : s.token.associatedCounterRanges) {
                    associatedCounterRanges.append(r).append(", ");
                }
                associatedCounterRanges = new StringBuilder(
                        associatedCounterRanges.substring(0, associatedCounterRanges.length() - 2));
                associatedCounterRanges.append("\n");
            }
            transitions.append("Source = ").append(s).append(":\n");
            for (String symbol : s.transitions.keySet()) {
                for (NcaTransition t : s.transitions.get(symbol)) {
                    transitions.append(symbol).append(" -> ").append(t).append("\n");
                }
            }
            if (s.isStart) {
                startStates.append(s).append("\n");
            }
            if (s.isFinal) {
                finalConfigs.append(s);
                if (s.token.countersIncrementedHere.size() > 0) {
                    // XXX
                    CounterRange[] counters = new CounterRange[s.token.countersIncrementedHere.size()];
                    s.token.countersIncrementedHere.toArray(counters);
                    finalConfigs.append(String.format(" when counters %s are within their ranges.\n",
                            Arrays.toString(counters)));
                } else {
                    finalConfigs.append("\n");
                }
            }
        }
        return String.format(
                "States:\n---\n%s\n---\n" +
                        "Start States:\n---\n%s---\n" +
                        "Final configurations:\n---\n%s---\n" +
                        "State counter ranges:\n---\n%s---\n" +
                        "Transitions:\n---\n%s---",
                Arrays.toString(states), startStates, finalConfigs, associatedCounterRanges, transitions);
    }

    public NCA(SetsAndTokens setsAndTokens, String regex) {
        this.regex = regex;
        int id = 0;
        boolean isFinal = setsAndTokens.sets.l.size() > 0;
        states = new NcaState[setsAndTokens.stateTokens.size() + 1];
        // Add the start state.
        Token startToken = new Token("", id, TokenType.START_TOKEN);
        states[id++] = new NcaState(startToken, isFinal, startToken.id);
        // Add the final states.
        for (TokenString tokenString : setsAndTokens.sets.d) {
            assert (tokenString.tokens.size() == 1);
            Token token = tokenString.tokens.get(0);
            // Remove state ID?
            states[token.id] = new NcaState(token, true, token.id);
        }
        // Add the other states.
        for (Token token : setsAndTokens.stateTokens) {
            if (states[id] != null) {
                id++;
            } else {
                states[id] = new NcaState(token, false, id++);
            }
        }
        assert (id == states.length);
        // Add the transitions from the start state to the states in P
        for (TokenString tokenString : setsAndTokens.sets.p) {
            assert (tokenString.tokens.size() == 1);
            int destID = tokenString.tokens.get(0).id;
            addTransition(states[0], states[destID]);
        }
        // Add the transitions associated with F
        for (TokenString tokenString : setsAndTokens.sets.f) {
            assert (tokenString.tokens.size() == 2);
            Token src = tokenString.tokens.get(0);
            Token dest = tokenString.tokens.get(1);
            assert tokenString.transitionToken != null;
            switch (tokenString.transitionToken.type) {
                case CONCAT:
                    addForwardTransition(src, dest);
                    break;
                case STAR:
                case COUNTER:
                    addBackwardTransition(src, dest, tokenString.transitionToken);
                    break;
                default:
                    System.out.println("This code shouldn't be reached.");
                    break;
            }
        }
    }

    public void addForwardTransition(Token src, Token dest) {
        NcaTransition transition;
        if (src.lastQuantifierIsCounter() || src.lastQuantifierIsStar()) {
            // Check all counters on this transition. (the quantifier token should be the
            // last counter/star on it)
            transition = new NcaTransition(
                    src.countersIncrementedHere,
                    NcaTransitionType.CONDITIONAL_FORWARD,
                    states[dest.id]);
        } else {
            assert src.countersIncrementedHere.size() == 0 && src.starsEndingHere.size() == 0;
            transition = new NcaTransition(NcaTransitionType.UNCONDITIONAL, states[dest.id]);
        }
        states[src.id].addTransition(dest.symbol, transition);
    }

    private void addBackwardTransition(Token src, Token dest, Token transitionToken) {
        if (transitionToken.counterRange != null) {
            // Backward counter transition
            CounterRange counterToIncrement = transitionToken.counterRange;
            assert src.countersIncrementedHere.contains(counterToIncrement);
            assert dest.countersInitializedHere.contains(counterToIncrement);
            int end = src.countersIncrementedHere.indexOf(counterToIncrement);
            List<CounterRange> transitionCounters = src.countersIncrementedHere.subList(0, end + 1);
            NcaTransition transition = new NcaTransition(
                    transitionCounters,
                    NcaTransitionType.CONDITIONAL_BACKWARD_COUNTER,
                    states[dest.id],
                    transitionToken);
            states[src.id].addTransition(dest.symbol, transition);
        } else if (transitionToken.type == TokenType.STAR) {
            // Backward star transition.
            int i;
            for (i = 0; i < src.countersIncrementedHere.size(); i++) {
                CounterRange counterRange = src.countersIncrementedHere.get(i);
                if (counterRange.id > transitionToken.id) {
                    break;
                }
            }
            List<CounterRange> transitionCounters = src.countersIncrementedHere.subList(0, i);
            NcaTransition transition = new NcaTransition(
                    transitionCounters,
                    NcaTransitionType.CONDITIONAL_BACKWARD_STAR,
                    states[dest.id],
                    transitionToken);
            states[src.id].addTransition(dest.symbol, transition);
        } else {
            System.out.println("Failed to add backward transition. Invalid transition token.");
        }
    }

    public boolean evaluateFinalizationFunction(NcaState ncaState, HashMap<Integer, Integer> counterVals) {
        if (ncaState.isFinal) {
            for (CounterRange counterRange : ncaState.token.countersIncrementedHere) {
                if (counterRange.isOutOfRange(counterVals.get(counterRange.id))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private void addTransition(NcaState src, NcaState dest) {
        assert src.isStart;
        NcaTransition transition = new NcaTransition(NcaTransitionType.UNCONDITIONAL, dest);
        src.addTransition(dest.token.symbol, transition);
    }

    public List<NfaStateShim> evaluateTransitionFunction(NcaState currentState,
            HashMap<Integer, Integer> currentCounterVals, String symbol) {
        List<NfaStateShim> nextNfaStates = new ArrayList<>();
        for (NcaTransition t : currentState.transitions.get(symbol)) {
            if (t.isAllowed(currentCounterVals)) {
                NfaStateShim nextState = new NfaStateShim(t.dest, t.getUpdatedCounterVals(currentCounterVals));
                nextNfaStates.add(nextState);
            }
        }
        return nextNfaStates;
    }

    public NcaState startState() {
        return states[0];
    }

    public int size() {
        return states.length;
    }

    private static void computeSetsForCounter(Stack<Sets> stackedSets, Sets currentSets,
            Token counterToken, List<Token> stateTokens) {
        Sets oldSets = stackedSets.pop();
        int maxID = -3;
        for (TokenString ts : oldSets.d) {
            if (ts.tokens.size() < 1) {
                break;
            }
            assert ts.tokens.size() == 1;
            Token t = ts.tokens.get(0);
            t.countersIncrementedHere.add(counterToken.counterRange);
            if (maxID < t.id) {
                // Find the max token ID so that we can compute the set of states associated
                // with this counter.
                maxID = t.id;
            }
        }
        TokenString emptyString = new TokenString();
        if (counterToken.counterRange.lowerBound <= 0) {
            // Apply the same rules as * for L
            currentSets.l.add(emptyString);
        } else {
            // Keep the old L.
            currentSets.l.addAll(oldSets.l);
            // XXX: might break reachability analysis?
            if (currentSets.l.contains(emptyString)) {
                // The empty string can pad for counts lower than the original lower bound.
                counterToken.counterRange.lowerBound = 0;
            }
        }
        // Always apply the same rules as * for P and D.
        currentSets.p.addAll(oldSets.p);
        currentSets.d.addAll(oldSets.d);
        if (counterToken.counterRange.upperBound >= 2) {
            // Apply the same rules as * for F, with a counter between d and p.
            currentSets.f.addAll(oldSets.f);
            currentSets.f.addAll(concatAndSaveTransitionToken(oldSets.d, oldSets.p, counterToken));
        } else {
            // Keep the old F.
            currentSets.f.addAll(oldSets.f);
        }
        int minID = Integer.MAX_VALUE;
        for (TokenString ts : currentSets.p) {
            assert ts.tokens.size() == 1;
            Token t = ts.tokens.get(0);
            t.countersInitializedHere.add(counterToken.counterRange);
            if (minID > t.id) {
                minID = t.id;
            }
        }
        for (Token t : stateTokens) {
            if (minID <= t.id && t.id <= maxID) {
                t.associatedCounterRanges.add(counterToken.counterRange);
            }
        }
        stackedSets.push(currentSets);
    }

    private static void computeSetsForStar(Stack<Sets> stackedSets, Sets currentSets, Token starToken) {
        Sets oldSets = stackedSets.pop();
        for (TokenString ts : oldSets.d) {
            if (ts.tokens.size() < 1) {
                break;
            }
            assert ts.tokens.size() == 1;
            Token t = ts.tokens.get(0);
            t.starsEndingHere.add(starToken);
        }
        TokenString emptyString = new TokenString();
        currentSets.l.add(emptyString);
        currentSets.p.addAll(oldSets.p);
        currentSets.d.addAll(oldSets.d);
        currentSets.f.addAll(oldSets.f);
        currentSets.f.addAll(concatAndSaveTransitionToken(oldSets.d, oldSets.p, starToken));
        stackedSets.push(currentSets);
    }

    private static void computeSetsForChar(Stack<Sets> stackedSets, Sets currentSets, Token token) {
        // l = emptySet
        TokenString letter = new TokenString(token);
        currentSets.p.add(letter);
        currentSets.d.add(letter);
        stackedSets.push(currentSets);
        // f = emptySet
    }

    private static boolean currentTokenIsLastInGroup(Token nextToken) {
        if (nextToken == null) {
            return true;
        }
        switch (nextToken.type) {
            case COUNTER:
            case STAR:
                return false;
            default:
                return true;
        }
    }

    private static Set<TokenString> concatAndSaveTransitionToken(Set<TokenString> prefixes, Set<TokenString> suffixes,
            Token transitionToken) {
        Set<TokenString> newSet = new HashSet<>();
        for (TokenString p : prefixes) {
            for (TokenString s : suffixes) {
                newSet.add(p.concatenate(s, transitionToken));
            }
        }
        return newSet;
    }

    static SetsAndTokens computeSetsAndGetStateTokens(String regex) {
        char[] s = new char[regex.length()];
        regex.getChars(0, regex.length(), s, 0);
        MyScanner.initScanner(s);
        Token token = MyScanner.getToken();
        List<Token> stateTokens = new ArrayList<>();
        Stack<Sets> stackedSets = new Stack<>();
        Stack<Token> ops = new Stack<>();
        Token concat = new Token("", TokenType.CONCAT);
        while (token != null) {
            Sets currentSets = new Sets();
            Token nextToken = MyScanner.getToken();
            switch (token.type) {
                case L_PAR:
                    ops.push(token);
                    break;
                case R_PAR:
                    while (ops.peek().type != TokenType.L_PAR) {
                        applyOperation(stackedSets, ops);
                    }
                    ops.pop();
                    break;

                case COUNTER:
                    computeSetsForCounter(stackedSets, currentSets, token, stateTokens);
                    break;
                case STAR:
                    computeSetsForStar(stackedSets, currentSets, token);
                    break;

                case CHAR:
                case CHAR_CLASS:
                    computeSetsForChar(stackedSets, currentSets, token);
                    break;

                case BAR:
                    assert ops.isEmpty() || ops.peek().type == TokenType.L_PAR;
                    while (ops.size() > 0 && ops.peek().type != TokenType.L_PAR) {
                        applyOperation(stackedSets, ops);
                    }
                    ops.push(token);
                    break;

                case PLUS:
                    // Translate + to {1,}

                    token = new Token("{1,}", TokenType.COUNTER);
                    computeSetsForCounter(stackedSets, currentSets, token, stateTokens);
                    break;

                default:
                    throw new IllegalArgumentException(
                            String.format("Regex `%s` contains unsupported token of type `%s`", regex, token.type));
            }
            switch (token.type) {
                case BAR:
                case L_PAR:
                    break;
                default:
                    if (currentTokenIsLastInGroup(nextToken)) {
                        while (!ops.isEmpty() && ops.peek().type == TokenType.CONCAT) {
                            applyOperation(stackedSets, ops);
                        }
                        if (canConcatWith(nextToken)) {
                            ops.push(concat);
                        } else {
                            while (!ops.isEmpty() && ops.peek().type == TokenType.BAR) {
                                applyOperation(stackedSets, ops);
                            }
                        }
                    }
                    break;
            }
            if (token.type == TokenType.CHAR || token.type == TokenType.CHAR_CLASS) {
                stateTokens.add(token);
            }
            token = nextToken;
        }
        assert (stackedSets.size() == 1);
        return new SetsAndTokens(stackedSets.pop(), stateTokens);
    }

    private static boolean canConcatWith(Token nextToken) {
        if (nextToken == null) {
            return false;
        }
        switch (nextToken.type) {
            case CHAR:
            case CHAR_CLASS:
            case L_PAR:
                return true;
            default:
                return false;
        }
    }

    private static void applyOperation(Stack<Sets> stackedSets, Stack<Token> ops) {
        Token op = ops.pop();
        Sets f = stackedSets.pop();
        Sets e = stackedSets.pop();
        Sets newSets = new Sets();
        switch (op.type) {
            case BAR:
                newSets.l.addAll(e.l);
                newSets.l.addAll(f.l);
                newSets.p.addAll(e.p);
                newSets.p.addAll(f.p);
                newSets.d.addAll(e.d);
                newSets.d.addAll(f.d);
                newSets.f.addAll(e.f);
                newSets.f.addAll(f.f);
                break;
            case CONCAT:
                newSets.l.addAll(concat(e.l, f.l));
                newSets.p.addAll(e.p);
                newSets.p.addAll(concat(e.l, f.p));
                newSets.d.addAll(f.d);
                newSets.d.addAll(concat(e.d, f.l));
                newSets.f.addAll(e.f);
                newSets.f.addAll(f.f);
                newSets.f.addAll(concatAndSaveTransitionToken(e.d, f.p, op));
                break;
            default:
                System.out.println("This case should not be reached.");
                break;
        }
        stackedSets.push(newSets);
    }

    private static Set<TokenString> concat(Set<TokenString> prefixes, Set<TokenString> suffixes) {
        Set<TokenString> newSet = new HashSet<>();
        for (TokenString p : prefixes) {
            for (TokenString s : suffixes) {
                newSet.add(p.concatenate(s));
            }
        }
        return newSet;
    }

    public static NCA glushkov(String regex) {
        regex = QuantExprRewriteVisitor.rewriteUnboundedCounters(regex).getText().replace("<EOF>", "");
        SetsAndTokens setsAndTokens = computeSetsAndGetStateTokens(regex);
        return new NCA(setsAndTokens, regex);
    }

}
