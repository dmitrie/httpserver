package util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static util.Helper.combinePaths;

public class HelperTest {
  @Test
  public void testCombinePaths() throws Exception {
    assertEquals("/test/abc", combinePaths("/test/","/abc/"));
    assertEquals("/test/abc", combinePaths("/test","abc/"));
    assertEquals("test/abc", combinePaths("test","abc"));
  }
}
