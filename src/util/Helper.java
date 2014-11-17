package util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Helper {
  public static String readFile(String path, Charset charset) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, charset);
  }

  public static String combinePaths(String path1, String path2) {
    File file1 = new File(path1);
    File file2 = new File(file1, path2);
    return file2.getPath();
  }

  public static String getServerTime() {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    return dateFormat.format(calendar.getTime());
  }

  public static void sleep(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException ignored) {}
  }
}
