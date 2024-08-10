package automata;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cli.App;

/**
 * Constructs the full product NFA from an NFA. (See the constructor of this
 * class)
 *
 * Contains the code that explores the product NFA to determine if the original
 * NFA/regex is counter-ambiguous.
 * Exact analysis and approximate analysis are implemented here. Exact analysis
 * is implemented in the isAmbiguous method.
 * Approximate analysis is implemented in the mightBeAmbiguous method.
 */
public class ProductNFA {
    final String COUNTER_MATCHING_REGEX = "\\{(\\d+|\\d+,\\d+|\\d+,)}";
    final String regex;
    final Set<State> stateSet = new HashSet<>();
    final State root;
    final boolean shouldPrintApproximateRegexs = false;

    private static class State {
        final NFA.NfaState a;
        final NFA.NfaState b;
        final HashMap<String, List<State>> transitions = new HashMap<>();

        public State(NFA.NfaState a, NFA.NfaState b) {
            this.a = a;
            this.b = b;
        }

        public boolean isAmbiguous() {
            return a.ncaState.equals(b.ncaState)
                    && !a.counterVals.equals(b.counterVals);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof State)) {
                return false;
            }
            State that = (State) obj;
            return this.a.equals(that.a) && this.b.equals(that.b);
        }

        @Override
        public int hashCode() {
            return a.hashCode() + b.hashCode();
        }

        @Override
        public String toString() {
            String shimStringA = NfaStateShim.shimString(a.ncaState, a.counterVals);
            String shimStringB = NfaStateShim.shimString(b.ncaState, b.counterVals);
            return String.format("{shimA=%s, shimB=%s}", shimStringA, shimStringB);
        }
    }

    private List<Integer> getIndices(String s, String p) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            if (s.substring(i).startsWith(p)) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * @return true if the regular expression might be ambiguous, false if it is
     *         unambiguous.
     */
    public boolean mightBeAmbiguous() {
        Pattern pattern = Pattern.compile(COUNTER_MATCHING_REGEX);
        Matcher matcher = pattern.matcher(regex);
        Set<String> counters = new HashSet<>();
        while (matcher.find()) {
            counters.add(matcher.group());
        }
        assert counters.size() > 0;
        int numApproximateRegexsChecked = 0;
        for (String counter : counters) {
            List<Integer> indices = getIndices(regex, counter);
            for (int index : indices) {
                String pref = regex.substring(0, index);
                pref = pref.replaceAll(COUNTER_MATCHING_REGEX, "*");
                String suff = regex.substring(index + counter.length());
                suff = suff.replaceAll(COUNTER_MATCHING_REGEX, "*");
                assert !(pref == "" && suff == "");
                String approx = pref + counter + suff;
                if (shouldPrintApproximateRegexs) {
                    System.out.printf("Approximate regex %d:\n", numApproximateRegexsChecked);
                    System.out.println(approx);
                }
                NCA nca = NCA.glushkov(approx);
                NFA nfa = new NFA(nca);
                ProductNFA approxNfa = new ProductNFA(nfa);
                if (approxNfa.isAmbiguous()) {
                    return true;
                }
                numApproximateRegexsChecked++;
            }
        }
        // If there was at most one counter, the
        // exact analysis judged it as unambiguous.
        return numApproximateRegexsChecked > 1;
    }

    public boolean isAmbiguous() {
        return !findAmbiguities().isEmpty();
    }

    public Set<State> findAmbiguities() {
        Set<State> ambiguousStates = new HashSet<>();
        Set<State> visited = new HashSet<>();
        Queue<State> unvisited = new ArrayDeque<>();
        unvisited.add(root);
        while (!unvisited.isEmpty()) {
            State s1 = unvisited.remove();
            if (s1.isAmbiguous()) {
                ambiguousStates.add(s1);
            }
            for (String symbol : s1.transitions.keySet()) {
                for (State s2 : s1.transitions.get(symbol)) {
                    if (!visited.contains(s2)) {
                        unvisited.add(s2);
                    }
                }
            }
            visited.add(s1);
        }
        return ambiguousStates;
    }

    public ProductNFA(NFA nfa) {
        regex = nfa.regex;
        NFA.NfaState nfaRoot = nfa.nfaStates.get(nfa.startID);
        root = new State(nfaRoot, nfaRoot);
        Queue<State> unvisited = new ArrayDeque<>();
        unvisited.add(root);
        while (!unvisited.isEmpty()) {
            State s1 = unvisited.remove();
            for (String symbol : s1.a.transitions.keySet()) {
                s1.transitions.put(symbol, new ArrayList<>());
                for (NFA.NfaState destA : s1.a.transitions.get(symbol)) {
                    if (!s1.b.transitions.containsKey(symbol)) {
                        // s1.b has no transition on this symbol
                        continue;
                    }
                    for (NFA.NfaState destB : s1.b.transitions.get(symbol)) {
                        // if destA.id <= destB.ID?
                        State s2 = new State(destA, destB);
                        if (!stateSet.contains(s2)) {
                            stateSet.add(s2);
                            unvisited.add(s2);
                        }
                        List<State> transitionList = s1.transitions.get(symbol);
                        transitionList.add(s2);
                    }
                }
            }
        }
    }
}
