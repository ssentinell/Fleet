package ru.student.fleet;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cars")
public class CarController {
    private final CarService service;
    private final Clock clock;
    public CarController(CarService service, Clock clock) { this.service = service; this.clock = clock; }
    @GetMapping public CarService.CarList list(
            @RequestParam(name = "марка", defaultValue = "") String brand,
            @RequestParam(defaultValue = "") String owner,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "false") boolean sorted) {
        return service.search(brand, owner, id, yearFrom, yearTo, from, to, sorted);
    }
    @GetMapping("/histogram") public CarService.Histogram histogram() { return service.histogram(); }
    @GetMapping("/today") public LocalDate today() { return LocalDate.now(clock); }
    @GetMapping("/{id}") public CarResponse get(@PathVariable long id) { return service.get(id); }
    @PostMapping public ResponseEntity<CarResponse> create(@Valid @RequestBody CarRequest request) {
        CarResponse car = service.create(request);
        return ResponseEntity.created(URI.create("/api/cars/" + car.id())).body(car);
    }
    @PutMapping("/{id}") public CarResponse update(@PathVariable long id, @Valid @RequestBody CarRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
