package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;

@Entity
@Table(name = "cest")
public class Cest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7)
    private String codigo;

    @Column(nullable = false, length = 400)
    private String descricao;

    @Column(name = "ncm_prefixo", length = 8)
    private String ncmPrefixo;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }
    public String getNcmPrefixo() { return ncmPrefixo; }

    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setNcmPrefixo(String ncmPrefixo) { this.ncmPrefixo = ncmPrefixo; }
}
