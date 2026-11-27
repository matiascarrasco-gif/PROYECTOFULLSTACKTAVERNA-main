package cl.duoc.ep3.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// Intercepta excepciones de todos los controladores REST y devuelve respuestas HTTP uniformes.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Convierte respuestas 400, 404 y otros estados lanzados por el servicio.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getReason());
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }

    // Agrupa los errores producidos por validaciones @Valid o @NotBlank.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Los datos enviados no cumplen las reglas de validación.");
        problem.setTitle("Solicitud inválida");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    // Captura excepciones no previstas y evita exponer detalles internos al cliente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        String incidentId = UUID.randomUUID().toString();
        logger.error(
                "Error inesperado [{}] procesando {} {}",
                incidentId,
                request.getMethod(),
                request.getRequestURI(),
                exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno en el servidor. Referencia: " + incidentId);
        problem.setTitle("Error interno");
        problem.setProperty("incidentId", incidentId);
        return ResponseEntity.internalServerError().body(problem);
    }
}
