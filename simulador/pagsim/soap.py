"""Cliente minimo da API SOAP da AnyCompany, do jeito que um lojista chamaria."""
import socket
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape

from . import config

ENVELOPE = (
    '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" '
    'xmlns:pag="http://ws.anycompany.com.br/pagamento/v1">'
    "<soapenv:Body><pag:{op}><requisicao>{campos}</requisicao></pag:{op}></soapenv:Body>"
    "</soapenv:Envelope>"
)


class Timeout(Exception):
    """O lojista desistiu de esperar a resposta."""


class ErroRede(Exception):
    """Conexao recusada, resetada ou HTTP 5xx."""


def chamar(operacao, campos, timeout):
    corpo_campos = "".join(
        "<{0}>{1}</{0}>".format(nome, escape(str(valor))) for nome, valor in campos.items() if valor is not None
    )
    corpo = ENVELOPE.format(op=operacao, campos=corpo_campos).encode("utf-8")
    req = urllib.request.Request(
        config.URL_SOAP, data=corpo, headers={"Content-Type": "text/xml; charset=UTF-8"}, method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            conteudo = resp.read()
    except urllib.error.HTTPError as e:
        raise ErroRede("HTTP %d" % e.code)
    except urllib.error.URLError as e:
        if isinstance(e.reason, (socket.timeout, TimeoutError)):
            raise Timeout()
        raise ErroRede(str(e.reason))
    except (socket.timeout, TimeoutError):
        raise Timeout()
    except (ConnectionError, OSError) as e:
        raise ErroRede(str(e))

    raiz = ET.fromstring(conteudo)
    resposta = raiz.find(".//resposta")
    if resposta is None:
        raise ErroRede("resposta SOAP sem o elemento <resposta>")
    return {filho.tag: filho.text for filho in resposta}
