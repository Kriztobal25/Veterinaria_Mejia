package com.Veterinaria.Mejia.models;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especie_id")
    private Especie especie; // Nulo si es un producto general

    // Solo letras y espacios. Tamaño máximo 100.
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras")
    @Size(max = 100, message = "El nombre no debe superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    // Tipo de unidad para fraccionar (sacos, botellas, etc.)
    @NotBlank(message = "El tipo de unidad es obligatorio")
    @Pattern(regexp = "^(kg|unidad|blister|caja|litros)$", message = "La unidad debe ser válida (kg, unidad, caja, blister)")
    @Column(name = "tipo_unidad", nullable = false, length = 15)
    private String tipoUnidad;

    // Precio de inversión (No pasa de 999)
    @NotNull(message = "El precio de inversión es obligatorio")
    @DecimalMax(value = "999.00", message = "El precio de inversión no debe exceder 999.00")
    @Column(name = "precio_inversion", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioInversion;

    // Precio para el público
    @NotNull(message = "El precio de venta es obligatorio")
    @Column(name = "precio_venta_actual", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVentaActual;

    // Stock a ingresar (No pasa de 99)
    @DecimalMax(value = "99.00", message = "El stock a ingresar no puede ser mayor a 99")
    @Column(name = "stock_total", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal stockTotal = BigDecimal.ZERO;

    // Stock mínimo FIJO en 12
    @Column(name = "stock_minimo", precision = 10, scale = 2, updatable = false)
    @Builder.Default
    private final BigDecimal stockMinimo = new BigDecimal("12.00");

    // Siempre activo al crearse
    @Column(name = "estado", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean estado = true; 

    // ==========================================
    // ATRIBUTOS PARA FRACCIONAMIENTO / DOBLE STOCK
    // ==========================================

    @Column(name = "permite_fraccionamiento", columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean permiteFraccionamiento = false;

    // Representa los sacos/envases sellados enteros
    @Column(name = "stock_cerrado")
    @Builder.Default
    private Integer stockCerrado = 0;

    // Representa los kilos/gramos sueltos disponibles para la venta
    @Column(name = "stock_abierto", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal stockAbierto = BigDecimal.ZERO;

    // Cuántos kilos trae el saco original
    @Column(name = "contenido_por_envase", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal contenidoPorEnvase = BigDecimal.ZERO;

    // Precio manual que el Administrador define para cobrar por kilo/gramo
    @Column(name = "precio_por_fraccion", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal precioPorFraccion = BigDecimal.ZERO;

    // Método transitorio para saber "cuánto se está invirtiendo" en total de este producto
    public BigDecimal calcularInversionTotal() {
        if (stockTotal == null || precioInversion == null) return BigDecimal.ZERO;
        return stockTotal.multiply(precioInversion);
    }
}