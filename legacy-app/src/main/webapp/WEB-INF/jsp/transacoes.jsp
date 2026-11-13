<%@ include file="include/topo.jsp" %>
<h2>Transações</h2>

<div class="totais">
    <c:forEach var="e" items="${totais}">
        <a class="total status-${e.key}" href="?status=${e.key}"><b>${e.value}</b> ${e.key}</a>
    </c:forEach>
</div>

<form class="filtro" method="get">
    Status
    <select name="status">
        <option value="">Todos</option>
        <c:forEach var="s" items="PENDENTE,AUTORIZADA,NEGADA,EM_REVISAO,CAPTURADA,CANCELADA,ESTORNADA,LIQUIDADA">
            <option value="${s}" ${s == statusSelecionado ? 'selected' : ''}>${s}</option>
        </c:forEach>
    </select>
    Lojista
    <select name="lojista">
        <option value="">Todos</option>
        <c:forEach var="l" items="${lojistas}">
            <option value="${l.id}" ${l.id == lojistaSelecionado ? 'selected' : ''}><c:out value="${l.nome}"/></option>
        </c:forEach>
    </select>
    <button type="submit">Filtrar</button>
    <span class="dica">Mostrando as 200 mais recentes</span>
</form>

<table>
    <tr>
        <th>ID</th><th>Data</th><th>Lojista</th><th>Pedido</th><th>Cartão</th><th>Bandeira</th>
        <th class="num">Valor</th><th>Status</th><th>Retorno</th><th class="num">Score</th>
    </tr>
    <c:forEach var="t" items="${transacoes}">
        <tr>
            <td><a href="${pageContext.request.contextPath}/transacoes/${t.id}">${t.id}</a></td>
            <td><fmt:formatDate value="${t.dataCriacao}" pattern="dd/MM/yyyy HH:mm:ss"/></td>
            <td><c:out value="${t.lojista.nome}"/></td>
            <td><c:out value="${t.pedidoId}"/></td>
            <td class="mono">${t.numeroCartao}</td>
            <td>${t.bandeira}</td>
            <td class="num"><fmt:formatNumber value="${t.valor}" type="currency" currencySymbol="R$ "/></td>
            <td><span class="status status-${t.status}">${t.status}</span></td>
            <td><c:out value="${t.codigoRetorno}"/></td>
            <td class="num">${t.scoreFraude}</td>
        </tr>
    </c:forEach>
</table>
<%@ include file="include/rodape.jsp" %>
