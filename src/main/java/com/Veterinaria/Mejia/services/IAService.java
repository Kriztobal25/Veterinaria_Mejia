package com.Veterinaria.Mejia.services;

import org.springframework.stereotype.Service;

import com.Veterinaria.Mejia.dto.DiagnosticoDTO;
import com.Veterinaria.Mejia.dto.ResultadoDiagnosticoDTO;
import com.Veterinaria.Mejia.ia.MotorHeuristico;
import com.Veterinaria.Mejia.ia.ResultadoIA;

@Service
public class IAService {

    private final MotorHeuristico motor =
            new MotorHeuristico();

    public ResultadoDiagnosticoDTO analizar(
            DiagnosticoDTO dto) {

        ResultadoIA resultado =
                motor.evaluar(
                        dto.getTemperatura(),
                        dto.getSintomas()
                );

        ResultadoDiagnosticoDTO salida =
                new ResultadoDiagnosticoDTO();

        salida.setDiagnostico(
                resultado.getDiagnostico());

        salida.setPrioridad(
                resultado.getPrioridad());

        salida.setRecomendacion(
                resultado.getRecomendacion());

        return salida;
    }
}