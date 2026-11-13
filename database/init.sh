#!/bin/bash
# Cria o banco AnyCompany na primeira subida. Se o banco ja existir, nao faz nada.
set -e

: "${MSSQL_SA_PASSWORD:?defina MSSQL_SA_PASSWORD no .env}"
: "${BACKOFFICE_ADMIN_SENHA:?defina BACKOFFICE_ADMIN_SENHA no .env}"

if [ -x /opt/mssql-tools18/bin/sqlcmd ]; then
    SQLCMD="/opt/mssql-tools18/bin/sqlcmd -C"
else
    SQLCMD="/opt/mssql-tools/bin/sqlcmd"
fi

executar() {
    $SQLCMD -S sqlserver -U sa -P "$MSSQL_SA_PASSWORD" -b "$@"
}

echo "Aguardando o SQL Server..."
for i in $(seq 1 60); do
    if executar -Q "SELECT 1" > /dev/null 2>&1; then
        break
    fi
    sleep 3
done
executar -Q "SELECT 1" > /dev/null

EXISTE=$(executar -h -1 -W -Q "SET NOCOUNT ON; SELECT COUNT(*) FROM sys.databases WHERE name = 'AnyCompany'" | tr -d '[:space:]')
if [ "$EXISTE" = "1" ]; then
    echo "Banco AnyCompany ja existe. Nada a fazer."
    exit 0
fi

for script in /scripts/0*.sql; do
    echo "Executando $script"
    executar -i "$script" -v SenhaAdmin="$BACKOFFICE_ADMIN_SENHA"
done
echo "Banco AnyCompany criado com sucesso."
