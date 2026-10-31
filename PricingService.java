package com.printqueue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
@Service public class PricingService {
 private final PrintRateRepository repository;
 public PricingService(PrintRateRepository repository){this.repository=repository;}
 @Transactional public PrintRate current(){return repository.findById(1L).orElseGet(()->repository.save(new PrintRate()));}
 @Transactional public PrintRate change(BigDecimal bw, BigDecimal color){
  if(bw==null||color==null||bw.signum()<0||color.signum()<0||bw.scale()>2||color.scale()>2||
     bw.compareTo(new BigDecimal("10000"))>0||color.compareTo(new BigDecimal("10000"))>0)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rates must be between 0 and 10000 with at most 2 decimals");
  PrintRate p=current();p.bwRate=bw;p.colorRate=color;return repository.save(p);
 }
 public BigDecimal cost(String mode,int pages,int copies,PrintRate rates){
  return ("COLOR".equals(mode)?rates.colorRate:rates.bwRate).multiply(BigDecimal.valueOf((long)pages*copies));
 }
}
