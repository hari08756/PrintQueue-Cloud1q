package com.printqueue;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@RestController public class PricingApi {
 private final PricingService pricing;
 public PricingApi(PricingService pricing){this.pricing=pricing;}
 public record Rates(BigDecimal bwRate,BigDecimal colorRate){}
 @GetMapping("/api/customer/pricing") public Rates customer(){return map(pricing.current());}
 @GetMapping("/api/owner/pricing") public Rates owner(){return map(pricing.current());}
 @PutMapping("/api/owner/pricing") public Rates update(@RequestBody Rates input){return map(pricing.change(input.bwRate(),input.colorRate()));}
 private Rates map(PrintRate p){return new Rates(p.bwRate,p.colorRate);}
}
