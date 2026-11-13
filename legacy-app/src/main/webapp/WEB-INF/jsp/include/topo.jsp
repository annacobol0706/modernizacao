<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="pt_BR"/>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>AnyCompany - Backoffice</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/anycompany.css">
</head>
<body>
<div class="topo">
    <span class="logo">AnyCompany <small>Backoffice v2.7</small></span>
    <c:if test="${not empty sessionScope.usuarioLogado}">
        <a href="${pageContext.request.contextPath}/transacoes">Transações</a>
        <a href="${pageContext.request.contextPath}/revisao">Revisão de fraude</a>
        <a href="${pageContext.request.contextPath}/antifraude">Regras antifraude</a>
        <a href="${pageContext.request.contextPath}/liquidacao">Liquidação</a>
        <a href="${pageContext.request.contextPath}/lojistas">Lojistas</a>
        <span class="usuario"><c:out value="${sessionScope.usuarioLogado.nome}"/> |
            <a href="${pageContext.request.contextPath}/logout">Sair</a></span>
    </c:if>
</div>
<div class="conteudo">
<c:if test="${not empty mensagem}"><div class="msg ok"><c:out value="${mensagem}"/></div></c:if>
<c:if test="${not empty erro}"><div class="msg erro"><c:out value="${erro}"/></div></c:if>
