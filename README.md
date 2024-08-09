# Regular expressions with counters

## Compilation and execution

### Prerequisites

Install Maven and Java.

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

```Bash
java nca.App <input_file> <mode> [<string_to_match>]
```

For example:

```Bash
java nca.App src/test/input/example_3_2.txt ra
```

**Note:** text files containing regular expressions are provided in `src/test/input`.

The options for the `mode` argument are:

- `nca`, to construct and print the NCA.
- `nfa`, to construct and print the NFA.
- `match`, to construct the NFA and use it to try match an input string.
    The input string must be provided as the next argument.
- `ra`, to perform reachability analysis using the accurate reachability algorithm described by [Kong et al. 2022](https://dl.acm.org/doi/10.1145/3519939.3523456#).
- `aa`, to perform reachability analysis using the approximate reachability algorithm described by [Kong et al. 2022](https://dl.acm.org/doi/10.1145/3519939.3523456#).

### Examples

Example regexs are provided in `src/test/input`. Commands to run each mode for Example 3.2 (`.*a{2}`) are shown below.

```Bash
export fpath="src/test/input/example_3_2.txt"

# Construct and print the NCA
java nca.App ${fpath} nca

# Construct and print the NFA
java nca.App ${fpath} nfa

# Construct the NFA and try match "a"
java nca.App ${fpath} match a
# Construct the NFA and try match "aa"
java nca.App ${fpath} match aa
# Construct the NFA and try match "aaa"
java nca.App ${fpath} match aaa

# Perform accurate reachability analysis
java nca.App ${fpath} ra

# Perform approximate reachability analysis
java nca.App ${fpath} aa
```

Similar commands can be run for other examples, such as Example 3.4 (`.*(ab{3}|cd{3})`). The commands below show that the input string "abb" does not match the regex, whereas "abbb" does.

```Bash
# The regex should not match "abb"
java nca.App ${fpath} match abb
# The regex should match "abbb"
java nca.App ${fpath} match abbb
```

## Supported regular expression constructs

- **Parentheses**: Used for grouping
- **Counters**: Specify the number of occurrences (e.g. `{n}`, `{n,}`, `{n, m}`).
- **Kleene star (`*`) and Plus (`+`)**: `*` matches zero or more occurrences, `+` matches one or more occurrences.
- **Character classes**: Define a set of characters to match (e.g. `[a-z]`, `[0-9]`).
- **Predefined character classes**: Common character classes (e.g. `\d` for digits, and `\w` for word characters).
- **Logical OR (`|`)**: Match one pattern or another (e.g. `a*|b*`).s
