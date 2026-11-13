package br.com.anycompany.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CartaoUtilTest {

    @Test
    public void luhnValido() {
        assertTrue(CartaoUtil.validarLuhn("4111111111111111"));
    }

    @Test
    public void luhnInvalido() {
        assertFalse(CartaoUtil.validarLuhn("4111111111111112"));
    }

    @Test
    public void bandeiraVisa() {
        assertEquals("VISA", CartaoUtil.identificarBandeira("4111111111111111"));
    }
}
