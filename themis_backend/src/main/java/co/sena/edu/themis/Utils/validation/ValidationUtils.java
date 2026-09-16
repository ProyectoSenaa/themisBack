package co.sena.edu.themis.Utils.validation;

import co.sena.edu.themis.Utils.Exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ValidationUtils {
    public <T> T validateNotNull(T value, String message) {
        if (value == null) {
            throw new CustomException("Bad Request", message, HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    public void validateExists(Object entity, String message) {
        if (entity == null) {
            throw new CustomException("Not Found", message, HttpStatus.NOT_FOUND);
        }
    }

    public <T> T tryExecute(Supplier<T> operation, String contextMessage) {
        try {
            return operation.get();
        } catch (Exception ex) {
            throw new CustomException("Error", contextMessage + ": " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void tryExecute(Runnable operation, String contextMessage) {
        try {
            operation.run();
        } catch (Exception ex) {
            throw new CustomException("Error", contextMessage + ": " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

