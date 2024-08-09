package nca;

/**
 * A abstraction for counter ranges.
 * Used by in the NcaTransition class to help determine if a transition can be taken.
 */
public class CounterRange {
    int lowerBound;
    final int upperBound;
    final int id;

    public CounterRange(int lowerBound, int upperBound, int id) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.id = id;
    }

    public boolean isOutOfRange(int count) {
        return lowerBound > count || count > upperBound;
    }

    public boolean isBelowUpperBound(int count) {
        return count < upperBound;
    }

    @Override
    public String toString() {
        String range;
        if (lowerBound == upperBound) {
            range = String.format("{%d}", lowerBound);
        } else {
            range = String.format("{%d, %d}", lowerBound, upperBound);
        }
        return String.format("(c_id=%d, range=%s)", id, range);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CounterRange)) {
            return false;
        }
        CounterRange that = (CounterRange) obj;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}