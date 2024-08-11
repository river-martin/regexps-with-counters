package cli;

import org.apache.commons.cli.*;

import automata.NCA;
import automata.NFA;
import automata.ProductNFA;
import regexlang.QuantExprRewriteVisitor;
import regexlang.RegexSyntaxErrorException;
import regexlang.SimpleRegexpParser;
import regexlang.UnsupportedRegexException;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

/**
 * Launches the project, preprocesses input regular expressions and performs a
 * modified version of Glushkov's construction algorithm.
 */
public class App {
    private final static String COUNTER_MATCHING_REGEX = ".*?\\{(\\d+|\\d+,\\d+|\\d+,)}.*?";

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

    public static String preprocessRegex(String regex) throws UnsupportedRegexException, RegexSyntaxErrorException {
        SimpleRegexpParser regexParser = QuantExprRewriteVisitor.makeParser(regex);
        ParseTree tree = regexParser.regexp();
        tree = regexlang.QuantExprRewriteVisitor.rewriteUnboundedCounters(tree);
        if (regexParser.getNumberOfSyntaxErrors() > 0) {
            throw new RegexSyntaxErrorException(String.format("Regex `%s` contains a syntax error.", regex));
        }
        regex = tree.getText().replace("<EOF>", "");
        return regex;
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
        CommandLineParser clParser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine cmd = clParser.parse(options, args);
            String mode = cmd.getOptionValue("mode");

            Iterable<String> regexpStrs;
            if (cmd.hasOption('r')) {
                regexpStrs = Arrays.asList(new String[] { cmd.getOptionValue("regexp") });
            } else if (cmd.hasOption('f')) {
                regexpStrs = new IterableLines(cmd.getOptionValue("regexp-file"));
            } else {
                // A ParseException should be thrown before this point.
                throw new ParseException("No regex provided.");
            }
            for (String regexStr : regexpStrs) {
                SimpleRegexpParser regexParser = QuantExprRewriteVisitor.makeParser(regexStr);
                ParseTree tree = regexParser.regexp();
                tree = QuantExprRewriteVisitor.rewriteUnboundedCounters(tree);
                regexStr = tree.getText().replace("<EOF>", "");
                System.out.println(regexStr);
                switch (mode) {
                    case "nca":
                        System.out.println(NCA.glushkov(regexStr));
                        break;
                    case "nfa":
                        System.out.println(new NFA(NCA.glushkov(regexStr)));
                        break;
                    case "ra":
                        performReachabilityAnalysis(regexStr);
                        break;
                    case "aa":
                        performApproximateAnalysis(regexStr);
                        break;
                    case "match":
                        if (!cmd.hasOption("q"))
                            throw new ParseException("Query string is required for match mode.");
                        String queryString = cmd.getOptionValue("q");
                        System.out.println(match(regexStr, queryString));
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