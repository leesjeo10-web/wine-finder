package org.example.winefinder.common;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",
                error.getReason() == null ? "요청을 처리할 수 없습니다." : error.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException error) {
        var fields = error.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(Map.of("message", "입력값을 확인해 주세요.", "errors", fields));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).body(Map.of("message", "중복되거나 다른 데이터와 충돌하는 요청입니다."));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> uploadSize() {
        return ResponseEntity.status(413).body(Map.of("message", "사진은 5MB 이하로 올려 주세요."));
    }
}
