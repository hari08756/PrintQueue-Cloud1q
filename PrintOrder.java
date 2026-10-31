package com.printqueue;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="print_orders") public class PrintOrder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true) public String token;
 @Column(nullable=false) public String customerName;
 @Column(nullable=false) public String originalName;
 @Column(nullable=false) public String storedName;
 @Column(nullable=false) public String mode;
 public int copies;
 @Column(nullable=false) public boolean legacyCustomerDeleted=false;
 @Column(nullable=false) public boolean legacyOwnerHidden=false;
 @Column(nullable=false) public String paymentMethod;
 @Column(nullable=false) public String paymentStatus="UNPAID";
 @Column(nullable=false) public String status="PENDING";
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id") public AppUser customer;
 @Column(precision=12,scale=2) public java.math.BigDecimal totalAmount;
 public Instant createdAt=Instant.now();
}
