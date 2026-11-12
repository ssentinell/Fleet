package ru.student.fleet;

import java.time.*;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoData implements CommandLineRunner {
    private final CarRepository repository;
    private final Clock clock;
    public DemoData(CarRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
    @Override @Transactional public void run(String... args) {
        if (repository.count() != 0) return;
        LocalDate today = LocalDate.now(clock);
        repository.saveAll(List.of(
            car("Toyota", 2020, today.minusDays(2), "Петров Павел Андреевич"),
            car("BMW", 2020, today.minusDays(2), "Смирнова Анна Игоревна"),
            car("Лада", 2021, today.minusDays(7), "Иванов Максим Сергеевич"),
            car("Kia", 2019, today.minusDays(10), "Соколова Мария Олеговна"),
            car("Hyundai", 2022, today.minusDays(14), "Волков Алексей Романович"),
            car("ГАЗ", 2015, today.minusDays(20), "Кузнецова Дарья Ивановна"),
            car("УАЗ", 2021, today.minusDays(29), "Морозов Илья Павлович"),
            car("Skoda", 2023, today, "Петров Павел Андреевич"),
            car("Renault", 2018, today, "Смирнова Анна Игоревна"),
            car("Nissan", 2017, today.minusDays(31), "Лебедев Никита Игоревич"),
            car("Volkswagen", 2016, today.minusDays(45), "Орлова Елена Сергеевна"),
            car("Lexus", 2024, today.minusDays(90), "Егоров Артём Алексеевич")
        ));
    }
    private Car car(String brand, int year, LocalDate day, String owner) {
        return new Car(new CarRequest(brand, year, day, owner));
    }
}
