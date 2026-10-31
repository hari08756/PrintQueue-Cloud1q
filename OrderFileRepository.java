package com.printqueue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderFileRepository extends JpaRepository<OrderFile,Long>{List<OrderFile> findByOrderIdOrderByIdAsc(Long orderId);Optional<OrderFile> findByIdAndOrderId(Long id,Long orderId);}
