package com.printqueue;
import jakarta.persistence.*;
@Entity @Table(name="shop_branding") public class ShopBranding {
 @Id public Long id=1L;
 @Column(nullable=false,length=100) public String shopName="My Print Shop";
 @Column(length=25) public String phone="";
 @Column(length=220) public String address="";
 @Column(length=250) public String receiptMessage="Thank you for choosing us!";
 @Column(length=40) public String logoType;
 @Column(columnDefinition="bytea") public byte[] logoBytes;
}
