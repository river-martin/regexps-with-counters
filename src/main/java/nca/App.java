package nca;

import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Launches the project, preprocesses input regular expressions and performs a
 * modified version of Glushkov's construction algorithm.
 */
public class App {
    private final static String COUNTER_MATCHING_REGEX = ".*?\\{(\\d+|\\d+,\\d+|\\d+,)}.*?";

    protected static String preprocessRegex(String regex) {
        // TODO: document and test
        try {
            regex = regex.replace("||", "");
            regex = regex.replace("\\?", "");
            regex = regex.replace("?:", "").replace("?>", "").replace("?", "");
            regex = regex.replace("[]", "");
            Pattern pattern = Pattern.compile("\\^.*?\\$|/.*?/");
            Matcher matcher = pattern.matcher(regex);
            Set<String> matches = new HashSet<>();
            while (matcher.find()) {
                matches.add(matcher.group());
            }
            for (String match : matches) {
                String replacement = match.substring(1, match.length() - 1);
                regex = regex.replace(match, replacement);
            }
            String processed = translatePlus(regex);
            int i = 1;
            while (i < processed.length()) {
                char c2 = processed.charAt(i);
                char c1 = processed.charAt(i - 1);
                if (c1 == ',' && c2 == '}') {
                    String substring = processed.substring(0, i + 1);
                    String processedSubstring = "(" + translateUnboundedCounters(substring) + ")";
                    processed = processedSubstring + processed.substring(substring.length());
                    i = processedSubstring.length() + 1;
                } else {
                    i++;
                }
            }
            return processed.replace("{1}", "");
        } catch (Exception e) {
            throw new UnsupportedRegexException(String.format("The regex `%s` is invalid/unsupported.", regex), e);
        }
    }

    protected static String translatePlus(String regex) {
        // TODO: document and test
        return regex.replaceAll("(?<=[^\\\\])\\+", "{1,}");
    }

    protected static String translateUnboundedCounters(String regex) {
        String[] segments = regex.split("\\{\\d*,}");
        String[] bounds = regex.split(",}");
        if (segments.length == 1 && segments[0].equals(regex)) {
            return regex;
        }
        for (int i = 0; i < bounds.length; i++) {
            bounds[i] = bounds[i].substring(bounds[i].lastIndexOf("{") + 1);
        }
        StringBuilder translated = new StringBuilder();
        int i = 0;
        for (String segment : segments) {
            char c = segment.charAt(segment.length() - 1);
            String countableExpression;
            switch (c) {
                case ']':
                    countableExpression = getCharacterClass(segment);
                    break;
                case ')':
                    countableExpression = getGroup(segment);
                    break;
                default:
                    countableExpression = getCharOrPredefinedCharClass(segment);
                    break;
            }
            translated.append(segment.substring(0, segment.lastIndexOf(countableExpression)));
            translated.append(countableExpression).append("{").append(bounds[i++]).append("}");
            translated.append(countableExpression).append("*");
        }
        return translated.toString();
    }

    private static String getCharOrPredefinedCharClass(String regex) {
        if (regex.length() >= 2 && regex.charAt(regex.length() - 2) == '\\') {
            return regex.substring(regex.length() - 2);
        } else {
            return regex.substring(regex.length() - 1);
        }
    }

    private static String getGroup(String regex) {
        int rparCount = 1;
        int i;
        for (i = regex.length() - 2; i >= 0 && rparCount > 0; i--) {
            if (regex.charAt(i) == ')') {
                rparCount++;
            } else if (regex.charAt(i) == '(') {
                rparCount--;
            }
        }
        i++;
        return regex.substring(i);
    }

    private static String getCharacterClass(String regex) {
        int rbrackCount = 1;
        int i;
        for (i = regex.length() - 2; i >= 0 && rbrackCount > 0; i--) {
            if (regex.charAt(i) == ']') {
                rbrackCount++;
            } else if (regex.charAt(i) == '[') {
                rbrackCount--;
            }
        }
        i++;
        return regex.substring(i);
    }

    private static Set<TokenString> concat(Set<TokenString> prefixes, Set<TokenString> suffixes) {
        Set<TokenString> newSet = new HashSet<>();
        for (TokenString p : prefixes) {
            for (TokenString s : suffixes) {
                newSet.add(p.concatenate(s));
            }
        }
        return newSet;
    }

