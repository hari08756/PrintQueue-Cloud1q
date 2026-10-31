package com.printqueue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service public class UploadSettingsService {
 private final UploadSettingsRepository repo;
 public UploadSettingsService(UploadSettingsRepository repo){this.repo=repo;}
 @Transactional public UploadSettings current(){return repo.findById(1L).orElseGet(()->repo.save(new UploadSettings()));}
 @Transactional public UploadSettings update(UploadSettings requested){
  if(requested.maxFiles<1||requested.maxFiles>30||requested.maxFileMb<1||requested.maxFileMb>50)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Allowed: 1-30 files, 1-50 MB per file");
  UploadSettings setting=current();setting.maxFiles=requested.maxFiles;setting.maxFileMb=requested.maxFileMb;
  return repo.save(setting);
 }
}
