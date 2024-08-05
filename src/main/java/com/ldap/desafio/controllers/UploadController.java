package com.ldap.desafio.controllers;

import com.ldap.desafio.services.XMLProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final XMLProcessorService xmlProcessorService;

    @Autowired
    public UploadController(XMLProcessorService xmlProcessorService) {
        this.xmlProcessorService = xmlProcessorService;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        try {
            xmlProcessorService.processXML(file);
            return "Arquivo processado com sucesso.";
        } catch (Exception e) {
            return "Erro ao processar arquivo: " + e.getMessage();
        }
    }
}
