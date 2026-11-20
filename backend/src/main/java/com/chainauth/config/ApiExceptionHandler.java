package com.chainauth.config;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<Map<String,Object>> handleStatus(ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(Map.of("timestamp",Instant.now().toString(),"status",ex.getStatusCode().value(),"error",ex.getReason()==null?"Request failed":ex.getReason()));}
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex){String message=ex.getBindingResult().getFieldErrors().stream().findFirst().map(e->e.getField()+": "+e.getDefaultMessage()).orElse("Invalid request");return ResponseEntity.badRequest().body(Map.of("timestamp",Instant.now().toString(),"status",400,"error",message));}
 @ExceptionHandler(Exception.class)
 public ResponseEntity<Map<String,Object>> handle(Exception ex){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("timestamp",Instant.now().toString(),"status",500,"error","Internal server error"));}
}
