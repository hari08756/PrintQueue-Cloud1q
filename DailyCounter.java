package com.printqueue;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class DailyCounter { @Id public LocalDate day; public long value; @Column(nullable=false) public int generation=1; }
