package br.com.anycompany.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Garante que o usuario do backoffice esta logado (usuario guardado na sessao HTTP).
 */
public class LoginInterceptor extends HandlerInterceptorAdapter {

    public static final String USUARIO_SESSAO = "usuarioLogado";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (request.getSession().getAttribute(USUARIO_SESSAO) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        return true;
    }
}
