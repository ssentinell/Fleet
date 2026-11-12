package ru.student.fleet;

import jakarta.persistence.criteria.Predicate;
import java.text.Collator;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CarService {
    private final CarRepository repository;
    private final Clock clock;
    public CarService(CarRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }

    public CarList search(String brand, String owner, Long id, Integer yearFrom, Integer yearTo,
                          LocalDate from, LocalDate to, boolean sorted) {
        if (yearFrom != null && yearTo != null && yearFrom > yearTo)
            throw new IllegalArgumentException("Начальный год должен быть не больше конечного");
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("Начало периода должно быть не позже конца");
        Specification<Car> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (id != null) predicates.add(cb.equal(root.get("id"), id));
            for (var entry : Map.of("марка", brand, "ownerName", owner).entrySet()) {
                if (!entry.getValue().isBlank()) {
                    String value = entry.getValue().strip().toLowerCase(Locale.ROOT)
                            .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                    predicates.add(cb.like(cb.lower(root.get(entry.getKey())), "%" + value + "%", '!'));
                }
            }
            if (yearFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("productionYear"), yearFrom));
            if (yearTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("productionYear"), yearTo));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("registeredOn"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("registeredOn"), to));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        // Вариант Н–Т: год ↑, затем марка ↑. ID разрешает полное совпадение ключей.
        Comparator<Car> order = sorted
                ? Comparator.comparing(Car::getProductionYear)
                    .thenComparing(Car::getМарка, Collator.getInstance(Locale.forLanguageTag("ru")))
                    .thenComparing(Car::getId)
                : Comparator.comparing(Car::getId);
        return new CarList(repository.findAll(spec).stream().sorted(order).map(CarResponse::from).toList(), repository.count());
    }
    public CarResponse get(long id) { return CarResponse.from(find(id)); }
    @Transactional public CarResponse create(CarRequest data) {
        validateDates(data); return CarResponse.from(repository.save(new Car(data)));
    }
    @Transactional public CarResponse update(long id, CarRequest data) {
        Car car = find(id); validateDates(data); car.update(data); return CarResponse.from(repository.save(car));
    }
    @Transactional public void delete(long id) { repository.delete(find(id)); }
    private Car find(long id) { return repository.findById(id).orElseThrow(() -> new CarNotFoundException(id)); }
    private void validateDates(CarRequest data) {
        LocalDate today = LocalDate.now(clock);
        if (data.productionYear() > today.getYear())
            throw new IllegalArgumentException("Год выпуска не может быть больше текущего года");
        if (data.registeredOn().isAfter(today))
            throw new IllegalArgumentException("Дата постановки на учёт не может быть в будущем");
        if (data.registeredOn().getYear() < data.productionYear())
            throw new IllegalArgumentException("Дата постановки на учёт не может быть раньше года выпуска");
    }
    public Histogram histogram() {
        LocalDate to = LocalDate.now(clock), from = to.minusDays(29);
        Map<LocalDate, Long> counts = repository.countRegistrations(from, to).stream()
                .collect(Collectors.toMap(CarRepository.DailyCount::getDay, CarRepository.DailyCount::getTotal));
        List<DayCount> days = from.datesUntil(to.plusDays(1)).map(day -> new DayCount(day, counts.getOrDefault(day, 0L))).toList();
        return new Histogram(from, to, days.stream().mapToLong(DayCount::count).sum(), days);
    }
    public record CarList(List<CarResponse> cars, long total) { }
    public record DayCount(LocalDate date, long count) { }
    public record Histogram(LocalDate from, LocalDate to, long total, List<DayCount> days) { }
}
