-- ACL: menus liberados por usuário em cada empresa (emitente)
CREATE TABLE portal_menu_acesso (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pma_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_pma_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    CONSTRAINT fk_pma_menu FOREIGN KEY (menu_id) REFERENCES portal_menu (id) ON DELETE CASCADE,
    CONSTRAINT uk_pma_user_emp_menu UNIQUE (usuario_id, empresa_id, menu_id),
    INDEX idx_pma_user_emp (usuario_id, empresa_id)
);
