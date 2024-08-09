package nca;

import java.util.*;

/**
 * Implements a nondeterministic counter automata.
 */
public class NCA {
    private final NcaState[] states;
    protected final String regex;

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
                        associatedCounterRanges.substring(0, associatedCounterRanges.length() - 2)
                );
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
                            Arrays.toString(counters)
                    ));
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
                   Arrays.toString(states), startStates, finalConfigs, associatedCounterRanges, transitions
               );
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
            // Check all counters on this transition. (the quantifier token should be the last counter/star on it)
            transition = new NcaTransition(
                src.countersIncrementedHere,
                NcaTransitionType.CONDITIONAL_FORWARD,
                states[dest.id]
            );
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
                transitionToken
            );
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
                transitionToken
            );
            states[src.id].addTransition(dest.symbol, transition);
        } else {
            System.out.println("Failed to add backward transition. Invalid transition token.");
        }
    }

    public boolean evaluateFinalizationFunction(Configuration config) {
        if (config.state.isFinal) {
            for (CounterRange counterRange : config.state.token.countersIncrementedHere) {
                if (counterRange.isOutOfRange(config.counterValues.get(counterRange.id))) {
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

    public List<Configuration> evaluateTransitionFunction(Configuration currentConfig, String symbol) {
        List<Configuration> configs = new ArrayList<>();
        for (NcaTransition t : currentConfig.state.transitions.get(symbol)) {
            if (t.isAllowed(currentConfig.counterValues)) {
                Configuration config = new Configuration();
                config.state = t.dest;
                config.counterValues = t.getUpdatedCounterValues(currentConfig.counterValues);
                configs.add(config);
            }
        }
        return configs;
    }

    public Configuration getRootConfig() {
        Configuration config = new Configuration();
        config.state = states[0];
        config.counterValues = new HashMap<>();
        return config;
    }

    public int size() {
        return states.length;
    }

}
