package cli;

import org.apache.commons.cli.*;

import automata.NCA;
import automata.NFA;
import automata.ProductNFA;
import automata.UnsupportedRegexException;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Launches the project, preprocesses input regular expressions and performs a
 * modified version of Glushkov's construction algorithm.
 */
public class App {
    private final static String COUNTER_MATCHING_REGEX = ".*?\\{(\\d+|\\d+,\\d+|\\d+,)}.*?";

    public static String preprocessRegex(String regex) {
        // TODO: document and test

        regex = regex.replace("||", "|()|");
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
    }

    /**
     * Replaces the + operator with the equivalent {1,} operator.
     * 
     * XXX: This method does not handle escaped + operators appropriately. It does
     * not handle plusses in character classes appropriately either.
     * 
     * @param regex The regular expression to replace the + operators in.
     * @return The regular expression with the + operators replaced with {1,}.
     */
    protected static String translatePlus(String regex) {
        return regex.replaceAll("\\+", "{1,}");
    }

    protected static String translateUnboundedCounters(String regex) {
        // Rewrite unbounded counters to bounded counters followed by a kleene star.
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
        ProductNFA pNfa = new ProductNFA(new NFA(NCA.glushkov(regexp)));
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
        ProductNFA pNfa = new ProductNFA(new NFA(NCA.glushkov(regexp)));
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
        return new NFA(NCA.glushkov(regex)).tryMatch(queryString);
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
                        System.out.println(NCA.glushkov(regex));
                        break;
                    case "nfa":
                        System.out.println(new NFA(NCA.glushkov(regex)));
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
            formatter.printHelp("java cli.App [-f|-r] <arg> -m <arg> [-q <arg>]", options);
        } catch (UnsupportedRegexException e) {
            System.out.println(e.getMessage());
        }
    }
}