package io.github.t3wv.nfse.nacional.danfse;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formatação de valores exibidos no DANFSe.
 */
final class DanfseFormats {

    private static final DecimalFormat MONEY_FMT;

    static {
        final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        MONEY_FMT = new DecimalFormat("#,##0.00", symbols);
        MONEY_FMT.setParseBigDecimal(true);
    }

    private DanfseFormats() {}

    static String money(final String value) {
        if (DanfseDom.blank(value)) {
            return "0,00";
        }
        try {
            return MONEY_FMT.format(Double.parseDouble(value));
        } catch (final Exception ignored) {
            return value;
        }
    }

    static String pct(final String value) {
        if (DanfseDom.blank(value)) {
            return "-";
        }
        return value.replace('.', ',') + "%";
    }

    static String pctOrZero(final String value) {
        if (DanfseDom.blank(value)) {
            return "0,00%";
        }
        return pct(value);
    }

    static double asDouble(final String value) {
        if (DanfseDom.blank(value)) {
            return 0d;
        }
        try {
            return Double.parseDouble(value);
        } catch (final Exception ignored) {
            return 0d;
        }
    }

    static String moneySum(final String... values) {
        double total = 0d;
        for (final String value : values) {
            total += asDouble(value);
        }
        return money(Double.toString(total));
    }
}
