package util;

public class StringUtils {
  public static String defaultString(String value) {
    return defaultString(value, "");
  }

  public static String defaultString(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  public static String addPostfix(String value, String postfix) {
    return isEmpty(value) ? "" : value + postfix;
  }

  public static boolean isEmpty(String value) {
    return value == null || "".equals(value);
  }

  public static boolean isNotEmpty(String value) {
    return !isEmpty(value);
  }
}
