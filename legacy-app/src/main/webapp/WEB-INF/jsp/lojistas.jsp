<%@ include file="include/topo.jsp" %>
<h2>Lojistas</h2>
<table>
    <tr>
        <th>ID</th><th>Nome</th><th>CNPJ</th><th>API key</th><th class="num">MDR</th>
        <th class="num">Limite por transação</th><th>Ativo</th>
    </tr>
    <c:forEach var="l" items="${lojistas}">
        <tr>
            <td>${l.id}</td>
            <td><c:out value="${l.nome}"/></td>
            <td>${l.cnpj}</td>
            <td class="mono">${l.apiKey}</td>
            <td class="num">${l.taxaMdr}%</td>
            <td class="num"><fmt:formatNumber value="${l.limiteTransacao}" type="currency" currencySymbol="R$ "/></td>
            <td>${l.ativo ? 'Sim' : 'Não'}</td>
        </tr>
    </c:forEach>
</table>
<%@ include file="include/rodape.jsp" %>