    private static void computeSetsForCounter(Stack<Sets> stackedSets, Sets currentSets,
            Token counterToken, List<Token> stateTokens) {
        Sets oldSets = stackedSets.pop();
        int maxID = -3;
        for (TokenString ts : oldSets.d) {
            if (ts.tokens.size() < 1) {
                break;
            }
            assert ts.tokens.size() == 1;
            Token t = ts.tokens.get(0);
            t.countersIncrementedHere.add(counterToken.counterRange);
            if (maxID < t.id) {
                // Find the max token ID so that we can compute the set of states associated
                // with this counter.
                maxID = t.id;
            }
        }
        TokenString emptyString = new TokenString();
        if (counterToken.counterRange.lowerBound <= 0) {
            // Apply the same rules as * for L
            currentSets.l.add(emptyString);
        } else {
            // Keep the old L.
            currentSets.l.addAll(oldSets.l);
            // XXX: might break reachability analysis?
            if (currentSets.l.contains(emptyString)) {
                // The empty string can pad for counts lower than the original lower bound.
                counterToken.counterRange.lowerBound = 0;
            }
        }
        // Always apply the same rules as * for P and D.
        currentSets.p.addAll(oldSets.p);
        currentSets.d.addAll(oldSets.d);
        if (counterToken.counterRange.upperBound >= 2) {
            // Apply the same rules as * for F, with a counter between d and p.
            currentSets.f.addAll(oldSets.f);
            currentSets.f.addAll(concatAndSaveTransitionToken(oldSets.d, oldSets.p, counterToken));
        } else {
            // Keep the old F.
            currentSets.f.addAll(oldSets.f);
        }
        int minID = Integer.MAX_VALUE;
        for (TokenString ts : currentSets.p) {
            assert ts.tokens.size() == 1;
            Token t = ts.tokens.get(0);
            t.countersInitializedHere.add(counterToken.counterRange);
            if (minID > t.id) {
                minID = t.id;
            }
        }
        for (Token t : stateTokens) {
            if (minID <= t.id && t.id <= maxID) {
                t.associatedCounterRanges.add(counterToken.counterRange);
            }
        }
        stackedSets.push(currentSets);
    }

    private static void computeSetsForStar(Stack<Sets> stackedSets, Sets currentSets, Token starToken) {
        Sets oldSets = stackedSets.pop();
        for (TokenString ts : oldSets.d) {
            if (ts.tokens.size() < 1) {
                break;
            }
            assert ts.tokens.size() == 1;
            Token t = ts.tokens.get(0);
            t.starsEndingHere.add(starToken);
        }
        TokenString emptyString = new TokenString();
        currentSets.l.add(emptyString);
        currentSets.p.addAll(oldSets.p);
        currentSets.d.addAll(oldSets.d);
        currentSets.f.addAll(oldSets.f);
        currentSets.f.addAll(concatAndSaveTransitionToken(oldSets.d, oldSets.p, starToken));
        stackedSets.push(currentSets);
    }

    private static void computeSetsForChar(Stack<Sets> stackedSets, Sets currentSets, Token token) {
        // l = emptySet
        TokenString letter = new TokenString(token);
        currentSets.p.add(letter);
        currentSets.d.add(letter);
        stackedSets.push(currentSets);
        // f = emptySet
    }

    private static boolean currentTokenIsLastInGroup(Token nextToken) {
        if (nextToken == null) {
            return true;
        }
        switch (nextToken.type) {
            case COUNTER:
            case STAR:
                return false;
            default:
                return true;
        }
    }

    private static Set<TokenString> concatAndSaveTransitionToken(Set<TokenString> prefixes, Set<TokenString> suffixes,
            Token transitionToken) {
        Set<TokenString> newSet = new HashSet<>();
        for (TokenString p : prefixes) {
            for (TokenString s : suffixes) {
                newSet.add(p.concatenate(s, transitionToken));
            }
        }
        return newSet;
    }

