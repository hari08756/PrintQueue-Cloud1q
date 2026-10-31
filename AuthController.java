package com.printqueue;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final UserRepository users;private final PasswordEncoder encoder;
 AuthController(UserRepository users,PasswordEncoder encoder){this.users=users;this.encoder=encoder;}
 public record Registration(String username,String password,String displayName){}
 @PostMapping("/register") public Map<String,String> register(@RequestBody Registration req){
  if(req.username()==null||!req.username().matches("[A-Za-z0-9._@-]{3,80}")||req.password()==null||req.password().length()<8||req.password().length()>128)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Username 3-80 chars; password 8-128 chars");
  String username=req.username().toLowerCase(Locale.ROOT);
  if(users.findByUsername(username).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Username already exists");
  AppUser u=new AppUser();u.username=username;u.passwordHash=encoder.encode(req.password());u.role="CUSTOMER";u.displayName=req.displayName()==null?username:req.displayName().trim();users.save(u);
  return Map.of("username",u.username,"role",u.role);
 }
}
