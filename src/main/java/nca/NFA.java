package nca;

import java.util.*;

/**
 * Implements a nondeterministic finite automaton.
 *
 * Contains the code that implements the string matcher (using a form of on the fly subset construction).
 *
 * Contains the code that constructs an NFA from a NCA (in the constructor of this class). This is done by starting at
 * the start state and then visiting all the other states in the NCA; by taking all available transitions.
 */
public class NFA {
    final HashMap<Integer, NfaState> nfaStates;
    final Set<NfaState> finalStates;
    final int startID;
    protected final String regex;

    protected static class NfaState {
        final Configuration ncaConfig;
        final HashMap<String, List<NfaState>> transitions;
        final int id;

        public NfaState(int id, Configuration config) {
            this.id = id;
            ncaConfig = config;
            transitions = new HashMap<>();
        }

        @Override
        public String toString() {
            return String.format("(id=%d, config=%s)", id, ncaConfig.toString());
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NfaState)) {
                return false;
            }
            NfaState that = (NfaState) obj;
            return this.id == that.id;
        }
    }

    private boolean checkForCharacterMatch(String symbol, char c) {
        // Used to check if the symbol is a character class.
        String characterClassRegex = "\\.|\\[.*-.*]|\\[.*]|\\\\.";
        if (symbol.matches(characterClassRegex)) {
            return (c + "").matches(symbol);
        } else {
            return symbol.equals(c + "");
        }
    }

    private Set<NfaState> getNextMergedState(char c, Set<NfaState> ms) {
        Set<NfaState> nextMs = new HashSet<>();
        for (NfaState s : ms) {
            for (String symbol : s.transitions.keySet()) {
                if (checkForCharacterMatch(symbol, c)) {
                    nextMs.addAll(s.transitions.get(symbol));
                }
            }
        }
        return nextMs;
    }

    public boolean tryMatch(String input) {
        Set<NfaState> ms = new HashSet<>();
        // Add start state
        ms.add(nfaStates.get(startID));
        for (char c : input.toCharArray()) {
            ms = getNextMergedState(c, ms);
        }
        for (NfaState s : ms) {
            if (finalStates.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public NFA(NCA nca) {
        regex = nca.regex;
        nfaStates = new HashMap<>();
        finalStates = new HashSet<>();
        HashMap<Configuration, Integer> stateIds = new HashMap<>();
        Queue<NfaState> newStates = new ArrayDeque<>();
        int id = nca.size();
        startID = id;
        Configuration config = nca.getRootConfig();
        stateIds.put(config, id);
        NfaState state = new NfaState(id++, config);
        nfaStates.put(state.id, state);
        newStates.add(state);
        while (newStates.size() > 0) {
            state = newStates.remove();
            if (nca.evaluateFinalizationFunction(state.ncaConfig)) {
                finalStates.add(state);
            }
            for (String symbol : state.ncaConfig.state.transitions.keySet()) {
                List<NfaState> nfaTransitions = new ArrayList<>();
                for (Configuration nextConfig : nca.evaluateTransitionFunction(state.ncaConfig, symbol)) {
                    NfaState dest;
                    if (!stateIds.containsKey(nextConfig)) {
                        stateIds.put(nextConfig, id);
                        dest = new NfaState(id++, nextConfig);
                        nfaStates.put(dest.id, dest);
                        newStates.add(dest);
                    } else {
                        dest = nfaStates.get(stateIds.get(nextConfig));
                    }
                    nfaTransitions.add(dest);
                }
                state.transitions.put(symbol, nfaTransitions);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder transitions = new StringBuilder();
        StringBuilder startStates = new StringBuilder();
        StringBuilder finalStatesString = new StringBuilder();
        StringBuilder stateString = new StringBuilder();
        for (Integer key : nfaStates.keySet()) {
            NfaState s = nfaStates.get(key);
            transitions.append(s.id).append(":\n");
            for (String symbol : s.transitions.keySet()) {
                for (NfaState dest : s.transitions.get(symbol)) {
                    transitions.append(symbol).append(" -> ").append(dest).append("\n");
                }
            }
            if (s.id == startID) {
                startStates.append(s).append("\n");
            }
            if (finalStates.contains(s)) {
                finalStatesString.append(s).append("\n");
            }
            stateString.append(s).append("\n");
        }
        return String.format(
                   "States:\n---\n%s---\n" +
                   "Start States:\n---\n%s---\n" +
                   "Final states:\n---\n%s---\n" +
                   "Transitions:\n---\n%s---",
                stateString, startStates, finalStatesString, transitions
               );
    }
}
