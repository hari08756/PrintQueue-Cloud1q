package com.printqueue;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.Base64;
@RestController public class ShopBrandingApi {
 private final ShopBrandingRepository repo;
 ShopBrandingApi(ShopBrandingRepository repo){this.repo=repo;}
 public record Brand(String shopName,String logoDataUrl,String phone,String address,String receiptMessage) {}
 private ShopBranding current(){return repo.findById(1L).orElseGet(ShopBranding::new);}
 private Brand view(){ShopBranding b=current();String logo=b.logoBytes==null?null:"data:"+b.logoType+";base64,"+Base64.getEncoder().encodeToString(b.logoBytes);return new Brand(b.shopName,logo,b.phone,b.address,b.receiptMessage);}
 @GetMapping("/api/customer/shop-branding") public Brand customer(){return view();}
 @GetMapping("/api/owner/shop-branding") public Brand owner(){return view();}
 @PutMapping(value="/api/owner/shop-branding",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Brand save(@RequestParam("shopName") String shopName,@RequestParam(value="logo",required=false) MultipartFile logo,@RequestParam(value="removeLogo",defaultValue="false") boolean removeLogo,@RequestParam(value="phone",defaultValue="") String phone,@RequestParam(value="address",defaultValue="") String address,@RequestParam(value="receiptMessage",defaultValue="") String receiptMessage) throws IOException {
  String name=shopName.trim();if(name.isBlank()||name.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Shop name must be 1-100 characters");
  if(phone.length()>25 || !phone.matches("[0-9+() .-]*")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid shop contact number");
  if(address.length()>220 || receiptMessage.length()>250) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Shop details too long");
  ShopBranding b=current();b.shopName=name;b.phone=phone.trim();b.address=address.trim();b.receiptMessage=receiptMessage.trim();
  if(removeLogo){b.logoBytes=null;b.logoType=null;}
  if(logo!=null&&!logo.isEmpty()){
   if(logo.getSize()>300*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Logo must be 300 KB or smaller");
   byte[] bytes=logo.getBytes();String type=logo.getContentType();
   boolean png=bytes.length>8&&(bytes[0]&255)==137&&(bytes[1]&255)==80&&(bytes[2]&255)==78&&(bytes[3]&255)==71;
   boolean jpg=bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255;
   if(!(png&&"image/png".equals(type))&&!(jpg&&"image/jpeg".equals(type)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Upload a valid PNG or JPG image");
   b.logoBytes=bytes;b.logoType=png?"image/png":"image/jpeg";
  }
  b.id=1L;repo.save(b);return view();
 }
}
