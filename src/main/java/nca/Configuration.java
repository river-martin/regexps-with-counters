package nca;

import java.util.*;

/**
 * A container for NcaStates and counter values.
 * Used as part of the NFA states.
 */
public class Configuration {
    protected NcaState state;
    protected HashMap<Integer, Integer> counterValues;

    @Override
    public String toString() {
        StringBuilder counterValueString = new StringBuilder("[");
        boolean hasCounter = false;
        for (Integer counterId : counterValues.keySet()) {
            counterValueString.append("c").append(counterId).append("=").append(counterValues.get(counterId)).append(", ");
            hasCounter = true;
        }
        int end = counterValueString.length();
        if (hasCounter) {
            end = end - 2;
        }
        counterValueString = new StringBuilder(counterValueString.substring(0, end) + "]");
        return String.format("{s_id=%d, s_sym=%s, c_vals=%s}", state.id, state.token.symbol, counterValueString);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Configuration)) {
            return false;
        }
        Configuration that = (Configuration) obj;
        return this.state.equals(that.state) && this.counterValues.equals(that.counterValues);
    }

    @Override
    public int hashCode() {
        return state.hashCode() + counterValues.hashCode();
    }
}
