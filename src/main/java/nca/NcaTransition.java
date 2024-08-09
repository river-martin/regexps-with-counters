package nca;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * An abstraction for the transitions between NCA states.
 *
 * Contains the logic that is used to determine whether a given transition can be taken,
 * when given certain counter values.
 */
public class NcaTransition {
    protected final NcaState dest;
    protected final NcaTransitionType type;
    protected List<CounterRange> counterDependencies;
    protected Token transitionToken;

    public NcaTransition(List<CounterRange> counters, NcaTransitionType type, NcaState dest) {
        this.counterDependencies = counters;
        this.type = type;
        this.dest = dest;
    }

    public NcaTransition(List<CounterRange> counters, NcaTransitionType type, NcaState dest, Token transitionToken) {
        this.counterDependencies = counters;
        this.transitionToken = transitionToken;
        this.type = type;
        this.dest = dest;
    }

    @Override
    public String toString() {
        CounterRange[] countersInRange;
        switch (type) {
            case UNCONDITIONAL:
                return dest.id + "";
            case CONDITIONAL_BACKWARD_COUNTER:
                countersInRange = new CounterRange[counterDependencies.size() - 1];
                counterDependencies.subList(0, counterDependencies.size() - 1).toArray(countersInRange);
                return String.format("%d, when counters %s are within their ranges and counter %s is less than its upper bound.",
                        dest.id, Arrays.toString(countersInRange), counterDependencies.get(counterDependencies.size() - 1)
                );
            case CONDITIONAL_BACKWARD_STAR:
                countersInRange = new CounterRange[counterDependencies.size()];
                counterDependencies.toArray(countersInRange);
                return String.format("%d, when counters %s are within their ranges. (backward * transition)",
                        dest.id, Arrays.toString(countersInRange)
                );
            case CONDITIONAL_FORWARD:
                countersInRange = new CounterRange[counterDependencies.size()];
                counterDependencies.toArray(countersInRange);
                return String.format("%d, when counters %s are within their ranges.",
                        dest.id, Arrays.toString(countersInRange)
                );
            default:
                return "";
        }
    }

    public NcaTransition(NcaTransitionType type, NcaState dest) {
        this.type = type;
        this.dest = dest;
    }

    private boolean checkForwardCondition(HashMap<Integer, Integer> counterValues) {
        for (CounterRange counter : counterDependencies) {
            if (counter.isOutOfRange(counterValues.get(counter.id))) {
                return false;
            }
        }
        if (counterDependencies.size() > 0) {
            CounterRange lastCounter = counterDependencies.get(counterDependencies.size() - 1);
            return lastCounter.isBelowUpperBound(counterValues.get(lastCounter.id));
        } else {
            return true;
        }
    }

    private boolean checkBackwardCounterCondition(HashMap<Integer, Integer> counterValues) {
        for (int i = 0; i < counterDependencies.size() - 1; i++) {
            CounterRange counter = counterDependencies.get(i);
            if (counter.isOutOfRange(counterValues.get(counter.id))) {
                return false;
            }
        }
        if (counterDependencies.size() > 0) {
            CounterRange lastCounter = counterDependencies.get(counterDependencies.size() - 1);
            return lastCounter.isBelowUpperBound(counterValues.get(lastCounter.id));
        } else {
            return true;
        }
    }

    private boolean checkBackwardStarCondition(HashMap<Integer, Integer> counterValues) {
        for (CounterRange counter : counterDependencies) {
            if (counter.isOutOfRange(counterValues.get(counter.id))) {
                return false;
            }
        }
        return true;
    }


    /**
     * Checks whether this transition can be taken given the current counter values
     * and the type of the transition.
     */
    public boolean isAllowed(HashMap<Integer, Integer> counterValues) {
        switch (type) {
            case UNCONDITIONAL:
                return true;
            case CONDITIONAL_FORWARD:
                return checkForwardCondition(counterValues);
            case CONDITIONAL_BACKWARD_COUNTER:
                return checkBackwardCounterCondition(counterValues);
            case CONDITIONAL_BACKWARD_STAR:
                return checkBackwardStarCondition(counterValues);
            default:
                System.out.println("This code should not be reached.");
                return false;
        }
    }

    /**
     * Increments or initializes counters.
     */
    public HashMap<Integer, Integer> getUpdatedCounterValues(HashMap<Integer, Integer> counterValues) {
        HashMap<Integer, Integer> updatedValues = (HashMap<Integer, Integer>) counterValues.clone();
        CounterRange counterToIncrement;
        List<CounterRange> countersToInit = dest.token.countersInitializedHere;
        switch (type) {
            case CONDITIONAL_BACKWARD_COUNTER:
                counterToIncrement = counterDependencies.get(counterDependencies.size() - 1);
                assert counterValues.containsKey(counterToIncrement.id);
                for (CounterRange counter : countersToInit) {
                    assert (counterValues.containsKey(counter.id));
                    if (counter.id < counterToIncrement.id) {
                        updatedValues.put(counter.id, 1);
                    } else {
                        break;
                    }
                }
                updatedValues.put(counterToIncrement.id, counterValues.get(counterToIncrement.id) + 1);
                break;
            case CONDITIONAL_BACKWARD_STAR:
                assert transitionToken.type == TokenType.STAR;
                Token star = transitionToken;
                for (CounterRange counter : countersToInit) {
                    if (counter.id < star.id) {
                        updatedValues.put(counter.id, 1);
                    } else {
                        break;
                    }
                }
                break;
            case UNCONDITIONAL:
            case CONDITIONAL_FORWARD:
                for (CounterRange counter : countersToInit) {
                    updatedValues.put(counter.id, 1);
                }
                break;
            default:
                System.out.println(type);
                break;
        }
        return updatedValues;
    }

}
