# Regular expressions with counters

## Compilation and execution

### Prerequisites

Install Maven and Java.

Install ANTLR and use it to generate the regex parser:

```Bash
python3 -m venv venv
source venv/bin/activate
pip install antlr4-tools
antlr4 src/main/java/regexlang/SimpleRegexp.g4 -visitor
```

### Compilation

```Bash
# Compiles the project to the `target` directory
mvn compile
```

### Running the test suite

```Bash
# All tests should pass
mvn test
```

### Execution

#### Setting the class path

Add `target/classes` and the path to the dependencies to the `CLASSPATH` environment variable by running the command below.

```Bash
source scripts/set_classpath.sh
```

#### Running the application

Run the application with a command that matches the template below.

```Markdown
java cli.App [-f|-r] <arg> -m <arg> [-q <arg>]
```

For example:

```Bash
java cli.App -f src/test/resources/regexs/example_3_2.txt -m ra
```

The options for the `mode` argument are:

- `nca`, to construct and print the NCA.
- `nfa`, to construct and print the NFA.
- `match`, to construct the NFA and use it to try match an input string.
    The input string must be provided as the next argument.
- `ra`, to perform reachability analysis using the accurate reachability algorithm described by [Kong et al. 2022](https://dl.acm.org/doi/10.1145/3519939.3523456#).
- `aa`, to perform reachability analysis using the approximate reachability algorithm described by [Kong et al. 2022](https://dl.acm.org/doi/10.1145/3519939.3523456#).

### Examples

Example regexs are provided in `src/test/resources/regexs/`. Commands to run each mode for Example 3.2 (`.*a{2}`) are shown below.

```Bash
export fpath="src/test/resources/regexs/example_3_2.txt"

# Construct and print the NCA
java cli.App -f ${fpath} -m  nca

# Construct and print the NFA
java cli.App -f ${fpath} -m  nfa

# Construct the NFA and try match "a"
java cli.App -f ${fpath} -m  match -q a
# Construct the NFA and try match "aa"
java cli.App -f ${fpath} -m  match -q aa
# Construct the NFA and try match "aaa"
java cli.App -f ${fpath} -m  match -q aaa

# Perform accurate reachability analysis
java cli.App -f ${fpath} -m  ra

# Perform approximate reachability analysis
java cli.App -f ${fpath} -m  aa
```

Similar commands can be run for other examples, such as Example 3.4 (`.*(ab{3}|cd{3})`). The commands below show that the input string "abb" does not match the regex, whereas "abbb" does.

```Bash
export fpath="src/test/resources/regexs/example_3_4.txt"

# The regex should not match "abb"
java cli.App -f ${fpath} -m  match -q abb
# The regex should match "abbb"
java cli.App -f ${fpath} -m  match -q abbb
```

## Supported regular expression constructs

- **Parentheses**: Used for grouping
- **Counters**: Specify the number of occurrences (e.g. `{n}`, `{n,}`, `{n, m}`).
- **Kleene star (`*`) and Plus (`+`)**: `*` matches zero or more occurrences, `+` matches one or more occurrences.
- **Character classes**: Define a set of characters to match (e.g. `[a-z]`, `[0-9]`).
- **Predefined character classes**: Common character classes (e.g. `\d` for digits, and `\w` for word characters).
- **Logical OR (`|`)**: Match one pattern or another (e.g. `a*|b*`).s
