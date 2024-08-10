package automata;

import java.util.*;

/**
 * Implements a nondeterministic finite automaton.
 *
 * Contains the code that implements the string matcher (using a form of on the
 * fly subset construction).
 *
 * Contains the code that constructs an NFA from an NCA (in the constructor of
 * this class). This is done by starting at
 * the start state and then visiting all the other states in the NCA; by taking
 * all available transitions.
 */
public class NFA {
    final HashMap<Integer, NfaState> nfaStates;
    final Set<NfaState> finalStates;
    final int startID;
    protected final String regex;

    public static class NfaState {
        final HashMap<String, List<NfaState>> transitions;
        final int id;
        protected final NcaState ncaState;
        public final HashMap<Integer, Integer> counterVals;

        public NfaState(int id, NcaState ncaState, HashMap<Integer, Integer> counterVals) {
            this.id = id;
            this.ncaState = ncaState;
            this.counterVals = counterVals;
            transitions = new HashMap<>();
        }

        @Override
        public String toString() {

            return String.format("(id=%d, shim=%s)", id, NfaStateShim.shimString(ncaState, counterVals));
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
        HashMap<NfaStateShim, NfaState> shimsToNfaStates = new HashMap<>();
        Queue<NfaState> newNfaStates = new ArrayDeque<>();

        int id = nca.size();
        startID = id;
        NfaStateShim startShim = new NfaStateShim(nca.startState(), new HashMap<>());
        NcaState ncaStart = nca.startState();
        // It is unnecessary to keep track of the counter values for the start state.
        HashMap<Integer, Integer> counterVals = new HashMap<>();

        NfaState nfaStartState = new NfaState(id++, ncaStart, counterVals);
        shimsToNfaStates.put(startShim, nfaStartState);

        nfaStates.put(nfaStartState.id, nfaStartState);
        newNfaStates.add(nfaStartState);

        NfaState nfaState;
        while (newNfaStates.size() > 0) {
            nfaState = newNfaStates.remove();
            if (nca.evaluateFinalizationFunction(nfaState.ncaState, nfaState.counterVals)) {
                finalStates.add(nfaState);
            }
            for (String symbol : nfaState.ncaState.transitions.keySet()) {
                List<NfaState> nfaTransitions = new ArrayList<>();
                for (NfaStateShim nextShim : nca.evaluateTransitionFunction(nfaState.ncaState, nfaState.counterVals,
                        symbol)) {
                    NfaState dest;
                    if (shimsToNfaStates.containsKey(nextShim)) {
                        // The NFA state already exists.
                        dest = shimsToNfaStates.get(nextShim);
                        assert dest != null;
                        assert nfaStates.containsKey(dest.id);
                        assert nfaStates.get(dest.id) == dest;
                        nfaTransitions.add(dest);
                    } else {
                        // The NFA state does not exist yet.
                        dest = new NfaState(id++, nextShim.ncaState, nextShim.counterVals);
                        shimsToNfaStates.put(nextShim, dest);
                        nfaStates.put(dest.id, dest);
                        newNfaStates.add(dest);
                    }
                    nfaTransitions.add(dest);
                }
                nfaState.transitions.put(symbol, nfaTransitions);
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
                stateString, startStates, finalStatesString, transitions);
    }
}
