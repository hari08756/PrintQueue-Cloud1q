package com.printqueue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderRepository extends JpaRepository<PrintOrder,Long>{
 List<PrintOrder> findAllByOrderByIdAsc();
 List<PrintOrder> findByCustomerIdOrderByIdDesc(Long customerId);
 Optional<PrintOrder> findByIdAndCustomerId(Long id,Long customerId);
}
