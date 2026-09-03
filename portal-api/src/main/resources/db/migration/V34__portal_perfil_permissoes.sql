-- Grupos de permissão (perfil) com menus liberados — estilo agrowFront /parametros/permissoes

CREATE TABLE portal_perfil (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conta_id BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500) NULL,
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pp_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    INDEX idx_pp_conta (conta_id),
    CONSTRAINT uk_pp_conta_nome UNIQUE (conta_id, nome)
);

CREATE TABLE portal_perfil_menu (
    perfil_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (perfil_id, menu_id),
    CONSTRAINT fk_ppm_perfil FOREIGN KEY (perfil_id) REFERENCES portal_perfil (id) ON DELETE CASCADE,
    CONSTRAINT fk_ppm_menu FOREIGN KEY (menu_id) REFERENCES portal_menu (id) ON DELETE CASCADE
);

ALTER TABLE usuario_empresa
    ADD COLUMN portal_perfil_id BIGINT NULL,
    ADD CONSTRAINT fk_ue_portal_perfil FOREIGN KEY (portal_perfil_id) REFERENCES portal_perfil (id) ON DELETE SET NULL;
