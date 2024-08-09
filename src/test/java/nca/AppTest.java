package nca;

import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test cases for some of the features of this project.
 */
public class AppTest {
    final String INPUT_DIR = "src/test/input/";
    final String COUNTER_MATCHING_REGEX = "\\{(\\d+|\\d+,\\d+|\\d+,)}";


    /**
     * Tests the approximate and exact analyses algorithms using some of the regular expressions from regexlib.txt
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
        for (String regex : App.readFile(fileName)) {
            System.out.println(regex);
            int maxUpperBound = getMaxUpperBound(regex);
            NFA nfa = new NFA(App.glushkov(regex));
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
     * Gets acceptable regular expressions from regexlib.txt and writes them to processed_regexlib.txt
     */
    public void processAndFilterRegexLib() throws IOException {
        System.out.println("Processing and filtering regexlib.txt (this takes a while)");
        String fileName = INPUT_DIR + "regexlib.txt";
        PrintWriter processedRegexWriter = new PrintWriter(new FileWriter(INPUT_DIR + "processed_regexlib.txt"));
        for (String regex : App.readFile(fileName)) {
            if (regex.length() > 70) {
                continue;
            }
            regex = App.preprocessRegex(regex);
            if (regex == null) {
                continue;
            }
            try {
                NFA nfa = new NFA(App.glushkov(regex));
                ProductNFA pNfa = new ProductNFA(nfa);
                boolean ambiguous = pNfa.isAmbiguous();
                boolean definitelyUnambiguous = !pNfa.mightBeAmbiguous();
                assert !(definitelyUnambiguous && ambiguous);
                processedRegexWriter.println(regex);
            } catch (Exception e) {
                System.out.println("Processed regex from regexlib.txt now rejected:\n" + regex);
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
        System.out.println(matches.size());
        int currentMax = 0;
        for (String match : matches) {
            System.out.println(match);
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
     * Tests that regular expressions with unbounded counters are translated to bounded counters.
     */
    @Test
    public void testTranslationOfUnboundedCounters() {
        String fileName = INPUT_DIR + "unbounded_counters.txt";
        String[] lines = App.readFile(fileName);
        for (String line : lines) {
            assert line.contains(",}");
            String processed = App.preprocessRegex(line);
            assert processed != null;
            assert !processed.contains(",}");
        }
    }

    /**
     * Tests the NFA's string matcher by comparing it's output to that of Java's Matcher class.
     * (Using randomly generated input strings)
     */
    @Test
    public void testMatcher() {
        System.out.println("Testing matcher. (this might take a while)");
        String fileName = INPUT_DIR + "matcher_test_input.txt";
        String[] testStrings = generateRandomStrings();
        String[] regexs = App.readFile(fileName);
        String breaks = "bcaa";
        testStrings[0] = breaks;
        testMatching(regexs, testStrings);
        regexs = App.readFile(INPUT_DIR + "matcher_test_input_2.txt");
        testStrings = new String[4];
        for (int i = 0; i < 4; i++) {
            testStrings[i] = "";
            for (int j = 0; j < i + 4; j++) {
                testStrings[i] += "aa";
            }
            testStrings[i] += "bbbbbb";
        }
        testMatching(regexs, testStrings);
    }

    private String[] generateRandomStrings() {
        String[] generated = new String[1000];
        generated[0] = "";
        for (int i = 1; i < 1000; i++) {
            int len = (int) (Math.random() * 1000 + 1);
            generated[i] = "";
            for (int j = 0; j < len; j++) {
                int randomIndex = (int) (Math.random() * "abcdef".length());
                generated[i] += "abcdef".charAt(randomIndex);
            }
        }
        return generated;
    }

    /**
     * Tests the NFA's string matcher by comparing it's output to that of Java's Matcher class.
     * (Using randomly generated input strings)
     */
    private void testMatching(String[] regexs, String[] testStrings) {
        for (String regex : regexs) {
            System.out.println("regex = " + regex);
            NFA nfa = new NFA(App.glushkov(App.preprocessRegex(regex)));
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
     * Tests the analysis algorithms using the two examples from the paper referenced in the project specification.
     */
    @Test
    public void testAnalysesWithExamples() {
        String ex32 = INPUT_DIR + "example_3_2.txt";
        String ex34 = INPUT_DIR + "example_3_4.txt";
        String r32 = App.preprocessRegex(App.readFile(ex32)[0]);
        String r34 = App.preprocessRegex(App.readFile(ex34)[0]);
        ProductNFA pnfa32 = new ProductNFA(new NFA(App.glushkov(r32)));
        ProductNFA pnfa34 = new ProductNFA(new NFA(App.glushkov(r34)));
        assert !pnfa32.isAmbiguous() && !pnfa32.mightBeAmbiguous();
        assert !pnfa34.isAmbiguous() && pnfa34.mightBeAmbiguous();
    }

}
