"""Configuracao do simulador, lida de variaveis de ambiente."""
import os

URL_APP = os.environ.get("ANYCOMPANY_URL", "http://app:8080/anycompany").rstrip("/")
URL_SOAP = URL_APP + "/services/PagamentoService"
URL_ADQUIRENTE_ADMIN = os.environ.get("ADQUIRENTE_ADMIN_URL", "http://adquirente:8080/__admin").rstrip("/")

DB_HOST = os.environ.get("DB_HOST", "sqlserver")
DB_PORTA = int(os.environ.get("DB_PORTA", "1433"))
DB_SENHA = os.environ.get("MSSQL_SA_PASSWORD")

BACKOFFICE_SENHA = os.environ.get("BACKOFFICE_ADMIN_SENHA")
