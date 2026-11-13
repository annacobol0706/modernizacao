<%@ include file="include/topo.jsp" %>
<div class="login">
    <h2>Acesso ao backoffice</h2>
    <form method="post" action="${pageContext.request.contextPath}/login">
        <label>Usuário <input type="text" name="login" autofocus></label>
        <label>Senha <input type="password" name="senha"></label>
        <button type="submit">Entrar</button>
    </form>
</div>
<%@ include file="include/rodape.jsp" %>
