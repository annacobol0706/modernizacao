<%@ include file="include/topo.jsp" %>
<h2>Transação ${t.id} <span class="status status-${t.status}">${t.status}</span></h2>

<table class="detalhe">
    <tr><th>Lojista</th><td><c:out value="${t.lojista.nome}"/> (CNPJ ${t.lojista.cnpj})</td></tr>
    <tr><th>Pedido</th><td><c:out value="${t.pedidoId}"/></td></tr>
    <tr><th>Portador</th><td><c:out value="${t.nomePortador}"/></td></tr>
    <tr><th>Cartão</th><td class="mono">${t.numeroCartao} &nbsp; validade ${t.validade} &nbsp; CVV ${t.cvv}</td></tr>
    <tr><th>Bandeira</th><td>${t.bandeira}</td></tr>
    <tr><th>Valor</th><td><fmt:formatNumber value="${t.valor}" type="currency" currencySymbol="R$ "/> ${t.moeda}</td></tr>
    <tr><th>Valor capturado</th><td><fmt:formatNumber value="${t.valorCapturado}" type="currency" currencySymbol="R$ "/></td></tr>
    <tr><th>Retorno</th><td><c:out value="${t.codigoRetorno}"/> - <c:out value="${t.mensagemRetorno}"/></td></tr>
    <tr><th>Código de autorização</th><td>${t.codigoAutorizacao}</td></tr>
    <tr><th>NSU</th><td>${t.nsu}</td></tr>
    <tr><th>Liquidação</th><td>${t.liquidacaoId}</td></tr>
    <tr><th>Criada em</th><td><fmt:formatDate value="${t.dataCriacao}" pattern="dd/MM/yyyy HH:mm:ss"/></td></tr>
    <tr><th>Atualizada em</th><td><fmt:formatDate value="${t.dataAtualizacao}" pattern="dd/MM/yyyy HH:mm:ss"/></td></tr>
</table>

<h3>Análise de fraude</h3>
<c:choose>
    <c:when test="${empty analise}"><p>Sem análise registrada.</p></c:when>
    <c:otherwise>
        <table class="detalhe">
            <tr><th>Score</th><td>${analise.score}</td></tr>
            <tr><th>Decisão</th><td>${analise.decisao}</td></tr>
            <tr><th>Motivos</th><td><c:out value="${analise.motivos}"/></td></tr>
            <tr><th>Revisor</th><td><c:out value="${analise.revisor}"/>
                <fmt:formatDate value="${analise.dataRevisao}" pattern="dd/MM/yyyy HH:mm"/></td></tr>
        </table>
    </c:otherwise>
</c:choose>
<p><a href="${pageContext.request.contextPath}/transacoes">&larr; voltar</a></p>
<%@ include file="include/rodape.jsp" %>
