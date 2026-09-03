package br.com.synki.nfse.portal.web.dto;

import java.util.ArrayList;
import java.util.List;

public class MenuDto {

    public Long id;
    public String label;
    public String icon;
    public Integer ordemMenu;
    public String outcome;
    public String operadorTemAcesso;
    public Boolean ativo;
    public MenuRefDto parent;
    public List<SubMenuDto> submenus = new ArrayList<>();

    public static class MenuRefDto {
        public Long id;
        public String label;

        public MenuRefDto() {}

        public MenuRefDto(Long id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    public static class SubMenuDto {
        public Long id;
        public String label;
        public String icon;
        public String outcome;
    }
}
