package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "conta_empresa")
@IdClass(ContaEmpresa.ContaEmpresaId.class)
public class ContaEmpresa {

    @Id
    @Column(name = "conta_id")
    private Long contaId;

    @Id
    @Column(name = "empresa_id")
    private Long empresaId;

    public static ContaEmpresa of(Long contaId, Long empresaId) {
        var ce = new ContaEmpresa();
        ce.contaId = contaId;
        ce.empresaId = empresaId;
        return ce;
    }

    public Long getContaId() { return contaId; }
    public Long getEmpresaId() { return empresaId; }

    public static class ContaEmpresaId implements Serializable {
        private Long contaId;
        private Long empresaId;

        public ContaEmpresaId() {}

        public ContaEmpresaId(Long contaId, Long empresaId) {
            this.contaId = contaId;
            this.empresaId = empresaId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ContaEmpresaId that)) return false;
            return contaId.equals(that.contaId) && empresaId.equals(that.empresaId);
        }

        @Override
        public int hashCode() {
            return contaId.hashCode() * 31 + empresaId.hashCode();
        }
    }
}
