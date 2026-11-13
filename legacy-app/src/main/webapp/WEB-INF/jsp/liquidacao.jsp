<%@ include file="include/topo.jsp" %>
<h2>Liquidação</h2>
<p class="dica">O batch roda todo dia às 02:00 (horário do servidor). Use o botão para rodar agora.</p>

<form method="post" action="${pageContext.request.contextPath}/liquidacao/executar">
    <button type="submit">Executar liquidação agora</button>
</form>

<table>
    <tr>
        <th>ID</th><th>Data</th><th>Lojista</th><th class="num">Qtd</th><th class="num">Bruto</th>
        <th class="num">Taxa</th><th class="num">Líquido</th><th>Arquivo</th>
    </tr>
    <c:forEach var="l" items="${liquidacoes}">
        <tr>
            <td>${l.id}</td>
            <td><fmt:formatDate value="${l.dataCriacao}" pattern="dd/MM/yyyy HH:mm:ss"/></td>
            <td><c:out value="${l.lojista.nome}"/></td>
            <td class="num">${l.qtdTransacoes}</td>
            <td class="num"><fmt:formatNumber value="${l.valorBruto}" type="currency" currencySymbol="R$ "/></td>
            <td class="num"><fmt:formatNumber value="${l.valorTaxa}" type="currency" currencySymbol="R$ "/></td>
            <td class="num"><fmt:formatNumber value="${l.valorLiquido}" type="currency" currencySymbol="R$ "/></td>
            <td class="mono">${l.arquivo}</td>
        </tr>
    </c:forEach>
</table>
<%@ include file="include/rodape.jsp" %>
