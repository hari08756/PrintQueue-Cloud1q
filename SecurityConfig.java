package com.printqueue;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpMethod;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.List;
import java.util.Locale;
@Configuration public class SecurityConfig {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService details(UserRepository users){return name -> {
  AppUser u=users.findByUsername(name).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
  return new User(u.username,u.passwordHash,List.of(new SimpleGrantedAuthority("ROLE_"+u.role)));
 };}
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.csrf(csrf->csrf.disable()).cors(Customizer.withDefaults())
   .authorizeHttpRequests(auth->auth
    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
    .requestMatchers("/error").permitAll()
    .requestMatchers("/api/health","/api/test","/api/auth/register").permitAll()
    .requestMatchers("/", "/index.html", "/customer.html", "/assets/**", "/favicon.ico").permitAll()
    .requestMatchers("/api/owner/**").hasRole("OWNER")
    .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
    .anyRequest().denyAll())
   .httpBasic(Customizer.withDefaults()).build();
 }
 @Bean CorsConfigurationSource cors(){CorsConfiguration cfg=new CorsConfiguration();
  cfg.setAllowedOrigins(List.of("http://localhost:5173","http://127.0.0.1:5173"));cfg.setAllowedMethods(List.of("GET","POST","PATCH","PUT","DELETE","OPTIONS"));
  cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));CorsConfigurationSource src=null;
  UrlBasedCorsConfigurationSource urls=new UrlBasedCorsConfigurationSource();urls.registerCorsConfiguration("/api/**",cfg);return urls;
 }
 @Bean CommandLineRunner createOwner(UserRepository users, PasswordEncoder passwords,
 @Value("${app.owner.username:owner}") String username,
 @Value("${app.owner.password:}") String password,
 @Value("${app.owner.reset-password:false}") boolean resetPassword){return args->{
  if(password == null || password.isBlank()) throw new IllegalStateException("Set OWNER_PASSWORD before starting backend");
  String normalized = username.trim().toLowerCase(Locale.ROOT);
  AppUser owner = users.findByUsername(normalized).orElse(null);
  if (owner == null) {
   owner = new AppUser();
   owner.username = normalized;
   owner.displayName = "Print shop owner";
   owner.role = "OWNER";
   owner.passwordHash = passwords.encode(password);
   users.save(owner);
   System.out.println("PrintQueue: owner account created: " + normalized);
  } else if (resetPassword) {
   if (!"OWNER".equals(owner.role)) throw new IllegalStateException("Cannot reset a non-owner account: " + normalized);
   owner.passwordHash = passwords.encode(password);
   users.save(owner);
   System.out.println("PrintQueue: owner password reset for: " + normalized);
  } else if (!"OWNER".equals(owner.role)) {
   throw new IllegalStateException("Configured owner username belongs to non-owner user: " + normalized);
  }
 };}
}
