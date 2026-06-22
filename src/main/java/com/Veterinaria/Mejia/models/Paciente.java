package com.Veterinaria.Mejia.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "pacientes")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_historia", unique = true, length = 20)
    private String numeroHistoria;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "especie_id", nullable = false)
    private Especie especie;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String raza;

    @Column(name = "color_pelaje", length = 50)
    private String colorPelaje;

    @Column(unique = true, length = 50)
    private String microchip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SexoPaciente sexo;

    @Column(name = "fecha_nacimiento")
    @Temporal(TemporalType.DATE)
    private LocalDate fechaNacimiento;

    @Column(name = "peso_historico")
    private Double pesoHistorico;

    @Column(columnDefinition = "TEXT")
    private String alergias;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "ruta_foto", length = 255)
    private String rutaFoto;

    private Boolean esterilizado = false;

    @Column(length = 10)
    private String sangre;

    @Column(nullable = false)
    private Boolean estado = true;

    public enum SexoPaciente {
        Macho, Hembra
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNumeroHistoria() { return numeroHistoria; }
    public void setNumeroHistoria(String numeroHistoria) { this.numeroHistoria = numeroHistoria; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Especie getEspecie() { return especie; }
    public void setEspecie(Especie especie) { this.especie = especie; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public String getColorPelaje() { return colorPelaje; }
    public void setColorPelaje(String colorPelaje) { this.colorPelaje = colorPelaje; }

    public String getMicrochip() { return microchip; }
    public void setMicrochip(String microchip) { this.microchip = microchip; }

    public SexoPaciente getSexo() { return sexo; }
    public void setSexo(SexoPaciente sexo) { this.sexo = sexo; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public Double getPesoHistorico() { return pesoHistorico; }
    public void setPesoHistorico(Double pesoHistorico) { this.pesoHistorico = pesoHistorico; }

    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getRutaFoto() { return rutaFoto; }
    public void setRutaFoto(String rutaFoto) { this.rutaFoto = rutaFoto; }

    public Boolean getEsterilizado() { return esterilizado; }
    public void setEsterilizado(Boolean esterilizado) { this.esterilizado = esterilizado; }

    public String getSangre() { return sangre; }
    public void setSangre(String sangre) { this.sangre = sangre; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
}