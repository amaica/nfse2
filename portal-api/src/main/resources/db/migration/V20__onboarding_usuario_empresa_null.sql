-- Permite usuário de conta SaaS antes da primeira empresa (onboarding)
ALTER TABLE usuario MODIFY empresa_id BIGINT NULL;
