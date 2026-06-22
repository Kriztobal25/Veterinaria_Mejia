package com.Veterinaria.Mejia.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.Veterinaria.Mejia.dto.DiagnosticoDTO;
import com.Veterinaria.Mejia.dto.ResultadoDiagnosticoDTO;
import com.Veterinaria.Mejia.services.IAService;

@Controller
@RequestMapping("/ia")
public class IAController {

    @Autowired
    private IAService iaService;

    @GetMapping
    public String mostrarFormulario(Model model) {

        model.addAttribute(
                "diagnostico",
                new DiagnosticoDTO());

        return "ia/diagnostico";
    }

    @PostMapping("/analizar")
    public String analizar(
            @ModelAttribute DiagnosticoDTO dto,
            Model model) {

        ResultadoDiagnosticoDTO resultado =
                iaService.analizar(dto);

        model.addAttribute(
                "resultado",
                resultado);

        return "ia/diagnostico";
    }
}