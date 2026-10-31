package com.printqueue;
import jakarta.persistence.*;
@Entity @Table(name="order_files") public class OrderFile {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id") public PrintOrder order;
 @Column(nullable=false) public String originalName;
 @Column(nullable=false,unique=true) public String storedName;
 @Column(nullable=false) public String mode;
 public int copies;
 public int pages=1;
 // Customer deletion removes the document globally; owner deletion only hides it in the owner queue.
 @Column(nullable=false) public boolean customerDeleted=false;
 @Column(nullable=false) public boolean ownerHidden=false;
 @Column(precision=12,scale=2) public java.math.BigDecimal lineAmount;
}