    static SetsAndTokens computeSetsAndGetStateTokens(String regex) {
        char[] s = new char[regex.length()];
        regex.getChars(0, regex.length(), s, 0);
        MyScanner.initScanner(s);
        Token token = MyScanner.getToken();
        List<Token> stateTokens = new ArrayList<>();
        Stack<Sets> stackedSets = new Stack<>();
        Stack<Token> ops = new Stack<>();
        Token concat = new Token("", TokenType.CONCAT);
        while (token != null) {
            Sets currentSets = new Sets();
            Token nextToken = MyScanner.getToken();
            switch (token.type) {
                case L_PAR:
                    ops.push(token);
                    break;
                case R_PAR:
                    while (ops.peek().type != TokenType.L_PAR) {
                        applyOperation(stackedSets, ops);
                    }
                    ops.pop();
                    break;

                case COUNTER:
                    computeSetsForCounter(stackedSets, currentSets, token, stateTokens);
                    break;
                case STAR:
                    computeSetsForStar(stackedSets, currentSets, token);
                    break;

                case CHAR:
                case CHAR_CLASS:
                    computeSetsForChar(stackedSets, currentSets, token);
                    break;

                case BAR:
                    assert ops.isEmpty() || ops.peek().type == TokenType.L_PAR;
                    while (ops.size() > 0 && ops.peek().type != TokenType.L_PAR) {
                        applyOperation(stackedSets, ops);
                    }
                    ops.push(token);
                    break;

                default:
                    System.out.println("This case shouldn't be reached. (app.java):");
                    System.out.println(regex);
                    System.out.println(token.type);
                    break;
            }
            switch (token.type) {
                case BAR:
                case L_PAR:
                    break;
                default:
                    if (currentTokenIsLastInGroup(nextToken)) {
                        while (!ops.isEmpty() && ops.peek().type == TokenType.CONCAT) {
                            applyOperation(stackedSets, ops);
                        }
                        if (canConcatWith(nextToken)) {
                            ops.push(concat);
                        } else {
                            while (!ops.isEmpty() && ops.peek().type == TokenType.BAR) {
                                applyOperation(stackedSets, ops);
                            }
                        }
                    }
                    break;
            }
            if (token.type == TokenType.CHAR || token.type == TokenType.CHAR_CLASS) {
                stateTokens.add(token);
            }
            token = nextToken;
        }
        assert (stackedSets.size() == 1);
        return new SetsAndTokens(stackedSets.pop(), stateTokens);
    }

    private static boolean canConcatWith(Token nextToken) {
        if (nextToken == null) {
            return false;
        }
        switch (nextToken.type) {
            case CHAR:
            case CHAR_CLASS:
            case L_PAR:
                return true;
            default:
                return false;
        }
    }

    private static void applyOperation(Stack<Sets> stackedSets, Stack<Token> ops) {
        Token op = ops.pop();
        Sets f = stackedSets.pop();
        Sets e = stackedSets.pop();
        Sets newSets = new Sets();
        switch (op.type) {
            case BAR:
                newSets.l.addAll(e.l);
                newSets.l.addAll(f.l);
                newSets.p.addAll(e.p);
                newSets.p.addAll(f.p);
                newSets.d.addAll(e.d);
                newSets.d.addAll(f.d);
                newSets.f.addAll(e.f);
                newSets.f.addAll(f.f);
                break;
            case CONCAT:
                newSets.l.addAll(concat(e.l, f.l));
                newSets.p.addAll(e.p);
                newSets.p.addAll(concat(e.l, f.p));
                newSets.d.addAll(f.d);
                newSets.d.addAll(concat(e.d, f.l));
                newSets.f.addAll(e.f);
                newSets.f.addAll(f.f);
                newSets.f.addAll(concatAndSaveTransitionToken(e.d, f.p, op));
                break;
            default:
                System.out.println("This case should not be reached.");
                break;
        }
        stackedSets.push(newSets);
    }

    static NCA glushkov(String regex) {
        SetsAndTokens setsAndTokens = computeSetsAndGetStateTokens(regex);
        return new NCA(setsAndTokens, regex);
    }

