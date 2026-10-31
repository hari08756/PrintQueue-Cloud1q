package com.printqueue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Private storage: local on Windows; Supabase when SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are set. */
@Service public class DocumentStore {
  private final Path local;
  private final String url, key, bucket;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  DocumentStore(@Value("${app.upload-dir:private-uploads}") String uploadDir,
                @Value("${SUPABASE_URL:}") String url,
                @Value("${SUPABASE_SERVICE_ROLE_KEY:}") String key,
                @Value("${SUPABASE_BUCKET:print-documents}") String bucket) {
    this.local=Path.of(uploadDir).toAbsolutePath().normalize();
    this.url=url.replaceAll("/+$", "");this.key=key;this.bucket=bucket;
    if ((url.isBlank()) != (key.isBlank())) throw new IllegalStateException("Set BOTH SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY");
  }
  private boolean cloud(){return !url.isBlank();}
  private Path safe(String id){if(!id.matches("[a-fA-F0-9-]{36}"))throw new IllegalArgumentException("Invalid storage ID");return local.resolve(id).normalize();}
  private String resource(String id){safe(id);return url+"/storage/v1/object/"+bucket+"/"+id;}
  private HttpRequest.Builder request(String id){return HttpRequest.newBuilder(URI.create(resource(id)))
    .timeout(Duration.ofSeconds(60)).header("apikey",key).header("Authorization","Bearer "+key);}
  private HttpRequest.Builder authenticatedRequest(String id){return HttpRequest.newBuilder(URI.create(resource(id).replace("/object/", "/object/authenticated/")))
    .timeout(Duration.ofSeconds(60)).header("apikey",key).header("Authorization","Bearer "+key);}
  private HttpResponse<byte[]> send(HttpRequest request) throws IOException {
    try {return http.send(request,HttpResponse.BodyHandlers.ofByteArray());}
    catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Storage request interrupted",e);}
  }
  public void put(String id,MultipartFile file) throws IOException {
    if(!cloud()){Files.createDirectories(local);try(InputStream in=file.getInputStream()){Files.copy(in,safe(id));}return;}
    HttpResponse<byte[]> r=send(request(id).header("Content-Type","application/octet-stream")
      .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes())).build());
    if(r.statusCode()!=200&&r.statusCode()!=201)throw new IOException("Document storage upload failed: HTTP "+r.statusCode());
  }
  public boolean exists(String id) {
    if(id==null||id.isBlank())return false;
    try {
      if(!cloud())return Files.isRegularFile(safe(id));
      HttpResponse<byte[]> r=send(authenticatedRequest(id).method("HEAD",HttpRequest.BodyPublishers.noBody()).build());
      return r.statusCode()==200;
    } catch(Exception e){return false;}
  }
  public byte[] read(String id) throws IOException {
    if(!cloud()){Path path=safe(id);if(!Files.isRegularFile(path))throw new NoSuchFileException(id);return Files.readAllBytes(path);}
    HttpResponse<byte[]> r=send(authenticatedRequest(id).GET().build());
    if(r.statusCode()==404||r.statusCode()==400)throw new NoSuchFileException(id);
    if(r.statusCode()!=200)throw new IOException("Document storage read failed: HTTP "+r.statusCode());
    return r.body();
  }
  public void delete(String id) throws IOException {
    if(id==null||id.isBlank())return;
    if(!cloud()){Files.deleteIfExists(safe(id));return;}
    HttpResponse<byte[]> r=send(request(id).DELETE().build());
    if(r.statusCode()!=200&&r.statusCode()!=204&&r.statusCode()!=404)
      throw new IOException("Document storage delete failed: HTTP "+r.statusCode());
  }
}
