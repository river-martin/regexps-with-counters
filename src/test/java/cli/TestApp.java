package cli;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import automata.NCA;
import automata.NFA;
import automata.ProductNFA;
import regexlang.QuantifierRewriteVisitor;
import regexlang.SimpleRegexpBaseVisitor;
import regexlang.SimpleRegexpParser.UnboundedCounterContext;

/**
 * Test cases for some of the features of this project.
 */
public class TestApp {
    final String INPUT_DIR = "src/test/input/";
    final String COUNTER_MATCHING_REGEX = "\\{(\\d+|\\d+,\\d+|\\d+,)}";

    /**
     * Tests the approximate and exact analyses algorithms using some of the regular
     * expressions from regexlib.txt
     *
     * All times each of these algorithms and writes the results to separate files.
     */
    @Test
    public void testAnalysesWithProcessedRegexLib() throws IOException {
        processAndFilterRegexLib();
        String approxTimeFileName = "report/approx_times.csv";
        String exactTimeFileName = "report/exact_times.csv";
        String fileName = INPUT_DIR + "processed_regexlib.txt";
        PrintWriter approxTimesWriter = new PrintWriter(new FileWriter(approxTimeFileName));
        PrintWriter exactTimesWriter = new PrintWriter(new FileWriter(exactTimeFileName));
        Iterable<String> lines = new IterableLines(fileName);
        for (String regex : lines) {
            assert (!regex.isEmpty());
            assert App.containsCounter(regex);
            int maxUpperBound = getMaxUpperBound(regex);
            NFA nfa = new NFA(NCA.glushkov(regex));
            ProductNFA pNfa = new ProductNFA(nfa);
            Instant before = Instant.now();
            boolean definitelyNotAmbiguous = !pNfa.mightBeAmbiguous();
            Instant after = Instant.now();
            approxTimesWriter.printf("%s,%s\n", maxUpperBound, Duration.between(before, after).toNanos());
            before = Instant.now();
            boolean ambiguous = pNfa.isAmbiguous();
            after = Instant.now();
            exactTimesWriter.printf("%s,%s\n", maxUpperBound, Duration.between(before, after).toNanos());
            assert !(definitelyNotAmbiguous && ambiguous);
        }
        exactTimesWriter.close();
        approxTimesWriter.close();
    }

    /**
     * Gets acceptable regular expressions from regexlib.txt and writes them to
     * processed_regexlib.txt
     */
    public void processAndFilterRegexLib() throws IOException {
        System.out.println("Processing and filtering regexlib.txt (this takes a while)");
        String fileName = INPUT_DIR + "regexlib.txt";
        PrintWriter processedRegexWriter = new PrintWriter(new FileWriter(INPUT_DIR + "processed_regexlib.txt"));
        for (String regex : new IterableLines(fileName)) {
            if (regex.length() > 70) {
                continue;
            }
            regex = App.preprocessRegex(regex);
            if (!App.containsCounter(regex)) {
                // We only want to test regular expressions with counters.
                continue;
            }
            try {
                ProductNFA pNfa = new ProductNFA(new NFA(NCA.glushkov(regex)));
                boolean ambiguous = pNfa.isAmbiguous();
                boolean definitelyUnambiguous = !pNfa.mightBeAmbiguous();
                assert !(definitelyUnambiguous && ambiguous);
                processedRegexWriter.println(regex);
            } catch (Exception e) {
                // XXX: This may reject valid regular expressions that crash the
                // code for the glushkov construction, the Product NFA construction, or the
                // either of the ambiguity detection algorithms.
                System.out.printf("Warning: regex `%s` rejected\n", regex);
            }
        }
        processedRegexWriter.close();
        System.out.println("Done processing and filtering regexlib.txt");
    }

    /**
     * Gets the largest upper bound of any of the counters in the regex.
     */
    private int getMaxUpperBound(String regex) {
        Pattern pattern = Pattern.compile(COUNTER_MATCHING_REGEX);
        Matcher matcher = pattern.matcher(regex);
        Set<String> matches = new HashSet<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        int currentMax = 0;
        for (String match : matches) {
            int val;
            if (match.contains(",")) {
                val = Integer.parseInt(match.substring(match.indexOf(",") + 1, match.length() - 1));
            } else {
                val = Integer.parseInt(match.substring(1, match.length() - 1));
            }
            if (val > currentMax) {
                currentMax = val;
            }
        }
        return currentMax;
    }

