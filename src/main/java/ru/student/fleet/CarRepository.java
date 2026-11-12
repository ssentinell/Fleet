package ru.student.fleet;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {
    @Query("select c.registeredOn as day, count(c) as total from Car c " +
            "where c.registeredOn between :from and :to group by c.registeredOn order by c.registeredOn")
    List<DailyCount> countRegistrations(LocalDate from, LocalDate to);
    interface DailyCount { LocalDate getDay(); long getTotal(); }
}
