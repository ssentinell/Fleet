package ru.student.fleet;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Одна запись соответствует одному автомобилю в автопарке. */
@Entity
@Table(name = "cars")
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Вариант Н–Т: имя поля Java и колонки MySQL — кириллицей.
    @Column(name = "`марка`", nullable = false, length = 100)
    private String марка;
    @Column(nullable = false)
    private Integer productionYear;
    @Column(nullable = false)
    private LocalDate registeredOn;
    @Column(nullable = false, length = 200)
    private String ownerName;

    protected Car() { }
    public Car(CarRequest request) { update(request); }
    public void update(CarRequest request) {
        марка = request.марка().strip();
        productionYear = request.productionYear();
        registeredOn = request.registeredOn();
        ownerName = request.ownerName().strip();
    }
    public Long getId() { return id; }
    public String getМарка() { return марка; }
    public Integer getProductionYear() { return productionYear; }
    public LocalDate getRegisteredOn() { return registeredOn; }
    public String getOwnerName() { return ownerName; }
}
