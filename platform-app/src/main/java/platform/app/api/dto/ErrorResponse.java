package platform.app.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        String wagonCode,
        String date
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null, null);
    }

    public static ErrorResponse of(String error, String message, String wagonCode, String date) {
        return new ErrorResponse(error, message, wagonCode, date);
    }
}
