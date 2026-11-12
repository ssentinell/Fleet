package ru.student.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record CarResponse(Long id, @JsonProperty("марка") String марка, Integer productionYear,
                          LocalDate registeredOn, String ownerName) {
    static CarResponse from(Car car) {
        return new CarResponse(car.getId(), car.getМарка(), car.getProductionYear(), car.getRegisteredOn(), car.getOwnerName());
    }
}
