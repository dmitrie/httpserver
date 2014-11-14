package util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.Charset.forName;
import static util.HttpRequestRegEx.BasicRules.*;

public class HttpRequestRegEx {

  public static final String CRLF = "\r\n";

  static enum BasicRules {
    CRLF("\\u000D\\u000A"),
    CHAR("[\\u0000-\\u007F]"),
    QUOTED_PAIR("\\\\" + CHAR),
    LWS("(" + CRLF + ")?[ \\u0009]+"),
    TEXT("([^\\u0000-\\u001F\\u007F]|" + LWS + ")"),
    QDTEXT("([^\\u0000-\\u001F\\u007F\\u0022]|" + LWS + ")"),
    SEPARATOR("[()<>@,;:\\\\\"/\\[\\]?={} \\u0009]"),
    TOKEN("[^()<>@,;:\\\\\"/\\[\\]?={} \\u0009\\u0000-\\u001F\\u007F-\\uFFFF]+"),
    QUOTED_STRING("\"(" + QDTEXT + "|" + QUOTED_PAIR + ")*\"");

    private final String pattern;

    BasicRules(String pattern) {
      this.pattern = pattern;
    }

    public String getPattern() {
      return pattern;
    }

    @Override
    public String toString() {
      return pattern;
    }
  }

  private static final Pattern VALID_HEADER = Pattern.compile(
    TOKEN+":("+TEXT+"*|("+TOKEN+"|"+SEPARATOR+"|"+QUOTED_STRING+")*)"
  );

  private static final Pattern VALID_METHOD = Pattern.compile(
    "(OPTIONS|GET|HEAD|POST|PUT|DELETE|TRACE|CONNECT|"+TOKEN+")"
  );

  private static final Pattern VALID_PROTOCOL = Pattern.compile(
    "HTTP/\\d+\\.\\d+"
  );

  private static final Pattern VALID_REQUEST_LINE_FORMAT = Pattern.compile(
    "[^ ]+ [^ ]+ [^ ]+"
  );

  private static final Pattern CHARSET_FROM_CONTENT_TYPE = Pattern.compile(
    ".*;(" + LWS + ")*charset=(" + TOKEN + ").*"
  );

  public static boolean validateHeader(String headers) {
    return VALID_HEADER.matcher(headers).matches();
  }

  public static boolean validateMethod(String method) {
    return VALID_METHOD.matcher(method).matches();
  }

  public static boolean validateProtocol(String protocol) {
    return VALID_PROTOCOL.matcher(protocol).matches();
  }

  public static boolean validateRequestLineFormat(String requestLine) {
    return VALID_REQUEST_LINE_FORMAT.matcher(requestLine).matches();
  }

  public static String replaceMultipleLWSWithSingleSpace(String headers) {
    return headers.replaceAll("(" + LWS + ")+", " ");
  }

  public static Charset getParsedBodyCharset(String contentType) {
    try {
      Matcher matcher = CHARSET_FROM_CONTENT_TYPE.matcher(contentType);
      matcher.matches();
      return forName(matcher.group(3));
    } catch (Exception e) {
      return StandardCharsets.ISO_8859_1;
    }
  }
}