    /**
     * Test that unbounded counters are translated correctly.
     */
    @Test
    public void testTranslationOfUnboundedCounters() {
        String fileName = INPUT_DIR + "unbounded_counters.txt";
        ParseTree tree;
        for (String line : new IterableLines(fileName)) {
            try {
                 tree = QuantifierRewriteVisitor.parse(line);
                 System.out.println("Tree: " + tree.toStringTree());
                 System.out.println(tree.getText());
            } catch (RecognitionException e) {
                System.out.println(e.getMessage());
                System.out.println("Error: " + line);
                assert line.isEmpty();
                continue;
            }
            ParseTree rewrittenTree = QuantifierRewriteVisitor.rewrite(tree);
            assert rewrittenTree != null;
            SimpleRegexpBaseVisitor<ParseTree> validationVisitor = new SimpleRegexpBaseVisitor<ParseTree>() {
                @Override
                public ParseTree visitUnboundedCounter(UnboundedCounterContext ctx) {
                    // If we get here, the rewriter did not do its job
                    assert false;
                    return ctx;
                }
            };
            validationVisitor.visit(rewrittenTree);

            // The rewriter should not change the tree if there are no unbounded counters
            // (and there should not be any after the first rewrite)
            ParseTree validationTree = QuantifierRewriteVisitor.rewrite(rewrittenTree);
            assert validationTree != null;
            assert validationTree.equals(rewrittenTree);
        }
    }

    private String[] generateRandomStrings(int numStrings, int maxlen, String alphabet) {
        String[] generated = new String[numStrings];
        // First string is the empty string
        generated[0] = "";
        for (int i = 1; i < numStrings; i++) {
            int len = (int) (Math.random() * maxlen + 1);
            StringBuilder sb = new StringBuilder(len);
            for (int j = 0; j < len; j++) {
                int randomIndex = (int) (Math.random() * alphabet.length());
                sb.append(alphabet.charAt(randomIndex));
            }
            generated[i] = sb.toString();
        }
        return generated;
    }

    /**
     * Tests the NFA's string matcher by comparing it's output to that of Java's
     * Matcher class.
     * (Using randomly generated input strings)
     */
    @Test
    public void testMatcherWithRandomStrings() {
        String fileName = INPUT_DIR + "matcher_test_input.txt";
        String[] testStrings = generateRandomStrings(1000, 1000, "abcdef");
        Iterable<String> regexs = new IterableLines(fileName);
        String breaks = "bcaa";
        testStrings[1] = breaks;
        testMatcher(regexs, testStrings);
    }

    @Test
    public void testMatcherWithSimpleStrings() {
        String[] testStrings = new String[4];
        for (int i = 0; i < 4; i++) {
            testStrings[i] = "";
            for (int j = 0; j < i + 4; j++) {
                testStrings[i] += "aa";
            }
            testStrings[i] += "bbbbbb";
        }
        Iterable<String> regexs = new IterableLines(INPUT_DIR + "matcher_test_input_2.txt");
        testMatcher(regexs, testStrings);
    }

    /**
     * Tests the NFA's string matcher by comparing it's output to that of Java's
     * Matcher class.
     * (Using randomly generated input strings)
     */
    private void testMatcher(Iterable<String> regexs, String[] testStrings) {
        for (String regex : regexs) {
            NFA nfa = new NFA(NCA.glushkov(App.preprocessRegex(regex)));
            Pattern pattern = Pattern.compile(regex);
            for (String testString : testStrings) {
                Matcher matcher = pattern.matcher(testString);
                boolean nfaMatches = nfa.tryMatch(testString);
                boolean javaMatches = matcher.matches();
                if (nfaMatches != javaMatches) {
                    System.out.println("teststring = " + testString);
                    System.out.println("NFA matches: " + nfa.tryMatch(testString));
                    System.out.println("Java matches: " + matcher.matches());
                }
                assert nfaMatches == javaMatches;
            }
        }
    }

    /**
     * Tests the analysis algorithms using the two examples from Kong et al. 2022.
     */
    @Test
    public void testAnalysesWithExamples() {
        String r1 = App.preprocessRegex(".*a{2}");
        String r2 = App.preprocessRegex(".*(ab{3}|cd{3})");
        ProductNFA pnfa32 = new ProductNFA(new NFA(NCA.glushkov(r1)));
        ProductNFA pnfa34 = new ProductNFA(new NFA(NCA.glushkov(r2)));
        assert !pnfa32.isAmbiguous() && !pnfa32.mightBeAmbiguous();
        assert !pnfa34.isAmbiguous() && pnfa34.mightBeAmbiguous();
    }

}
