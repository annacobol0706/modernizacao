package br.com.anycompany.util;

import java.util.Calendar;

/**
 * Utilitarios de cartao.
 *
 * @author rcosta
 * @since 1.0
 */
public class CartaoUtil {

    private CartaoUtil() {
    }

    public static boolean validarLuhn(String numero) {
        if (numero == null || !numero.matches("\\d{13,19}")) {
            return false;
        }
        int soma = 0;
        boolean dobrar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (dobrar) {
                d = d * 2;
                if (d > 9) {
                    d = d - 9;
                }
            }
            soma += d;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }

    public static String identificarBandeira(String numero) {
        if (numero == null || numero.length() < 6) {
            return "DESCONHECIDA";
        }
        // ELO precisa vir antes de VISA/MASTER (alguns BINs ELO comecam com 4 e 5)
        String[] binsElo = {"636368", "438935", "504175", "451416", "636297", "5067", "4576", "4011", "6504", "6509", "6516", "6550"};
        for (String bin : binsElo) {
            if (numero.startsWith(bin)) {
                return "ELO";
            }
        }
        if (numero.startsWith("34") || numero.startsWith("37")) {
            return "AMEX";
        }
        if (numero.startsWith("4")) {
            return "VISA";
        }
        int prefixo2 = Integer.parseInt(numero.substring(0, 2));
        int prefixo4 = Integer.parseInt(numero.substring(0, 4));
        if ((prefixo2 >= 51 && prefixo2 <= 55) || (prefixo4 >= 2221 && prefixo4 <= 2720)) {
            return "MASTERCARD";
        }
        return "DESCONHECIDA";
    }

    /**
     * Validade no formato MM/AA. O cartao vale ate o ultimo dia do mes.
     */
    public static boolean validadeExpirada(String validade) {
        int mes = Integer.parseInt(validade.substring(0, 2));
        int ano = 2000 + Integer.parseInt(validade.substring(3, 5));
        if (mes < 1 || mes > 12) {
            return true;
        }
        Calendar hoje = Calendar.getInstance();
        int anoAtual = hoje.get(Calendar.YEAR);
        int mesAtual = hoje.get(Calendar.MONTH) + 1;
        return ano < anoAtual || (ano == anoAtual && mes < mesAtual);
    }

    public static String mascarar(String numero) {
        if (numero == null || numero.length() < 10) {
            return numero;
        }
        StringBuilder sb = new StringBuilder(numero.substring(0, 6));
        for (int i = 6; i < numero.length() - 4; i++) {
            sb.append('*');
        }
        sb.append(numero.substring(numero.length() - 4));
        return sb.toString();
    }
}
