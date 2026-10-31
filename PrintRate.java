package com.printqueue;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="print_rates") public class PrintRate {
 @Id public Long id=1L;
 @Column(nullable=false,precision=10,scale=2) public BigDecimal bwRate=new BigDecimal("2.00");
 @Column(nullable=false,precision=10,scale=2) public BigDecimal colorRate=new BigDecimal("10.00");
}
