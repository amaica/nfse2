package io.github.t3wv.nfse.nacional.danfse;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Acesso mínimo a elementos XML da NFS-e nacional (namespace-agnóstico).
 */
final class DanfseDom {

    private DanfseDom() {}

    static Element first(final Element parent, final String local) {
        if (parent == null) {
            return null;
        }
        NodeList list = parent.getElementsByTagNameNS("*", local);
        if (list.getLength() == 0) {
            list = parent.getElementsByTagName(local);
        }
        if (list.getLength() == 0) {
            return null;
        }
        final Node node = list.item(0);
        return node instanceof Element element ? element : null;
    }

    static String text(final Element parent, final String local) {
        final Element element = first(parent, local);
        if (element == null) {
            return null;
        }
        final String value = element.getTextContent();
        return value == null ? null : value.trim();
    }

    static String firstText(final Element parent, final String... locals) {
        for (final String local : locals) {
            final String value = text(parent, local);
            if (!blank(value)) {
                return value;
            }
        }
        return null;
    }

    static boolean blank(final String value) {
        return value == null || value.isBlank();
    }

    static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }

    static String nullToDash(final String value) {
        return blank(value) ? "-" : value;
    }

    static String join(final String separator, final String... parts) {
        final StringBuilder builder = new StringBuilder();
        for (final String part : parts) {
            if (blank(part)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(separator);
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }
}
