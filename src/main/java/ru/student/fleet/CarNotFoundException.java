package ru.student.fleet;

public class CarNotFoundException extends RuntimeException {
    public CarNotFoundException(long id) { super("Автомобиль с ID " + id + " не найден"); }
}
