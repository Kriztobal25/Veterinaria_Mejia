package com.Veterinaria.Mejia.ia;

import java.util.ArrayList;
import java.util.List;

public class MotorHeuristico {

    private List<ReglaDiagnostico> reglas;

    public MotorHeuristico() {

        reglas = new ArrayList<>();

        reglas.add(
                new ReglaDiagnostico(
                        "Posible Parvovirus",
                        "diarrea",
                        39.0,
                        10,
                        "Hospitalización inmediata"
                )
        );

        reglas.add(
                new ReglaDiagnostico(
                        "Posible Moquillo",
                        "tos",
                        39.5,
                        9,
                        "Evaluación urgente"
                )
        );

        reglas.add(
                new ReglaDiagnostico(
                        "Lesión ortopédica",
                        "cojera",
                        37.0,
                        6,
                        "Radiografía"
                )
        );

        reglas.add(
                new ReglaDiagnostico(
                        "Problema digestivo",
                        "vomito",
                        38.0,
                        5,
                        "Observación clínica"
                )
        );
    }

    public ResultadoIA evaluar(
            Double temperatura,
            String sintomas) {

        if (sintomas == null) {
            sintomas = "";
        }

        sintomas = sintomas.toLowerCase();

        for (ReglaDiagnostico regla : reglas) {

            if (sintomas.contains(regla.getSintomaClave())
                    && temperatura >= regla.getTemperaturaMinima()) {

                return new ResultadoIA(
                        regla.getEnfermedad(),
                        regla.getPrioridad(),
                        regla.getRecomendacion()
                );
            }
        }

        return new ResultadoIA(
                "No se encontró coincidencia",
                1,
                "Consultar al veterinario"
        );
    }
}