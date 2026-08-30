-- Hibernate valida VARCHAR(7); migration V19 criou CHAR(7)
ALTER TABLE uso_mensal MODIFY COLUMN ano_mes VARCHAR(7) NOT NULL;
