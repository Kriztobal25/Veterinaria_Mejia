package com.Veterinaria.Mejia.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Veterinaria.Mejia.models.Venta;
import com.Veterinaria.Mejia.repository.DetalleVentaRepository;
import com.Veterinaria.Mejia.repository.VentaRepository;

@Service
public class ReporteService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    // =================================================================
    // MOTOR PRINCIPAL QUE ALIMENTA AL CONTROLADOR
    // =================================================================
    public Map<String, Object> generarReporteDashboard(String rango) {
        Map<String, Object> reporte = new HashMap<>();
        LocalDateTime fechaInicio = calcularFechaInicio(rango);

        // Extraemos todas las ventas válidas de ese periodo para procesarlas en memoria
        List<Venta> ventasDelPeriodo = ventaRepository.findByFechaEmisionAfterAndEstado(fechaInicio, true);

        // 1. CÁLCULO DE MÉTRICAS FINANCIERAS (KPIs)
        BigDecimal ingresosVentas = calcularIngresosBrutos(ventasDelPeriodo);
        BigDecimal valorInvertido = calcularInversion(fechaInicio);
        BigDecimal perdidasTotales = calcularPerdidas(); // Si hay ventas anuladas o mermas
        BigDecimal gananciaNeta = ingresosVentas.subtract(valorInvertido).subtract(perdidasTotales);

        reporte.put("cantidadVentas", ventasDelPeriodo.size());
        reporte.put("ingresosVentas", ingresosVentas);
        reporte.put("valorInvertido", valorInvertido);
        reporte.put("perdidasTotales", perdidasTotales);
        reporte.put("gananciaNeta", gananciaNeta);

        // 2. TABLA: TOP 10 PRODUCTOS (Llama al query de tu DetalleVentaRepository)
        reporte.put("topProductos", detalleVentaRepository.findTop10ProductosVendidos(fechaInicio, null));

        // 3. GRÁFICOS DINÁMICOS SEGÚN LA OPCIÓN
        Map<String, BigDecimal> datosAgrupados = agruparVentasParaGrafico(ventasDelPeriodo, rango);
        reporte.put("labelsGrafico", new ArrayList<>(datosAgrupados.keySet())); // Eje X (Horas o Días)
        reporte.put("datosGrafico", new ArrayList<>(datosAgrupados.values()));  // Eje Y (Dinero S/)

        return reporte;
    }

    // =================================================================
    // LÓGICA DE FECHAS
    // =================================================================
    private LocalDateTime calcularFechaInicio(String rango) {
        LocalDateTime now = LocalDateTime.now();
        switch (rango) {
            case "semana": return now.minusDays(7).withHour(0).withMinute(0);
            case "mes": return now.minusMonths(1).withHour(0).withMinute(0);
            case "hoy": 
            default: return now.withHour(0).withMinute(0).withSecond(0);
        }
    }

    // =================================================================
    // CÁLCULOS MATEMÁTICOS FINANCIEROS
    // =================================================================
    private BigDecimal calcularIngresosBrutos(List<Venta> ventas) {
        return ventas.stream()
                .map(Venta::getTotalVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularInversion(LocalDateTime fechaInicio) {
        // Llama a tu base de datos para sumar el (Costo del Producto * Cantidad Vendida)
        BigDecimal inversion = detalleVentaRepository.calcularInversionDeVentasJPQL(fechaInicio);
        return inversion != null ? inversion : BigDecimal.ZERO;
    }

    private BigDecimal calcularPerdidas() {
        // Aquí puedes poner la lógica futura si tienes productos vencidos, 
        // o si tienes registro de "mermas". Por ahora iniciamos en 0.
        return BigDecimal.ZERO;
    }

    // =================================================================
    // PROCESAMIENTO INTELIGENTE DEL GRÁFICO (EJE X y Y)
    // =================================================================
    private Map<String, BigDecimal> agruparVentasParaGrafico(List<Venta> ventas, String rango) {
        // Usamos TreeMap para que las fechas/horas se ordenen solas de menor a mayor
        Map<String, BigDecimal> ventasAgrupadas = new TreeMap<>();
        
        // Si el filtro es "hoy", mostramos horas (Ej: 14:00). Si es semana/mes, mostramos días (Ej: 07/06)
        DateTimeFormatter formatoEjeX = rango.equals("hoy") ? 
                                        DateTimeFormatter.ofPattern("HH:00") : 
                                        DateTimeFormatter.ofPattern("dd/MM");

        for (Venta v : ventas) {
            String etiqueta = v.getFechaEmision().format(formatoEjeX);
            BigDecimal montoActual = ventasAgrupadas.getOrDefault(etiqueta, BigDecimal.ZERO);
            ventasAgrupadas.put(etiqueta, montoActual.add(v.getTotalVenta()));
        }

        return ventasAgrupadas;
    }
}