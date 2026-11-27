package cl.duoc.ep3.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidoPaterno;

    @Column(nullable = false, length = 100)
    private String apellidoMaterno;

    @Column(nullable = false, length = 9)
    private String rut;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false)
    private boolean duoc;

    @Column(nullable = false)
    private int puntos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.CUSTOMER;

    protected Account() {
    }

    public Account(String email, String passwordHash, String nombres, String apellidoPaterno,
                   String apellidoMaterno, String rut, LocalDate birthDate, String phone,
                   String address, boolean duoc, UserRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombres = nombres;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.rut = rut;
        this.birthDate = birthDate;
        this.phone = phone;
        this.address = address;
        this.duoc = duoc;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombres() { return nombres; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public String getRut() { return rut; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isDuoc() { return duoc; }
    public int getPuntos() { return puntos; }
    public UserRole getRole() { return role; }

    public void addPoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("La cantidad de puntos debe ser positiva.");
        }
        this.puntos = Math.addExact(this.puntos, amount);
    }

    public boolean redeemPoints(int amount) {
        if (amount <= 0 || this.puntos < amount) {
            return false;
        }
        this.puntos -= amount;
        return true;
    }

    public void updateProfile(String nombres, String apellidoPaterno, String apellidoMaterno,
                              String rut, String phone, String address) {
        this.nombres = nombres;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.rut = rut;
        this.phone = phone;
        this.address = address;
    }
}
