package org.auctionfx.auctionbidsystemspringbootrework.exception;

import org.auctionfx.auctionbidsystemspringbootrework.dto.response.NotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = UserException.class)
    ResponseEntity<NotificationResponse.ApiResponse> handlingException(UserException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        NotificationResponse.ApiResponse apiResponse = new NotificationResponse.ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = PaymentException.class)
    ResponseEntity<NotificationResponse.ApiResponse> handlingException(PaymentException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        NotificationResponse.ApiResponse apiResponse = new NotificationResponse.ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = NotificationException.class)
    ResponseEntity<NotificationResponse.ApiResponse> handlingException(NotificationException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        NotificationResponse.ApiResponse apiResponse = new NotificationResponse.ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = ItemException.class)
    ResponseEntity<NotificationResponse.ApiResponse> handlingException(ItemException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        NotificationResponse.ApiResponse apiResponse = new NotificationResponse.ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AuctionException.class)
    ResponseEntity<NotificationResponse.ApiResponse> handlingException(AuctionException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        NotificationResponse.ApiResponse apiResponse = new NotificationResponse.ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = IllegalStateException.class)
    ResponseEntity<NotificationResponse.ApiResponse> handlingException(IllegalStateException exception) {
        NotificationResponse.ApiResponse apiResponse = new NotificationResponse.ApiResponse();

        apiResponse.setCode(101);
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<NotificationResponse.ApiResponse> handlingException(RuntimeException exception) {
        NotificationResponse.ApiResponse apiResponse = new NotificationResponse.ApiResponse();

        apiResponse.setCode(89999);
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<NotificationResponse.ApiResponse> handlingException(Exception exception) {
        NotificationResponse.ApiResponse apiResponse = new NotificationResponse.ApiResponse();

        apiResponse.setCode(99999);
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }
}
