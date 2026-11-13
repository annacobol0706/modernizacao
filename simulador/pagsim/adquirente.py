"""Muda o comportamento da adquirente simulada (WireMock) em tempo de execucao.

Os stubs criados aqui ficam so em memoria: reiniciar o container da adquirente
volta tudo ao normal.
"""
import json
import urllib.request

from . import config

METADADO = {"pagsim": "caos"}

RESPOSTA_APROVADA = (
    '<?xml version="1.0" encoding="UTF-8"?><respostaAdquirente><codigo>00</codigo>'
    "<mensagem>Transacao autorizada</mensagem>"
    "<codigoAutorizacao>{{randomValue length=6 type='NUMERIC'}}</codigoAutorizacao>"
    "<nsu>{{randomValue length=9 type='NUMERIC'}}</nsu></respostaAdquirente>"
)


def _stub(prioridade, resposta, padrao_cartao=None):
    req = {"method": "POST", "urlPath": "/adquirente/v1/autorizar"}
    if padrao_cartao:
        req["bodyPatterns"] = [
            {"matchesXPath": {"expression": "//numeroCartao/text()", "matches": padrao_cartao}}
        ]
    return {"priority": prioridade, "request": req, "response": resposta, "metadata": METADADO}


def _aprovada(**atraso):
    r = {
        "status": 200,
        "headers": {"Content-Type": "application/xml; charset=UTF-8"},
        "body": RESPOSTA_APROVADA,
        "transformers": ["response-template"],
    }
    r.update(atraso)
    return r


# Prioridades no WireMock: 1 vence tudo. Os cartoes de teste fixos usam 5 a 8 e a
# aprovacao padrao usa 10. Os stubs com prioridade 9 so mudam as aprovacoes comuns.
MODOS = {
    "normal": [],
    # Todas as aprovacoes ficam lentas (mediana de 2,5 s).
    "lenta": [
        _stub(9, _aprovada(delayDistribution={"type": "lognormal", "median": 2500, "sigma": 0.5})),
    ],
    # Como na Black Friday: ~3% das chamadas levam 25 s, ~5% falham com HTTP 503
    # e as aprovacoes ficam mais lentas que o normal.
    "instavel": [
        _stub(1, _aprovada(fixedDelayMilliseconds=25000), ".*[0-2]3$"),
        _stub(1, {"status": 503, "body": "Service Unavailable"}, ".*[0-4]7$"),
        _stub(9, _aprovada(delayDistribution={"type": "lognormal", "median": 600, "sigma": 0.5})),
    ],
    # Link com a adquirente fora do ar.
    "fora": [
        _stub(1, {"fault": "CONNECTION_RESET_BY_PEER"}),
    ],
}


def _post(caminho, corpo):
    req = urllib.request.Request(
        config.URL_ADQUIRENTE_ADMIN + caminho,
        data=json.dumps(corpo).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        return resp.read()


def definir_modo(modo):
    if modo not in MODOS:
        raise ValueError("modo invalido: %s (use %s)" % (modo, ", ".join(MODOS)))
    _post("/mappings/remove-by-metadata", {"matchesJsonPath": {"expression": "$.pagsim", "equalTo": "caos"}})
    for stub in MODOS[modo]:
        _post("/mappings", stub)
