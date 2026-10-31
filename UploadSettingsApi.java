package com.printqueue;
import org.springframework.web.bind.annotation.*;
@RestController public class UploadSettingsApi {
 private final UploadSettingsService service;
 UploadSettingsApi(UploadSettingsService service){this.service=service;}
 public record Limits(int maxFiles,int maxFileMb){}
 private Limits view(){UploadSettings s=service.current();return new Limits(s.maxFiles,s.maxFileMb);}
 @GetMapping("/api/customer/upload-settings") public Limits customer(){return view();}
 @GetMapping("/api/owner/upload-settings") public Limits owner(){return view();}
 @PutMapping("/api/owner/upload-settings") public Limits save(@RequestBody Limits body){
  UploadSettings s=new UploadSettings();s.maxFiles=body.maxFiles();s.maxFileMb=body.maxFileMb();service.update(s);return view();
 }
}
