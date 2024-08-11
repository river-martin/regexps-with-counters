package regexlang;

public class RegexSyntaxErrorException extends IllegalArgumentException {
  public RegexSyntaxErrorException(String message, Throwable cause) {
      super(message, cause);
  }

  public RegexSyntaxErrorException(String message) {
      super(message);
  }
}
