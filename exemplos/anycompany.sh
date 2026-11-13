#!/bin/bash
# Cliente simples da API SOAP da AnyCompany (Mac, Linux, WSL ou Git Bash).
#
# Uso:
#   export ANYCOMPANY_API_KEY=<api key do lojista, veja no backoffice em Lojistas>
#   ./anycompany.sh autorizar <cartao> <valor> [captura: true|false]
#   ./anycompany.sh capturar  <transacaoId>
#   ./anycompany.sh cancelar  <transacaoId>
#   ./anycompany.sh consultar <transacaoId>
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
URL="${ANYCOMPANY_URL:-http://127.0.0.1:8080/anycompany/services/PagamentoService}"

uso() {
    sed -n '4,9p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
}

: "${ANYCOMPANY_API_KEY:?exporte ANYCOMPANY_API_KEY com a API key do lojista (backoffice > Lojistas)}"

OPERACAO="$1"
case "$OPERACAO" in
    autorizar)
        [ $# -ge 3 ] || uso
        CORPO=$(sed -e "s/{{API_KEY}}/$ANYCOMPANY_API_KEY/" \
                    -e "s/{{PEDIDO}}/PED-$(date +%s)/" \
                    -e "s/{{CARTAO}}/$2/" \
                    -e "s/{{VALOR}}/$3/" \
                    -e "s/{{CAPTURA}}/${4:-false}/" \
                    "$DIR/autorizar.xml")
        ;;
    capturar|cancelar|consultar)
        [ $# -ge 2 ] || uso
        CORPO=$(sed -e "s/{{OPERACAO}}/$OPERACAO/g" \
                    -e "s/{{API_KEY}}/$ANYCOMPANY_API_KEY/" \
                    -e "s/{{TRANSACAO_ID}}/$2/" \
                    "$DIR/operacao.xml")
        ;;
    *)
        uso
        ;;
esac

curl -s -H 'Content-Type: text/xml; charset=UTF-8' --data-binary "$CORPO" "$URL" | awk '
{
    i = index($0, "<resposta>"); j = index($0, "</resposta>")
    if (i && j) {
        s = substr($0, i + 10, j - i - 10)
        gsub(/<\/[A-Za-z]+>/, "\n", s)
        gsub(/<[A-Za-z]+>/, "&: ", s)
        gsub(/[<>]/, "", s)
        printf "%s", s
    } else {
        print
    }
}'
