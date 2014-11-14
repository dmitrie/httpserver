package util;

import java.util.LinkedHashMap;

public class LinkedCaseInsensitiveMap extends LinkedHashMap<String, String> {

  @Override
  public String get(Object key) {
    for (String originalKey : keySet()) {
      if (originalKey.equalsIgnoreCase((String) key))
        return super.get(originalKey);
    }
    return null;
  }

  @Override
  public String put(String key, String value) {
    for (String originalKey : keySet()) {
      if (originalKey.equalsIgnoreCase(key)) {
        return super.put(originalKey, value);
      }
    }
    return super.put(key, value);
  }
}
