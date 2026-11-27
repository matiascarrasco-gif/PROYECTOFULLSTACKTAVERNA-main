package cl.duoc.ep3.dto;

import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.model.UserRole;

public record AccountResponse(
        Long id,
        String correo,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String apellidos,
        String rut,
        String telefono,
        String direccion,
        boolean isDuoc,
        int puntos,
        boolean isAdmin,
        UserRole role
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getNombres(),
                account.getApellidoPaterno(),
                account.getApellidoMaterno(),
                account.getApellidoPaterno() + " " + account.getApellidoMaterno(),
                account.getRut(),
                account.getPhone(),
                account.getAddress(),
                account.isDuoc(),
                account.getPuntos(),
                account.getRole() == UserRole.ADMIN,
                account.getRole());
    }
}
