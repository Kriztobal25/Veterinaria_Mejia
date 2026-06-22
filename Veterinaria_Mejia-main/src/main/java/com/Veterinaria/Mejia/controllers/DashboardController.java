package com.Veterinaria.Mejia.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.Veterinaria.Mejia.services.ReporteService;

@Controller
public class DashboardController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/dashboard")
    public String mostrarInicio(Model model) {
        
        // Extraemos las métricas del día usando el servicio que ya programamos
        Map<String, Object> metricasHoy = reporteService.generarReporteDashboard("hoy");
        
        // Enviamos a la vista solo lo que pediste: Cantidad vendida y Ganancia Neta
        model.addAttribute("cantidadVentas", metricasHoy.get("cantidadVentas"));
        model.addAttribute("gananciaNeta", metricasHoy.get("gananciaNeta"));
        
        return "reportes/dashboard";
    }
}