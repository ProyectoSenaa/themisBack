package co.sena.edu.themis.Utils.Exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException{

    private final HttpStatus httpStatus;
    private final String title;

    public CustomException(String title, String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.title = title;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getTitle() {
        return title;
    }
}
