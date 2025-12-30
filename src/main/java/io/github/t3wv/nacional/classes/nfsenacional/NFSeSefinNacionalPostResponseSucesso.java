package io.github.t3wv.nacional.classes.nfsenacional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NFSeSefinNacionalPostResponseSucesso {
    @JsonProperty("tipoAmbiente")
    private Integer tipoAmbiente;

    @JsonProperty("versaoAplicativo")
    private String versaoAplicativo;

    @JsonProperty("dataHoraProcessamento")
    private ZonedDateTime dataHoraProcessamento;

    @JsonProperty("idDps")
    private String idDps;

    @JsonProperty("chaveAcesso")
    private String chaveAcesso;

    @JsonProperty("nfseXmlGZipB64")
    private String nfseXmlGZipB64;

    @JsonProperty("eventos")
    private List<NFSeSefinNacionalEventoJson> eventos;

    @JsonProperty("alertas")
    private List<NFSeSefinNacionalPostMensagemProcessamento> alertas;

    public Integer getTipoAmbiente() {
        return tipoAmbiente;
    }

    public void setTipoAmbiente(Integer tipoAmbiente) {
        this.tipoAmbiente = tipoAmbiente;
    }

    public String getVersaoAplicativo() {
        return versaoAplicativo;
    }

    public void setVersaoAplicativo(String versaoAplicativo) {
        this.versaoAplicativo = versaoAplicativo;
    }

    public ZonedDateTime getDataHoraProcessamento() {
        return dataHoraProcessamento;
    }

    public void setDataHoraProcessamento(ZonedDateTime dataHoraProcessamento) {
        this.dataHoraProcessamento = dataHoraProcessamento;
    }

    public String getIdDps() {
        return idDps;
    }

    public void setIdDps(String idDps) {
        this.idDps = idDps;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public void setChaveAcesso(String chaveAcesso) {
        this.chaveAcesso = chaveAcesso;
    }

    public String getNfseXmlGZipB64() {
        return nfseXmlGZipB64;
    }

    public void setNfseXmlGZipB64(String nfseXmlGZipB64) {
        this.nfseXmlGZipB64 = nfseXmlGZipB64;
    }

    public List<NFSeSefinNacionalEventoJson> getEventos() {
        return eventos;
    }

    public NFSeSefinNacionalPostResponseSucesso setEventos(List<NFSeSefinNacionalEventoJson> eventos) {
        this.eventos = eventos;
        return this;
    }

    public List<NFSeSefinNacionalPostMensagemProcessamento> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<NFSeSefinNacionalPostMensagemProcessamento> alertas) {
        this.alertas = alertas;
    }

    @Override
    public String toString() {
        return "NFSeSefinNacionalPostResponseSucesso{" +
               "tipoAmbiente=" + tipoAmbiente +
               ", versaoAplicativo='" + versaoAplicativo + '\'' +
               ", dataHoraProcessamento=" + dataHoraProcessamento +
               ", idDps='" + idDps + '\'' +
               ", chaveAcesso='" + chaveAcesso + '\'' +
               ", nfseXmlGZipB64='" + nfseXmlGZipB64 + '\'' +
               ", eventos='" + eventos + '\'' +
               ", alertas=" + alertas +
               '}';
    }
}
