<%@ include file="include/topo.jsp" %>
<h2>Fila de revisão manual</h2>
<p class="dica">Transações com score entre 50 e 79. Ao aprovar, a transação é enviada para a adquirente.</p>

<c:if test="${empty pendentes}"><p>Nenhuma transação aguardando revisão.</p></c:if>
<c:if test="${not empty pendentes}">
<table>
    <tr>
        <th>Transação</th><th>Data</th><th>Lojista</th><th>Cartão</th><th class="num">Valor</th>
        <th class="num">Score</th><th>Motivos</th><th></th>
    </tr>
    <c:forEach var="a" items="${pendentes}">
        <tr>
            <td><a href="${pageContext.request.contextPath}/transacoes/${a.transacao.id}">${a.transacao.id}</a></td>
            <td><fmt:formatDate value="${a.transacao.dataCriacao}" pattern="dd/MM/yyyy HH:mm"/></td>
            <td><c:out value="${a.transacao.lojista.nome}"/></td>
            <td class="mono">${a.transacao.numeroCartao}</td>
            <td class="num"><fmt:formatNumber value="${a.transacao.valor}" type="currency" currencySymbol="R$ "/></td>
            <td class="num">${a.score}</td>
            <td><c:out value="${a.motivos}"/></td>
            <td class="acoes">
                <form method="post" action="${pageContext.request.contextPath}/revisao/${a.transacao.id}/aprovar">
                    <button type="submit" class="aprovar">Aprovar</button>
                </form>
                <form method="post" action="${pageContext.request.contextPath}/revisao/${a.transacao.id}/negar">
                    <button type="submit" class="negar">Negar</button>
                </form>
            </td>
        </tr>
    </c:forEach>
</table>
</c:if>
<%@ include file="include/rodape.jsp" %>
