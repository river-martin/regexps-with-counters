package regexlang;

public class UnsupportedRegexException extends IllegalArgumentException {
    public UnsupportedRegexException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedRegexException(String message) {
        super(message);
    }
}
