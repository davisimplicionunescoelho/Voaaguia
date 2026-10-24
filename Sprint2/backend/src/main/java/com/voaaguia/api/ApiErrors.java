package com.voaaguia.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.*;
import org.springframework.dao.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Requisição inválida":e.getReason())); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e) { return ResponseEntity.badRequest().body(Map.of("message","Revise os campos informados", "errors",e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()+": "+f.getDefaultMessage()).toList())); }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<?> malformed() { return ResponseEntity.badRequest().body(Map.of("message","JSON ou formato de campo inválido")); }
    @ExceptionHandler({DuplicateKeyException.class,OptimisticLockingFailureException.class}) ResponseEntity<?> conflict() { return ResponseEntity.status(409).body(Map.of("message","Registro já existente ou alterado por outra pessoa. Atualize e tente novamente.")); }
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> forbidden() { return ResponseEntity.status(403).body(Map.of("message","Perfil sem permissão para esta operação")); }
}
