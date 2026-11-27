package cl.duoc.ep3.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidoPaterno,
        @NotBlank @Size(max = 100) String apellidoMaterno,
        @NotBlank @Size(min = 8, max = 9) String rut,
        @Past LocalDate birthDate,
        @NotBlank @Size(max = 30) String telefono,
        @NotBlank @Size(max = 300) String direccion,
        @Size(max = 80) String codigoReferido
) {
}
