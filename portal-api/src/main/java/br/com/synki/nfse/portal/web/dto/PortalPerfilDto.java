package br.com.synki.nfse.portal.web.dto;

import java.util.ArrayList;
import java.util.List;

public class PortalPerfilDto {
    public Long id;
    public String nome;
    public String descricao;
    public Boolean ativo;
    public List<Long> menuIds = new ArrayList<>();
    public Integer totalMenus;
}
