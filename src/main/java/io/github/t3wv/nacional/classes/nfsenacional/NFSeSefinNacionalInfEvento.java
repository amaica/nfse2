
package io.github.t3wv.nacional.classes.nfsenacional;

import org.simpleframework.xml.*;

import java.time.ZonedDateTime;

@Root(name = "infEvento")
public class NFSeSefinNacionalInfEvento {

    @Element(name = "verAplic", required = false)
    protected String versaoApp;
    @Element(name = "ambGer")
    protected NFSeSefinNacionalInfEventoAmbienteGeracao ambienteGeracao;
    @Element(name = "nSeqEvento")
    protected String numeroSequencialEvento;
    @Element(name = "dhProc")
    protected ZonedDateTime dataHoraProcessamento;
    @Element(name = "nDFe")
    protected String numeroDFe;
    @Element(name = "pedRegEvento")
    protected NFSeSefinNacionalPedRegEvt pedidoRegistroEvento;
    @Attribute(name = "Id")
    protected String id;

    public String getVersaoApp() {
        return versaoApp;
    }

    public void setVersaoApp(String value) {
        this.versaoApp = value;
    }

    public NFSeSefinNacionalInfEventoAmbienteGeracao getAmbienteGeracao() {
        return ambienteGeracao;
    }

    public void setAmbienteGeracao(NFSeSefinNacionalInfEventoAmbienteGeracao value) {
        this.ambienteGeracao = value;
    }

    public String getNumeroSequencialEvento() {
        return numeroSequencialEvento;
    }

    public void setNumeroSequencialEvento(String numeroSequencialEvento) {
        this.numeroSequencialEvento = numeroSequencialEvento;
    }

    public ZonedDateTime getDataHoraProcessamento() {
        return dataHoraProcessamento;
    }

    public void setDataHoraProcessamento(ZonedDateTime value) {
        this.dataHoraProcessamento = value;
    }

    public String getNumeroDFe() {
        return numeroDFe;
    }

    public void setNumeroDFe(String numeroDFe) {
        this.numeroDFe = numeroDFe;
    }

    public NFSeSefinNacionalPedRegEvt getPedidoRegistroEvento() {
        return pedidoRegistroEvento;
    }

    public void setPedidoRegistroEvento(NFSeSefinNacionalPedRegEvt pedidoRegistroEvento) {
        this.pedidoRegistroEvento = pedidoRegistroEvento;
    }

    public String getId() {
        return id;
    }

    /**
     * Identificador do evento: "EVT" + Chave de acesso(50) Tipo do evento (6) + Pedido de Registro do Evento(3) (nPedRegEvento)
     * @param value
     */
    public void setId(String value) {
        this.id = value;
    }

}
