package com.printqueue;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;
@Entity @Table(name="shop_expenses") public class ShopExpense {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public LocalDate expenseDate;
 @Column(nullable=false,length=32) public String category;
 @Column(nullable=false,length=180) public String description;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal amount;
}
