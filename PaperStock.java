package com.printqueue;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="paper_stocks") public class PaperStock {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=80) public String name;
 @Column(nullable=false) public long sheets=0;
 @Column(nullable=false) public long lowStockThreshold=100;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal costPerSheet=BigDecimal.ZERO;
}
