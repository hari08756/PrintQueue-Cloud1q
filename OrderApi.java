package com.printqueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
@RestController public class OrderApi {
 private final OrderRepository orders;private final OrderFileRepository files;private final UserRepository users;private final QueueService queue;private final PricingService pricing;
 private final DocumentStore store;private final UploadSettingsService limits;
 OrderApi(OrderRepository orders,OrderFileRepository files,UserRepository users,QueueService queue,PricingService pricing,UploadSettingsService limits,DocumentStore store){
  this.orders=orders;this.files=files;this.users=users;this.queue=queue;this.pricing=pricing;this.limits=limits;this.store=store;}
 @GetMapping("/api/health") public Map<String,String> health(){return Map.of("status","ok");}
 private AppUser me(Authentication auth){return users.findByUsername(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 private OrderView view(PrintOrder o, boolean ownerView){List<FileView> fs=files.findByOrderIdOrderByIdAsc(o.id).stream()
  .filter(f->!f.customerDeleted && (!ownerView || !f.ownerHidden))
  .map(f->new FileView(f.id,f.originalName,f.mode,f.copies,f.pages,f.lineAmount,available(f.storedName))).toList();
  // Older Phase 3 rows have only legacy single-file columns and no ownership.
  if(files.findByOrderIdOrderByIdAsc(o.id).isEmpty()&&o.storedName!=null&&!o.legacyCustomerDeleted&&(!ownerView||!o.legacyOwnerHidden)) fs=List.of(new FileView(0L,o.originalName,o.mode,o.copies,0,null,available(o.storedName)));
  return new OrderView(o.id,o.token,o.customerName,o.status,o.paymentMethod,o.paymentStatus,o.createdAt.toString(),o.totalAmount,fs);}
 private boolean anyVisible(PrintOrder o, boolean ownerView){
  return !view(o,ownerView).files().isEmpty();
 }
 private boolean available(String stored){return store.exists(stored);}
 public record FileView(Long id,String name,String mode,int copies,int pages,java.math.BigDecimal lineAmount,boolean available){}
 public record OrderView(Long id,String token,String customerName,String status,String paymentMethod,String paymentStatus,String createdAt,java.math.BigDecimal totalAmount,List<FileView> files){}
 // An authenticated customer can inspect a PDF before making an order.
 @PostMapping(value="/api/customer/inspect", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Map<String,Object> inspect(@RequestPart("file") MultipartFile file) throws IOException {
  String name=Optional.ofNullable(file.getOriginalFilename()).orElse("");
  if(file.isEmpty()||file.getSize()>(long)limits.current().maxFileMb*1024*1024||!name.toLowerCase(Locale.ROOT).endsWith(".pdf"))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"PDF exceeds the configured upload size limit");
  return Map.of("pages",pdfPages(file),"type","PDF","name",name);
 }
 private int pdfPages(MultipartFile file) throws IOException {
  try(PDDocument doc=Loader.loadPDF(file.getBytes())) {
   int pages=doc.getNumberOfPages();
   if(pages<1||pages>10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"PDF must have 1 to 10000 pages");
   return pages;
  } catch(org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e){
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password-protected PDFs cannot be printed",e);
  } catch(IOException e){
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unreadable or invalid PDF",e);
  }
 }
 @GetMapping("/api/customer/orders") public List<OrderView> mine(Authentication auth){return orders.findByCustomerIdOrderByIdDesc(me(auth).id).stream().map(o->view(o,false)).filter(o->!o.files().isEmpty()).toList();}
 @GetMapping("/api/customer/orders/{id}") public OrderView mineOne(@PathVariable long id,Authentication auth){return view(owned(id,auth),false);}
 private PrintOrder owned(long id,Authentication auth){return orders.findByIdAndCustomerId(id,me(auth).id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));}
 @org.springframework.transaction.annotation.Transactional(rollbackFor=IOException.class)
 @PostMapping(value="/api/customer/orders",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public OrderView submit(Authentication auth,@RequestParam String paymentMethod,@RequestPart("files") List<MultipartFile> incoming,
 @RequestParam("modes") List<String> modes,@RequestParam("copies") List<Integer> copies,@RequestParam(value="pages",required=false) List<Integer> pages) throws IOException {
  UploadSettings settings=limits.current();
  if(incoming.isEmpty()||incoming.size()>settings.maxFiles||incoming.size()!=modes.size()||incoming.size()!=copies.size()||
   (pages!=null&&!pages.isEmpty()&&pages.size()!=incoming.size())||!Set.of("PAY_AT_SHOP","ONLINE").contains(paymentMethod))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid order settings");
  for(int i=0;i<incoming.size();i++){
   MultipartFile f=incoming.get(i);String name=Optional.ofNullable(f.getOriginalFilename()).orElse("");
   String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";
   if(f.isEmpty()||f.getSize()>(long)settings.maxFileMb*1024*1024||!Set.of("pdf","doc","docx","png","jpg","jpeg").contains(ext)||
     !Set.of("BW","COLOR").contains(modes.get(i))||copies.get(i)==null||copies.get(i)<1||copies.get(i)>100||
     (pages!=null&&!pages.isEmpty()&&(pages.get(i)==null||pages.get(i)<1||pages.get(i)>10000)))
     throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid file or print options");
  }
  // Validate every PDF before copying any order file.
  for(MultipartFile f:incoming) if(Optional.ofNullable(f.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT).endsWith(".pdf")) pdfPages(f);
  List<String> created=new ArrayList<>();
  try{
   PrintRate rates=pricing.current();java.math.BigDecimal total=java.math.BigDecimal.ZERO;
   List<OrderFile> savedFiles=new ArrayList<>();
   for(int i=0;i<incoming.size();i++){
    MultipartFile mf=incoming.get(i);String name=Optional.ofNullable(mf.getOriginalFilename()).orElse("document").replace("\\","/");name=name.substring(name.lastIndexOf('/')+1);
    String stored=UUID.randomUUID().toString();store.put(stored,mf);created.add(stored);
    OrderFile of=new OrderFile();of.originalName=name;of.storedName=stored;of.mode=modes.get(i);of.copies=copies.get(i);of.pages=name.toLowerCase(Locale.ROOT).endsWith(".pdf")?pdfPages(mf):((pages==null||pages.isEmpty())?1:pages.get(i));
    of.lineAmount=pricing.cost(of.mode,of.pages,of.copies,rates);total=total.add(of.lineAmount);savedFiles.add(of);
   }
   AppUser customer=me(auth);PrintOrder order=new PrintOrder();order.customer=customer;order.customerName=customer.displayName;
   order.originalName=savedFiles.get(0).originalName;order.storedName=savedFiles.get(0).storedName;order.mode=savedFiles.get(0).mode;
   order.copies=savedFiles.get(0).copies;order.paymentMethod=paymentMethod;order.totalAmount=total;
   // Token assigned after every file has been copied, within server-side queue transaction.
   order.token=queue.next();orders.save(order);
   for(OrderFile f:savedFiles){f.order=order;files.save(f);}
   return view(order,false);
  }catch(IOException|RuntimeException ex){for(String id:created)try{store.delete(id);}catch(IOException ignored){}throw ex;}
 }
 // One click removal of a complete order's documents.
 // Customer removal deletes underlying file bytes and removes them from BOTH views.
 @org.springframework.transaction.annotation.Transactional
 @DeleteMapping("/api/customer/orders/{id}/documents")
 public ResponseEntity<Void> customerDeleteSet(@PathVariable long id,Authentication auth) throws IOException {
  PrintOrder order=owned(id,auth);
  List<OrderFile> attached=files.findByOrderIdOrderByIdAsc(id);
  for(OrderFile file:attached){if(!file.customerDeleted){deleteStored(file.storedName);file.customerDeleted=true;files.save(file);}}
  if(attached.isEmpty()&&!order.legacyCustomerDeleted){deleteStored(order.storedName);order.legacyCustomerDeleted=true;orders.save(order);}
  return ResponseEntity.noContent().build();
 }
 // Owner removal hides the WHOLE set only in owner view: customer retains documents.
 @org.springframework.transaction.annotation.Transactional
 @DeleteMapping("/api/owner/orders/{id}/documents")
 public ResponseEntity<Void> ownerHideSet(@PathVariable long id){
  PrintOrder order=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  List<OrderFile> attached=files.findByOrderIdOrderByIdAsc(id);
  for(OrderFile file:attached){if(!file.customerDeleted&&!file.ownerHidden){file.ownerHidden=true;files.save(file);}}
  if(attached.isEmpty()&&!order.legacyCustomerDeleted){order.legacyOwnerHidden=true;orders.save(order);}
  return ResponseEntity.noContent().build();
 }
 @PostMapping("/api/owner/tokens/reset") public Map<String,String> resetTokens(){
  return Map.of("nextToken",queue.reset(),"message","Next issued token starts at 0001 in a new unique reset series");
 }
 @GetMapping("/api/owner/orders") public List<OrderView> all(){return orders.findAllByOrderByIdAsc().stream().map(o->view(o,true)).filter(o->!o.files().isEmpty()).toList();}
 @PatchMapping("/api/owner/orders/{id}/status") public OrderView status(@PathVariable long id,@RequestBody Map<String,String> payload){
  PrintOrder o=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));String s=payload.get("status");
  if(!Set.of("PENDING","PRINTING","READY","COLLECTED","CANCELLED").contains(s))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid status");
  o.status=s;return view(orders.save(o),true);
 }
 // Customer deletion is global: physically remove the document and hide it from both queues.
 // No endpoint accepts a user ID: ownership is resolved from the authenticated principal.
 @org.springframework.transaction.annotation.Transactional
 @DeleteMapping("/api/customer/orders/{orderId}/files/{fileId}")
 public ResponseEntity<Void> customerDeleteFile(@PathVariable long orderId,@PathVariable long fileId,Authentication auth) throws IOException {
  PrintOrder order=owned(orderId,auth);
  List<OrderFile> attached=files.findByOrderIdOrderByIdAsc(orderId);
  if(fileId==0 && attached.isEmpty()) {
   if(order.legacyCustomerDeleted)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document already deleted");
   deleteStored(order.storedName);
   order.legacyCustomerDeleted=true;
   orders.save(order);
  } else {
   OrderFile file=files.findByIdAndOrderId(fileId,orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
   if(file.customerDeleted)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document already deleted");
   deleteStored(file.storedName);
   file.customerDeleted=true;
   files.save(file);
  }
  return ResponseEntity.noContent().build();
 }
 // Owner deletion is view-only: customer ownership and actual file bytes remain intact.
 @org.springframework.transaction.annotation.Transactional
 @DeleteMapping("/api/owner/orders/{orderId}/files/{fileId}")
 public ResponseEntity<Void> ownerHideFile(@PathVariable long orderId,@PathVariable long fileId) {
  PrintOrder order=orders.findById(orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  List<OrderFile> attached=files.findByOrderIdOrderByIdAsc(orderId);
  if(fileId==0 && attached.isEmpty()) {
   if(order.legacyCustomerDeleted || order.legacyOwnerHidden)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
   order.legacyOwnerHidden=true;
   orders.save(order);
  } else {
   OrderFile file=files.findByIdAndOrderId(fileId,orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
   if(file.customerDeleted || file.ownerHidden)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
   file.ownerHidden=true;
   files.save(file);
  }
  return ResponseEntity.noContent().build();
 }
 private void deleteStored(String stored) throws IOException {store.delete(stored);}
 // Owner-only print preview. Word documents are converted to PDF with local LibreOffice.
 @GetMapping("/api/owner/orders/{orderId}/files/{fileId}/print-preview")
 public ResponseEntity<org.springframework.core.io.Resource> printPreview(@PathVariable long orderId,@PathVariable long fileId) throws IOException {
  PrintOrder order=orders.findById(orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  String stored;String name;
  if(fileId==0&&files.findByOrderIdOrderByIdAsc(orderId).isEmpty()){if(order.legacyCustomerDeleted||order.legacyOwnerHidden)throw new ResponseStatusException(HttpStatus.NOT_FOUND);stored=order.storedName;name=order.originalName;}
  else {OrderFile entry=files.findByIdAndOrderId(fileId,orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(entry.customerDeleted||entry.ownerHidden)throw new ResponseStatusException(HttpStatus.NOT_FOUND);stored=entry.storedName;name=entry.originalName;}
  if(stored==null||name==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"File record unavailable");
  byte[] content;
  try {content=store.read(stored);}catch(NoSuchFileException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found",e);}
  String lower=name.toLowerCase(Locale.ROOT);
  String mime;
  if(lower.endsWith(".pdf")) mime="application/pdf";
  else if(lower.endsWith(".png")) mime="image/png";
  else if(lower.endsWith(".jpg")||lower.endsWith(".jpeg")) mime="image/jpeg";
  else if(lower.endsWith(".doc")||lower.endsWith(".docx")) return convertWordPreview(content,lower.endsWith(".docx")?"docx":"doc");
  else throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Unsupported print format");
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime))
   .header(HttpHeaders.CONTENT_DISPOSITION,"inline")
   .header(HttpHeaders.CACHE_CONTROL,"no-store")
   .header("X-Content-Type-Options","nosniff")
   .body(new org.springframework.core.io.ByteArrayResource(content));
 }
 private ResponseEntity<org.springframework.core.io.Resource> convertWordPreview(byte[] content,String extension) throws IOException {
  // LibreOffice is optional and runs locally. Use isolated temporary files and a unique profile.
  Path temp=Files.createTempDirectory("printqueue-print-");
  try {
   Path input=temp.resolve("document."+extension);
   Files.write(input,content);
   Path profile=temp.resolve("profile");
   String office=System.getenv().getOrDefault("LIBREOFFICE_PATH","soffice");
   Process process;
   try {
    process=new ProcessBuilder(office,"-env:UserInstallation="+profile.toUri(),"--headless","--convert-to","pdf","--outdir",temp.toString(),input.toString())
     .redirectErrorStream(true).redirectOutput(temp.resolve("conversion.log").toFile()).start();
   } catch(IOException e) {
    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Install LibreOffice or set LIBREOFFICE_PATH to soffice.exe for Word printing",e);
   }
   boolean finished;
   try {finished=process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS);}
   catch(InterruptedException e){Thread.currentThread().interrupt();process.destroyForcibly();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Word conversion interrupted",e);}
   if(!finished){process.destroyForcibly();throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,"Word conversion timed out");}
   Path pdf=temp.resolve("document.pdf");
   if(process.exitValue()!=0||!Files.isRegularFile(pdf))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Unable to convert Word document to PDF");
   // Load into memory before removing temporary conversion files (15 MB input limit, bounded locally).
   byte[] data=Files.readAllBytes(pdf);
   if(data.length>50L*1024*1024)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Converted PDF is too large");
   return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
    .header(HttpHeaders.CONTENT_DISPOSITION,"inline")
    .header(HttpHeaders.CACHE_CONTROL,"no-store")
    .header("X-Content-Type-Options","nosniff")
    .body(new org.springframework.core.io.ByteArrayResource(data));
  } finally {
   try(var paths=Files.walk(temp)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException ignored){}});}
  }
 }
 @GetMapping("/api/owner/orders/{orderId}/files/{fileId}") public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable long orderId,@PathVariable long fileId) throws IOException {
  PrintOrder o=orders.findById(orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  String stored;String name;
  if(fileId==0&&files.findByOrderIdOrderByIdAsc(orderId).isEmpty()){if(o.legacyCustomerDeleted||o.legacyOwnerHidden)throw new ResponseStatusException(HttpStatus.NOT_FOUND);stored=o.storedName;name=o.originalName;}
  else {OrderFile f=files.findByIdAndOrderId(fileId,orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(f.customerDeleted||f.ownerHidden)throw new ResponseStatusException(HttpStatus.NOT_FOUND);stored=f.storedName;name=f.originalName;}
  if(stored==null||stored.isBlank())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No file attached");
  byte[] content;
  try {content=store.read(stored);}catch(NoSuchFileException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found",e);}
  String safeName=name.replaceAll("[\r\n\"]","_");
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+java.net.URLEncoder.encode(safeName,java.nio.charset.StandardCharsets.UTF_8).replace("+","%20"))
   .contentType(MediaType.APPLICATION_OCTET_STREAM).body(new org.springframework.core.io.ByteArrayResource(content));
 }
}
