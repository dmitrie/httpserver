package core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpMessageTest {

  @Test
  public void testGetContentLength() throws Exception {
    assertEquals(0, new Request().calculateContentLength());
  }
}