package com.printqueue;
import jakarta.persistence.*;
@Entity @Table(name="app_users") public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true,length=150) public String username;
 @Column(nullable=false) public String passwordHash;
 @Column(nullable=false) public String role;
 public String displayName;
}