    public static String[] readFile(String fileName) {
        List<String> lines = new ArrayList<>();
        try {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
            scanner.close();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: <input_file_name>");
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe + "");
        }
        String[] lineArray = new String[lines.size()];
        lines.toArray(lineArray);
        return lineArray;
    }

    protected static boolean containsCounter(String regex) {
        return regex.matches(COUNTER_MATCHING_REGEX);
    }

    private static void validateRegexForAnalysis(String regex) {
        if (!containsCounter(regex)) {
            throw new IllegalArgumentException("Regex must contain at least one counter.");
        }
    }

    private static void performReachabilityAnalysis(String regexp) {
        validateRegexForAnalysis(regexp);
        System.out.printf("Preprocessed regex: `%s`\n", regexp);
        ProductNFA pNfa = new ProductNFA(new NFA(glushkov(regexp)));
        boolean ambiguous = pNfa.isAmbiguous();
        System.out.print("Exact analysis tells us that the regex ");
        if (ambiguous) {
            System.out.println("is ambiguous.");
            System.out.println("States with counter-ambiguity:");
            System.out.println(pNfa.findAmbiguities());
        } else {
            System.out.println("is unambiguous.");
        }
    }

    private static void performApproximateAnalysis(String regexp) {
        validateRegexForAnalysis(regexp);
        ProductNFA pNfa = new ProductNFA(new NFA(glushkov(regexp)));
        boolean definitelyNotAmbiguous = !pNfa.mightBeAmbiguous();
        System.out.print("Approximate analysis tells us that the regex ");
        if (definitelyNotAmbiguous) {
            System.out.println("is unambiguous.");
        } else {
            System.out.println("may be ambiguous.");
        }
    }

    public static boolean match(String regex, String queryString) {
        regex = preprocessRegex(regex);
        return new NFA(glushkov(regex)).tryMatch(queryString);
    }

    private static Options makeCommandlineOptions() {
        Options options = new Options();

        // Either a single regex must be provided as a command line argument, or a path
        // to a file containing
        // one or more regexs (one per line) must be provided.
        Option fileOption = new Option("f", "regexp-file", true,
                "The path to the file containing the regular expression.");
        Option regexOption = new Option("r", "regexp", true, "The regular expression to process.");
        OptionGroup group = new OptionGroup();
        group.addOption(fileOption);
        group.addOption(regexOption);
        group.setRequired(true);
        options.addOptionGroup(group);

        Option modeOption = new Option("m", "mode", true,
                "The mode to run the program in. Modes available: nca, nfa, ra, aa, match");
        modeOption.setRequired(false);
        options.addOption(modeOption);

        Option queryStringOption = new Option("q", "query-string", true,
                "The string to match against the regular expression.");
        queryStringOption.setRequired(false);
        options.addOption(queryStringOption);
        return options;
    }

    protected static class IterableLines implements Iterable<String> {
        private final Scanner scanner;

        public IterableLines(String filename) {
            try {
                this.scanner = new Scanner(new File(filename));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("File not found.");
            }
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    if (scanner.hasNextLine()) {
                        return true;
                    } else {
                        scanner.close();
                        return false;
                    }
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return scanner.nextLine();
                }
            };
        }
    }

    public static void main(String[] args) {
        Options options = makeCommandlineOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse(options, args);
            String mode = cmd.getOptionValue("mode");

            Iterable<String> regexps;
            if (cmd.hasOption('r')) {
                regexps = Arrays.asList(new String[] { cmd.getOptionValue("regexp") });
            } else if (cmd.hasOption('f')) {
                regexps = new IterableLines(cmd.getOptionValue("regexp-file"));
            } else {
                // A ParseException should be thrown before this point.
                throw new ParseException("No regex provided.");
            }
            for (String regex : regexps) {
                regex = preprocessRegex(regex);
                switch (mode) {
                    case "nca":
                        System.out.println(glushkov(regex));
                        break;
                    case "nfa":
                        System.out.println(new NFA(glushkov(regex)));
                        break;
                    case "ra":
                        performReachabilityAnalysis(regex);
                        break;
                    case "aa":
                        performApproximateAnalysis(regex);
                        break;
                    case "match":
                        if (!cmd.hasOption("q"))
                            throw new ParseException("Query string is required for match mode.");
                        String queryString = cmd.getOptionValue("q");
                        match(regex, queryString);
                        break;
                    default:
                        throw new ParseException("Invalid mode.");
                }
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java nca.App [-f|-r] <arg> -m <arg> [-q <arg>]", options);
        } catch (UnsupportedRegexException e) {
            System.out.println(e.getMessage());
        }
    }
}