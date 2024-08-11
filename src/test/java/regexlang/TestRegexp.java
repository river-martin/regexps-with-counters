package regexlang;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestRegexp {
  @ParameterizedTest
  @ValueSource(strings = {"a{1,2}", "a{1,}", "a{,2}", "a{1}", "^[A-Z]{2}[0-9]{6}[A-DFM]{1}$"})
  public void testContainsCounter() {
    Regexp r = new Regexp("a{1,2}");
    assert r.containsCounter();
  }

}
