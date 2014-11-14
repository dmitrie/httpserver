package util;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class LinkedCaseInsensitiveMapTest {

  @Test
  public void testGet() throws Exception {
    LinkedCaseInsensitiveMap map = new LinkedCaseInsensitiveMap();
    map.putAll(new HashMap<String, String>() {{put("tEst", "123");}});
    assertEquals("123", map.get("tesT"));
  }

  @Test
  public void testPut() throws Exception {
    LinkedCaseInsensitiveMap map = new LinkedCaseInsensitiveMap();

    map.put("Test", "123");
    assertEquals("123", map.get("test"));

    map.put("tEST", "456");
    assertEquals("456", map.get("tesT"));
  }
}
