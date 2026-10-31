package com.printqueue;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
public interface CounterRepository extends JpaRepository<DailyCounter,LocalDate>{
 @Modifying @Transactional @Query(value="INSERT INTO daily_counter (day, value) VALUES (:day, 1) ON CONFLICT (day) DO UPDATE SET value = daily_counter.value + 1",nativeQuery=true)
 int increment(@Param("day") LocalDate day);
 @Modifying @Transactional @Query(value="INSERT INTO daily_counter (day, value, generation) VALUES (:day, 0, 2) ON CONFLICT (day) DO UPDATE SET value = 0, generation = daily_counter.generation + 1",nativeQuery=true)
 int reset(@Param("day") LocalDate day);
}
