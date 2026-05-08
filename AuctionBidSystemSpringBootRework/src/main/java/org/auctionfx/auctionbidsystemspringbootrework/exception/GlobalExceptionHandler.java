package org.auctionfx.auctionbidsystemspringbootrework.exception;

import java.io.IOException;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // THÊM MỚI: Bắt lỗi validation từ @Size, @NotBlank...
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingValidation(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY; // Lỗi mặc định nếu key sai

        try {
            // Ánh xạ string "USERNAME_INVALID" sang Enum ErrorCode
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
            // Không làm gì, giữ nguyên INVALID_KEY
        }

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    // --- CÁC HÀM CŨ CỦA BẠN GIỮ NGUYÊN BÊN DƯỚI ---

    @ExceptionHandler(value = UserException.class)
    ResponseEntity<ApiResponse> handlingException(UserException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = IllegalStateException.class)
    ResponseEntity<ApiResponse> handlingException(IllegalStateException exception) {
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(101);
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handlingException(RuntimeException exception) {
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(89999);
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = IOException.class)
    ResponseEntity<ApiResponse> handlingIOException(IOException exception) {
        ApiResponse apiResponse = new ApiResponse();
        
        apiResponse.setCode(500); 
        apiResponse.setMessage(exception.getMessage());
        
        return ResponseEntity.status(500).body(apiResponse);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handlingException(Exception exception) {
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(99999);
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }
}