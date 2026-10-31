package com.printqueue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Service public class QueueService {
 private final CounterRepository counters;
 public QueueService(CounterRepository counters){this.counters=counters;}
 @Transactional public String next(){
  LocalDate day=LocalDate.now(ZoneId.of("Asia/Kolkata"));
  counters.increment(day);
  DailyCounter c=counters.findById(day).orElseThrow();
  String base=day.toString().replace("-", "");
  return base + (c.generation>1?"-R"+c.generation:"") + "-" + String.format("%04d",c.value);
 }
 @Transactional public String reset(){
  LocalDate day=LocalDate.now(ZoneId.of("Asia/Kolkata"));
  counters.reset(day);
  DailyCounter c=counters.findById(day).orElseThrow();
  return day.toString().replace("-", "")+"-R"+c.generation+"-0001";
 }
}
