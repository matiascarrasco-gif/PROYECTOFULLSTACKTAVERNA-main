package cl.duoc.ep3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidoPaterno,
        @NotBlank @Size(max = 100) String apellidoMaterno,
        @NotBlank @Size(min = 8, max = 9) String rut,
        @NotBlank @Size(max = 30) String telefono,
        @NotBlank @Size(max = 300) String direccion
) {
}
