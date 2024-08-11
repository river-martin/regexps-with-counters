package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A container for configuration properties (read from resources/config.properties).
 */
public class Config {
  private static final Properties properties = new Properties();

  static {
    try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
      properties.load(input);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public static String getProperty(String key) {
    return properties.getProperty(key);
  }
}