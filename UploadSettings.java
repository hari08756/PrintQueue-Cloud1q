package com.printqueue;
import jakarta.persistence.*;
@Entity @Table(name="upload_settings") public class UploadSettings {
 @Id public Long id=1L;
 @Column(nullable=false) public int maxFiles=10;
 @Column(nullable=false) public int maxFileMb=15;
}
