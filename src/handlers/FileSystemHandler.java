package handlers;

import core.Handler;
import core.Request;
import core.Response;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static core.HttpStatusCode.NOT_FOUND;
import static core.HttpStatusCode.OK;
import static util.Helper.combinePaths;
import static util.Helper.getServerTime;

public class FileSystemHandler extends Handler {

  private String documentRoot;

  public FileSystemHandler(String documentRoot) {
    this.documentRoot = documentRoot;
  }

  @Override
  public void handle(Request request, Response response) {
    if (response.responseStatusCode != null)
      return;

    switch (request.requestMethod) {
      case "GET":
      case "HEAD":
        try {
          response.setBody(browsePath(request.requestURI, response.bodyCharset));
          response.responseStatusCode = OK;
        } catch (IOException e) {
          response.setBody("<div style=\"text-align: center;\"><h1 style=\"color: red;\">404 Error</h1><br>File not found</div>");
          response.responseStatusCode = NOT_FOUND;
        }

        response.setHeader("Content-Type", "text/html; charset=" + response.bodyCharset);
        response.setHeader("Last-modified", getServerTime());

        if ("HEAD".equals(request.requestMethod)) {
          response.setHeader("Content-Length", "" + response.calculateContentLength());
          response.setBody(null);
        }
        break;
      default:
        break;
    }
  }

  public String browsePath(URI requestURI, Charset charset) throws IOException {
    File localFile = covertRequestURIToLocalFile(requestURI);
    if (localFile.isDirectory())
      return generateDirectoryListingHTML(requestURI);
    else
      return new String(Files.readAllBytes(localFile.toPath()), charset);
  }

  public String generateDirectoryListingHTML(URI requestURI) {
    String body = "<h1>Index of " + requestURI.getPath() + "</h1>";
    body += "<ul>";
    body += generateDirectoryListing(requestURI)
              .stream()
              .map(line -> "<li>" + line + "</li>")
              .collect(Collectors.joining("\r\n"));
    body += "</ul>";
    return body;
  }

  public File covertRequestURIToLocalFile(URI requestURI) {
    return new File(combinePaths(getDocumentRoot(), requestURI.getPath()));
  }

  public String getDocumentRoot() {
    return documentRoot;
  }

  public List<String> generateDirectoryListing(URI requestURI) {
    File localFile = covertRequestURIToLocalFile(requestURI);

    List<String> links = Arrays.stream(localFile.listFiles())
      .filter(file -> !file.getName().endsWith("~"))
      .map(this::generateLinkToPath)
      .collect(Collectors.toList());

    if (!requestURI.getPath().equals("/"))
      links.add(0, generateLinkToParent(localFile));

    return links;
  }

  public String generateLink(String name, String relativePath) {
   return "<a href=\"" + encodeURL(relativePath) + "\">" + name + "</a>";
  }

  public String generateLinkToPath(File file) {
    String name = file.getName();
    String absPath = makeAbsPath(file);
    return generateLink(name, absPath);
  }

  public String makeAbsPath(File filePath) {
    return "/" + new File(getDocumentRoot()).toURI().relativize(filePath.toURI()).getPath();
  }

  public String generateLinkToParent(File file) {
    String name = "..";
    String absPath = makeAbsPath(file.getParentFile());
    return generateLink(name, absPath);
  }

  public String encodeURL(String relativePath) {
    try {
      return new URI(null, null, relativePath, null).toASCIIString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
