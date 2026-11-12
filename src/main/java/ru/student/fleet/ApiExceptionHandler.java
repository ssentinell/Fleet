package ru.student.fleet;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(CarNotFoundException.class)
    ResponseEntity<ApiError> notFound(CarNotFoundException e) { return error(HttpStatus.NOT_FOUND, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream().map(x -> x.getDefaultMessage()).distinct().sorted().reduce((a, b) -> a + ". " + b).orElse("Проверьте поля");
        return error(HttpStatus.BAD_REQUEST, message);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidArgument(IllegalArgumentException e) { return error(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalidFormat(Exception e) { return error(HttpStatus.BAD_REQUEST, "Неверный формат данных. Даты: ГГГГ-ММ-ДД, ID: целое число"); }
    private ResponseEntity<ApiError> error(HttpStatus status, String message) { return ResponseEntity.status(status).body(new ApiError(message)); }
    public record ApiError(String message) { }
}
