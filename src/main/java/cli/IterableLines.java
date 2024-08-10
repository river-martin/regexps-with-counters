package cli;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class IterableLines implements Iterable<String> {
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
