package br.com.synki.nfse.portal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarContaRequest(
        @NotBlank @Size(min = 2, max = 120) String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 72) String senha,
        @NotBlank @Size(min = 2, max = 120) String nomeConta
) {}
