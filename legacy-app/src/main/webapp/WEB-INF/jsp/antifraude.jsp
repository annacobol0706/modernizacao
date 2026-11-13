<%@ include file="include/topo.jsp" %>
<h2>Regras antifraude</h2>

<div class="caixa">
<h3>Pontuação</h3>
<table class="detalhe">
    <tr><th>Cartão na lista de bloqueio</th><td>100 (nega direto)</td></tr>
    <tr><th>Valor acima do limite do lojista</th><td>+40</td></tr>
    <tr><th>Horário entre 00h e 06h</th><td>+15</td></tr>
    <tr><th>Mais de 5 transações do cartão em 10 minutos</th><td>+50 (procedure)</td></tr>
    <tr><th>BIN de risco</th><td>+pontuação do BIN (procedure)</td></tr>
    <tr><th>Valor 3x acima da média histórica do cartão</th><td>+20 (procedure)</td></tr>
    <tr><th>Decisão</th><td>&ge; 80 nega, &ge; 50 revisão manual, abaixo aprova</td></tr>
</table>
</div>

<h3>Lista de bloqueio</h3>
<form class="filtro" method="post" action="${pageContext.request.contextPath}/antifraude/bloqueio">
    Cartão <input type="text" name="numeroCartao" size="20">
    Motivo <input type="text" name="motivo" size="40">
    <button type="submit">Bloquear</button>
</form>
<table>
    <tr><th>ID</th><th>Cartão</th><th>Motivo</th><th>Incluído em</th><th></th></tr>
    <c:forEach var="b" items="${bloqueios}">
        <tr>
            <td>${b.id}</td>
            <td class="mono">${b.numeroCartao}</td>
            <td><c:out value="${b.motivo}"/></td>
            <td><fmt:formatDate value="${b.dataInclusao}" pattern="dd/MM/yyyy HH:mm"/></td>
            <td><form method="post" action="${pageContext.request.contextPath}/antifraude/bloqueio/${b.id}/remover">
                <button type="submit" class="negar">Remover</button></form></td>
        </tr>
    </c:forEach>
</table>

<h3>BINs de risco</h3>
<p class="dica">Mantido pelo time de risco direto no banco (tabela dbo.BinRisco).</p>
<table>
    <tr><th>BIN</th><th>País</th><th class="num">Pontuação</th><th>Descrição</th></tr>
    <c:forEach var="bin" items="${bins}">
        <tr>
            <td class="mono">${bin.Bin}</td>
            <td>${bin.Pais}</td>
            <td class="num">${bin.Pontuacao}</td>
            <td><c:out value="${bin.Descricao}"/></td>
        </tr>
    </c:forEach>
</table>
<%@ include file="include/rodape.jsp" %>
