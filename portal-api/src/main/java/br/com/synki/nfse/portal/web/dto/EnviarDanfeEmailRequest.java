package br.com.synki.nfse.portal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EnviarDanfeEmailRequest(
        @NotBlank @Email String destinatario,
        String mensagem
) {}
