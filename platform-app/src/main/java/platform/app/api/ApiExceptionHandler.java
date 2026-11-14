package platform.app.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import platform.app.api.dto.ErrorResponse;
import platform.dislocation.InvalidDateException;
import platform.dislocation.ResultLimitExceededException;
import platform.fleet.InvalidWagonCodeException;
import platform.fleet.WagonNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WagonNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(WagonNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("WAGON_NOT_FOUND", ex.getMessage(), ex.wagonCode(), null));
    }

    @ExceptionHandler({
            InvalidWagonCodeException.class,
            platform.dislocation.InvalidWagonCodeException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidWagonCode(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_WAGON_CODE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidDateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDate(InvalidDateException ex) {
        String code = ex.getMessage() != null && ex.getMessage().contains("required")
                ? "MISSING_DATE"
                : "INVALID_DATE";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(code, ex.getMessage()));
    }

    @ExceptionHandler(ResultLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleLimit(ResultLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(
                        "RESULT_LIMIT_EXCEEDED",
                        ex.getMessage(),
                        ex.wagonCode(),
                        ex.date().toString()));
    }
}
