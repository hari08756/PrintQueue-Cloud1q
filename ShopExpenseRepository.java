package com.printqueue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface ShopExpenseRepository extends JpaRepository<ShopExpense,Long>{
 List<ShopExpense> findByExpenseDateOrderByIdDesc(LocalDate expenseDate);
}
