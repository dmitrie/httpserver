package core;

import org.junit.Test;

import static core.HttpRequestRegEx.*;
import static org.junit.Assert.*;

public class HttpRequestRegExTest {

  @Test
  public void testvalidateHeader_RFC2616_4_2() throws Exception {
    assertTrue(validateHeader("Accept:  text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
    assertTrue(validateHeader("Accept-Language:	en-US,en;q=0.5"));
    assertTrue(validateHeader("Accept-Encoding:	gzip, deflate"));
    assertTrue(validateHeader("Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7"));
    assertTrue(validateHeader("Connection:	keep-alive"));
    assertTrue(validateHeader("Cookie: PHPSESSID=r2t5uvjq435r4q7ib3vtdjq120"));
    assertTrue(validateHeader("Cache-Control: no-cache"));
    assertTrue(validateHeader("Host:	www.google.com"));
    assertTrue(validateHeader("Keep-Alive: 300"));
    assertTrue(validateHeader("Pragma: no-cache"));
    assertTrue(validateHeader("Referer:	http://www.google.com/"));
    assertTrue(validateHeader("User-Agent:	Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:32.0) Gecko/20100101 Firefox/32.0"));
    assertTrue(validateHeader("User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.1.5)\r\n  \t Gecko/20091102 Firefox/3.5.5 (.NET CLR 3.5.30729)"));


    assertTrue(validateHeader("field: value"));
    assertTrue(validateHeader("abc:"));
    assertTrue(validateHeader("\u0021: value"));
    assertTrue(validateHeader("\u007E: value"));
    assertTrue(validateHeader("abc:value"));
    assertTrue(validateHeader("abc: value"));
    assertTrue(validateHeader("abc: \tvalue"));
    assertTrue(validateHeader("abc:value \t "));
    assertTrue(validateHeader("abc:val\r\n ue"));
    assertTrue(validateHeader("abc:val\r\n \r\n \r\n u\r\n e"));
    assertTrue(validateHeader("abc:val\r\n\tue"));
    assertTrue(validateHeader("abc:val\r\n     \tue"));
    assertTrue(validateHeader("abc:value\r\n "));
    assertTrue(validateHeader("abc:\r\n\tvalue"));
    assertTrue(validateHeader("abc:\r\n\t  \tvalue"));
    assertTrue(validateHeader("a:value"));
    assertTrue(validateHeader("a:\t  \t  value   \t"));
    assertTrue(validateHeader("abc^*+:value"));
    assertTrue(validateHeader("abc:    \uFFFF\uFF00\r\n abc@#^$%&%$^* \t\t "));
    assertTrue(validateHeader("abc:\r\n "));
    assertTrue(validateHeader("abc: abc=\"\uFFFF\""));
    assertTrue(validateHeader("abc: abc/():;-|/\\\"\""));
    assertTrue(validateHeader("abc: abc=\"\\\u00AA\""));
    assertTrue(validateHeader("abc: abc=\"def\"&foo=\"bar\""));
    assertTrue(validateHeader("abc: abc=\"()\""));


    assertFalse(validateHeader(": value"));
    assertFalse(validateHeader("abc: value\r\n"));
    assertFalse(validateHeader("abc: value\n"));
    assertFalse(validateHeader("abc: value\r"));
    assertFalse(validateHeader("\u0000a: value"));
    assertFalse(validateHeader("\u0005a: value"));
    assertFalse(validateHeader("\u001Fb: value"));
    assertFalse(validateHeader("\u007Fc: value"));
    assertFalse(validateHeader("\rabc: value"));
    assertFalse(validateHeader("\nabc: value"));
    assertFalse(validateHeader("abc\r\n abc: value"));
    assertFalse(validateHeader("(abc: value"));
    assertFalse(validateHeader(")abc: value"));
    assertFalse(validateHeader("<abc: value"));
    assertFalse(validateHeader(">abc: value"));
    assertFalse(validateHeader("@abc: value"));
    assertFalse(validateHeader(",abc: value"));
    assertFalse(validateHeader(";abc: value"));
    assertFalse(validateHeader(":abc: value"));
    assertFalse(validateHeader("\\abc: value"));
    assertFalse(validateHeader("\"abc: value"));
    assertFalse(validateHeader("/abc: value"));
    assertFalse(validateHeader("[abc: value"));
    assertFalse(validateHeader("]abc: value"));
    assertFalse(validateHeader("?abc: value"));
    assertFalse(validateHeader("=abc: value"));
    assertFalse(validateHeader("{abc: value"));
    assertFalse(validateHeader("}abc: value"));
    assertFalse(validateHeader(" abc: value"));
    assertFalse(validateHeader("\tabc: value"));
    assertFalse(validateHeader("abc: value\nabc "));
    assertFalse(validateHeader("abc: value\rabc "));
    assertFalse(validateHeader("abc: value\r\nabc "));
    assertFalse(validateHeader("abc: \u0001"));
    assertFalse(validateHeader("abc: \u0001"));
    assertFalse(validateHeader("abc: \u001F"));
    assertFalse(validateHeader("abc: \u007F"));
  }

  @Test
  public void testValidateMethod_RFC2616_5_1_1() throws Exception {
    assertTrue(validateMethod("OPTIONS"));
    assertTrue(validateMethod("GET"));
    assertTrue(validateMethod("HEAD"));
    assertTrue(validateMethod("POST"));
    assertTrue(validateMethod("PUT"));
    assertTrue(validateMethod("DELETE"));
    assertTrue(validateMethod("TRACE"));
    assertTrue(validateMethod("CONNECT"));
    assertTrue(validateMethod("FOO_BAR"));

    assertFalse(validateMethod("a b"));
    assertFalse(validateMethod("a\r\n b"));
    assertFalse(validateMethod("ABC:"));
    assertFalse(validateMethod(" abc"));
    assertFalse(validateMethod("abc "));
    assertFalse(validateMethod("a/bc"));
  }

  @Test
  public void testValidateProtocol_RFC2616_3_1() throws Exception {
    assertTrue(validateProtocol("HTTP/1.1"));
    assertTrue(validateProtocol("HTTP/10.1"));
    assertTrue(validateProtocol("HTTP/1.10"));
    assertTrue(validateProtocol("HTTP/123.123"));
    assertTrue(validateProtocol("HTTP/0.0"));

    assertFalse(validateProtocol("HTTP/11"));
    assertFalse(validateProtocol("HTTP/1"));
    assertFalse(validateProtocol("HTTP1.1"));
    assertFalse(validateProtocol("HTTP/.9"));
    assertFalse(validateProtocol("HTTP/ 1.1"));
    assertFalse(validateProtocol("HTTP /1.1"));
    assertFalse(validateProtocol("HTTP / 1.1"));
  }

  @Test
  public void testValidateRequestLineFormat_RFC2616_5_1() throws Exception {
    assertTrue(validateRequestLineFormat("GET / HTTP/1.1"));
    assertTrue(validateRequestLineFormat("GET /test.html HTTP/1.1"));

    assertFalse(validateRequestLineFormat(" GET / HTTP/1.1"));
    assertFalse(validateRequestLineFormat("GET  / HTTP/1.1"));
    assertFalse(validateRequestLineFormat("GET /  HTTP/1.1"));
    assertFalse(validateRequestLineFormat("GET / HTTP/1.1 "));
    assertFalse(validateRequestLineFormat("GET / HTTP /1.1"));
    assertFalse(validateRequestLineFormat("GET / HTTP/ 1.1"));
    assertFalse(validateRequestLineFormat("GET\t/ HTTP/1.1"));
    assertFalse(validateRequestLineFormat("GET /\tHTTP/1.1"));
  }

  @Test
  public void testGetParsedBodyCharset_RFC2616_3_4_and_3_7() throws Exception {
    assertEquals("ISO-8859-4", getParsedBodyCharset("text/html; charset=ISO-8859-4").toString());
    assertEquals("ISO-8859-1", getParsedBodyCharset("text/html; charset=ISO-8859-4; charset=ISO-8859-1").toString());
    assertEquals("ISO-8859-4", getParsedBodyCharset("abc;charset=ISO-8859-4").toString());
    assertEquals("ISO-8859-1", getParsedBodyCharset("foo bar").toString());
  }
}


