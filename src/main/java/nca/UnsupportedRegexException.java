package nca;

public class UnsupportedRegexException extends IllegalArgumentException {
    public UnsupportedRegexException(String message, Throwable cause) {
        super(message, cause);
    }
}
