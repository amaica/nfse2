package io.github.t3wv.nacional.classes.nfsenacional;

import io.github.t3wv.utils.NFSePersister;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Persister;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class NFSeSefinNacionalInfoIBSCBSInfoValoresIBSCBS {

    @ElementList(name = "gReeRepRes", entry = "documentos", required = false)
    protected ArrayList<NFSeSefinNacionalInfoIBSCBSListaDoc> gReeRepRes;

    @Element(name = "trib")
    protected NFSeSefinNacionalInfoIBSCBSInfoTributos trib;

    public List<NFSeSefinNacionalInfoIBSCBSListaDoc> getGReeRepRes() {
        return gReeRepRes;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributos getTrib() {
        return trib;
    }

    public List<NFSeSefinNacionalInfoIBSCBSListaDoc> getgReeRepRes() {
        return gReeRepRes;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoValoresIBSCBS setgReeRepRes(ArrayList<NFSeSefinNacionalInfoIBSCBSListaDoc> gReeRepRes) {
        this.gReeRepRes = gReeRepRes;
        return this;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoValoresIBSCBS setTrib(NFSeSefinNacionalInfoIBSCBSInfoTributos trib) {
        this.trib = trib;
        return this;
    }

    @Override
    public String toString() {
        return "NFSeSefinNacionalInfoIBSCBSInfoValoresIBSCBS{" +
                "gReeRepRes=" + gReeRepRes +
                ", trib=" + trib +
                '}';
    }

    public String toXml() throws Exception {
        Persister serializer = new NFSePersister();
        StringWriter writer = new StringWriter();
        serializer.write(this, writer);
        return writer.toString();
    }
}

