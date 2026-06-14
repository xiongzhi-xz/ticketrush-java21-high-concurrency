package com.ticketrush.common.exception;

import com.ticketrush.common.api.ApiResponse;
import com.ticketrush.common.api.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>高并发接口必须保证失败响应稳定可观测，不能把 Java 异常栈直接暴露给调用方。
 * 后续压测时可以直接按错误码统计失败原因。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(ApiResponse.failure(errorCode, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return badRequest(formatFieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        return badRequest(formatFieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return badRequest(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable() {
        return ResponseEntity
                .status(ErrorCode.REQUEST_BODY_INVALID.httpStatus())
                .body(ApiResponse.failure(ErrorCode.REQUEST_BODY_INVALID));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException() {
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return ResponseEntity
                .status(ErrorCode.PARAM_INVALID.httpStatus())
                .body(ApiResponse.failure(ErrorCode.PARAM_INVALID, message));
    }

    private String formatFieldErrors(Iterable<FieldError> fieldErrors) {
        String message = toMessage(fieldErrors);
        if (message.isBlank()) {
            return ErrorCode.PARAM_INVALID.message();
        }
        return message;
    }

    private String toMessage(Iterable<FieldError> fieldErrors) {
        StringBuilder builder = new StringBuilder();
        for (FieldError fieldError : fieldErrors) {
            if (!builder.isEmpty()) {
                builder.append("; ");
            }
            builder.append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage());
        }
        return builder.toString();
    }
}
