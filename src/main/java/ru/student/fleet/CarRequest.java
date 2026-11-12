package ru.student.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CarRequest(
    @JsonProperty("марка") @NotBlank(message = "Введите марку автомобиля")
    @Size(max = 100, message = "Марка: не более 100 символов") String марка,
    @NotNull(message = "Введите год выпуска") @Min(value = 1886, message = "Год выпуска должен быть не раньше 1886") Integer productionYear,
    @NotNull(message = "Введите дату постановки на учёт") LocalDate registeredOn,
    @NotBlank(message = "Введите ФИО владельца") @Size(max = 200, message = "ФИО: не более 200 символов") String ownerName
) { }
