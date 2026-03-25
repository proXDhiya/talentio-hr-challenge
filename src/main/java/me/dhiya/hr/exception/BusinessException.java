package me.dhiya.hr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import me.dhiya.hr.dto.common.CustomFieldError;
import java.util.List;

public class BusinessException extends ResponseStatusException {

    private final List<CustomFieldError> fields;

    public BusinessException(HttpStatus status, String reason, List<CustomFieldError> fields) {
        super(status, reason);
        this.fields = fields;
    }

    public List<CustomFieldError> getFields() {
        return fields;
    }
}
