package automata;

import java.util.*;

/**
 * A container for the sets computed for Glushkov's construction algorithm.
 *
 * 'l' is used as an abbreviation of 'lambda'.
 */
public class Sets {
    protected final Set<TokenString> p, l, d, f;

    public Sets() {
        p = new HashSet<>();
        l = new HashSet<>();
        d = new HashSet<>();
        f = new HashSet<>();
    }
    @Override
    public String toString() {
        TokenString[] pArr = new TokenString[p.size()];
        p.toArray(pArr);
        Arrays.sort(pArr);
        String ps = Arrays.toString(pArr);

        TokenString[] lArr = new TokenString[l.size()];
        l.toArray(lArr);
        Arrays.sort(lArr);
        String ls = Arrays.toString(lArr);

        TokenString[] dArr = new TokenString[d.size()];
        d.toArray(dArr);
        Arrays.sort(dArr);
        String ds = Arrays.toString(dArr);

        TokenString[] fArr = new TokenString[f.size()];
        f.toArray(fArr);
        Arrays.sort(fArr);
        String fs = Arrays.toString(fArr);

        return "P: " + ps + "\nL: " + ls + "\nD: " + ds + "\nF: " + fs;
    }
}