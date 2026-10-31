package com.printqueue;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/owner/operations")
public class InventoryExpenseApi {
 private static final ZoneId ZONE=ZoneId.of("Asia/Kolkata");
 private final PaperStockRepository stocks;
 private final ShopExpenseRepository expenses;
 private final OrderRepository orders;
 private final OrderFileRepository files;
 public InventoryExpenseApi(PaperStockRepository stocks,ShopExpenseRepository expenses,OrderRepository orders,OrderFileRepository files){
  this.stocks=stocks;this.expenses=expenses;this.orders=orders;this.files=files;
 }
 public record StockInput(String name,Long sheets,Long lowStockThreshold,BigDecimal costPerSheet){}
 public record Adjustment(long delta){}
 public record ExpenseInput(String date,String category,String description,BigDecimal amount){}
 public record CostReport(String date,BigDecimal collectedRevenue,BigDecimal expenses,BigDecimal estimatedPaperCost,
    BigDecimal estimatedProfit,long estimatedPaperSheets,boolean paperCostConfigured,BigDecimal bookedAmount,
    List<ShopExpense> expenseEntries){}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private BigDecimal money(BigDecimal value,String label){
  if(value==null||value.signum()<0||value.compareTo(new BigDecimal("100000000"))>0||value.scale()>2)throw bad("Invalid "+label);
  return value.setScale(2,RoundingMode.HALF_UP);
 }
 @GetMapping("/stocks") public List<PaperStock> stocks(){return stocks.findAll().stream().sorted(Comparator.comparing(s->s.id)).toList();}
 @PostMapping("/stocks") public PaperStock createStock(@RequestBody StockInput input){
  if(input==null||input.name()==null||input.name().isBlank()||input.name().trim().length()>80)throw bad("Paper name must be 1-80 characters");
  if(input.sheets()==null||input.sheets()<0||input.sheets()>10000000)throw bad("Invalid sheet quantity");
  if(input.lowStockThreshold()==null||input.lowStockThreshold()<0||input.lowStockThreshold()>10000000)throw bad("Invalid low stock threshold");
  PaperStock s=new PaperStock();s.name=input.name().trim();s.sheets=input.sheets();s.lowStockThreshold=input.lowStockThreshold();s.costPerSheet=money(input.costPerSheet(),"cost per sheet");return stocks.save(s);
 }
 @PutMapping("/stocks/{id}") public PaperStock editStock(@PathVariable Long id,@RequestBody StockInput input){
  PaperStock s=stocks.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Stock not found"));
  if(input.name()==null||input.name().isBlank()||input.name().trim().length()>80)throw bad("Invalid paper name");
  if(input.lowStockThreshold()==null||input.lowStockThreshold()<0||input.lowStockThreshold()>10000000)throw bad("Invalid stock threshold");
  s.name=input.name().trim();s.lowStockThreshold=input.lowStockThreshold();s.costPerSheet=money(input.costPerSheet(),"cost per sheet");return stocks.save(s);
 }
 @PostMapping("/stocks/{id}/adjust") @org.springframework.transaction.annotation.Transactional
 public PaperStock adjust(@PathVariable Long id,@RequestBody Adjustment adjustment){
  PaperStock s=stocks.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Stock not found"));
  if(adjustment==null||adjustment.delta()==0||adjustment.delta()<-10000000||adjustment.delta()>10000000)throw bad("Invalid sheet adjustment");
  long next=s.sheets+adjustment.delta();if(next<0||next>10000000)throw bad("Insufficient stock or sheet limit exceeded");
  s.sheets=next;return stocks.save(s);
 }
 @PostMapping("/expenses") public ShopExpense expense(@RequestBody ExpenseInput input){
  if(input==null)throw bad("Expense required");
  LocalDate date=parseDate(input.date());
  if(input.category()==null||!Set.of("INK_TONER","PAPER_PURCHASE","ELECTRICITY","MAINTENANCE","OTHER").contains(input.category()))throw bad("Invalid expense category");
  if(input.description()==null||input.description().isBlank()||input.description().trim().length()>180)throw bad("Description must be 1-180 characters");
  BigDecimal amount=money(input.amount(),"expense amount");if(amount.signum()==0)throw bad("Expense amount must be greater than zero");
  ShopExpense e=new ShopExpense();e.expenseDate=date;e.category=input.category();e.description=input.description().trim();e.amount=amount;return expenses.save(e);
 }
 @DeleteMapping("/expenses/{id}") public void deleteExpense(@PathVariable Long id){
  if(!expenses.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Expense not found");
  expenses.deleteById(id);
 }
 private LocalDate parseDate(String raw){try{return raw==null||raw.isBlank()?LocalDate.now(ZONE):LocalDate.parse(raw);}catch(Exception e){throw bad("Date must be YYYY-MM-DD");}}
 @GetMapping("/summary") public CostReport summary(@RequestParam(required=false) String date){
  LocalDate day=parseDate(date);
  Instant start=day.atStartOfDay(ZONE).toInstant(),end=day.plusDays(1).atStartOfDay(ZONE).toInstant();
  List<PrintOrder> dayOrders=orders.findAll().stream().filter(o->o.createdAt!=null&&!o.createdAt.isBefore(start)&&o.createdAt.isBefore(end)&&!"CANCELLED".equals(o.status)).toList();
  BigDecimal collected=BigDecimal.ZERO,booked=BigDecimal.ZERO;
  long sheets=0;
  for(PrintOrder o:dayOrders){
   BigDecimal amount=o.totalAmount==null?BigDecimal.ZERO:o.totalAmount;
   booked=booked.add(amount);
   if("PAID".equals(o.paymentStatus)){
    collected=collected.add(amount);
    // Cost estimate only for paid, non-cancelled orders. A4-equivalent, one-sided sheets.
    for(OrderFile f:files.findByOrderIdOrderByIdAsc(o.id)){
     if(!f.customerDeleted&&f.pages>0&&f.copies>0){sheets+=((long)f.pages)*f.copies;}
    }
   }
  }
  List<ShopExpense> entries=expenses.findByExpenseDateOrderByIdDesc(day);
  BigDecimal spent=entries.stream().map(e->e.amount).reduce(BigDecimal.ZERO,BigDecimal::add);
  // Owner designates one stock item as the standard paper cost by creating the first entry.
  List<PaperStock> configured=stocks.findAll().stream().sorted(Comparator.comparing(s->s.id)).toList();
  boolean hasCost=!configured.isEmpty();
  BigDecimal paperCost=hasCost?configured.get(0).costPerSheet.multiply(BigDecimal.valueOf(sheets)):BigDecimal.ZERO;
  return new CostReport(day.toString(),collected,spent,paperCost,
     collected.subtract(spent).subtract(paperCost),sheets,hasCost,booked,entries);
 }
}
