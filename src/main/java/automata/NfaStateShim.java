package automata;

import java.util.*;

import automata.NFA.NfaState;

/**
 * A container for NcaStates and counter values.
 * Used to pass the NcaState and counter values to the NFA builder.
 */
public class NfaStateShim extends NfaState {
    // The NFA builder uses these temporary objects to create the actual NFA states
    // with valid IDs.
    private static final int SHIM_ID = -1;

    public NfaStateShim(NcaState ncaState, HashMap<Integer, Integer> counterVals) {
        super(SHIM_ID, ncaState, counterVals);
    }

    public static String shimString(NcaState state, HashMap<Integer, Integer> counterVals) {
        StringBuilder counterValstring = new StringBuilder("[");
        boolean hasCounter = false;
        for (Integer counterId : counterVals.keySet()) {
            counterValstring.append("c").append(counterId).append("=").append(counterVals.get(counterId)).append(", ");
            hasCounter = true;
        }
        int end = counterValstring.length();
        if (hasCounter) {
            end = end - 2;
        }
        counterValstring = new StringBuilder(counterValstring.substring(0, end) + "]");
        return String.format("{s_id=%d, s_sym=%s, c_vals=%s}", state.id, state.token.symbol, counterValstring);
    }

    @Override
    public String toString() {
        return shimString(ncaState, counterVals);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NfaStateShim)) {
            return false;
        }
        NfaStateShim that = (NfaStateShim) obj;
        return this.ncaState.equals(that.ncaState) && this.counterVals.equals(that.counterVals);
    }

    @Override
    public int hashCode() {
        return ncaState.hashCode() + counterVals.hashCode();
    }
}
