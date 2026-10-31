package com.printqueue;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Owner-only reporting. Amounts are taken from stored order totals, never client supplied. */
@RestController
@RequestMapping("/api/owner")
public class SalesReportApi {
 private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Kolkata");
 private final OrderRepository orders;
 public SalesReportApi(OrderRepository orders){this.orders=orders;}

 @PatchMapping("/orders/{id}/payment")
 public Map<String,String> payment(@PathVariable long id, @RequestBody Map<String,String> input){
  String status=input.get("paymentStatus");
  if(!Set.of("PAID","UNPAID").contains(status))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment status must be PAID or UNPAID");
  PrintOrder order=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));
  order.paymentStatus=status;
  orders.save(order);
  return Map.of("paymentStatus",status);
 }

 public record ReportOrder(Long id,String token,String customerName,String status,String paymentStatus,
  String paymentMethod,BigDecimal totalAmount,String createdAt){}
 public record DailyReport(String date,String timezone,int totalOrders,int paidOrders,int unpaidOrders,
  int cancelledOrders,BigDecimal bookedAmount,BigDecimal collectedAmount,BigDecimal outstandingAmount,
  List<ReportOrder> orders){}

 @GetMapping("/reports/daily")
 public DailyReport daily(@RequestParam(required=false) String date){
  LocalDate selected;
  try { selected=(date==null||date.isBlank())?LocalDate.now(SHOP_ZONE):LocalDate.parse(date); }
  catch(java.time.format.DateTimeParseException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use YYYY-MM-DD date");}
  Instant begin=selected.atStartOfDay(SHOP_ZONE).toInstant();
  Instant end=selected.plusDays(1).atStartOfDay(SHOP_ZONE).toInstant();
  List<PrintOrder> matches=orders.findAll().stream()
   .filter(o->o.createdAt!=null&&!o.createdAt.isBefore(begin)&&o.createdAt.isBefore(end))
   .sorted(Comparator.comparing((PrintOrder o)->o.createdAt).thenComparing(o->o.id))
   .toList();
  BigDecimal booked=BigDecimal.ZERO, collected=BigDecimal.ZERO, outstanding=BigDecimal.ZERO;
  int paid=0, unpaid=0, cancelled=0;
  List<ReportOrder> items=new ArrayList<>();
  for(PrintOrder o:matches){
   boolean isCancelled="CANCELLED".equals(o.status);
   boolean isPaid="PAID".equals(o.paymentStatus);
   BigDecimal amount=o.totalAmount==null?BigDecimal.ZERO:o.totalAmount;
   if(isCancelled) cancelled++;
   else {
    booked=booked.add(amount);
    if(isPaid){paid++; collected=collected.add(amount);}
    else {unpaid++; outstanding=outstanding.add(amount);}
   }
   items.add(new ReportOrder(o.id,o.token,o.customerName,o.status,o.paymentStatus,o.paymentMethod,o.totalAmount,o.createdAt.toString()));
  }
  return new DailyReport(selected.toString(),SHOP_ZONE.toString(),matches.size(),paid,unpaid,cancelled,
   booked,collected,outstanding,items);
 }
}
