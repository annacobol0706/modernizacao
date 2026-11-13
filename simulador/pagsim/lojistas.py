"""Comportamento dos lojistas: valores, captura, cancelamento e politica de retry.

A politica de retry segue o Manual de Integracao v2.3: em caso de timeout, o lojista
reenvia a mesma requisicao (mesmo pedidoId).
"""
import math
import random
import threading
import time
import uuid
from dataclasses import dataclass

from . import soap


@dataclass
class Perfil:
    cnpj: str
    nome: str
    peso: float              # participacao no volume de compras
    valor_mediano: float     # em reais
    valor_sigma: float       # dispersao (lognormal)
    valor_maximo: float
    captura_automatica: bool
    atraso_captura: tuple    # segundos (min, max) ate capturar, quando nao e automatica
    prob_cancelar: float     # cancela antes de capturar
    prob_estorno: float      # estorna depois de capturar
    timeout: float           # segundos que a integracao do lojista espera
    tentativas: int          # 1 = nao reenvia
    acompanha_revisao: bool  # consulta a transacao enquanto esta em revisao


PERFIS = [
    # Integracao de PDV (maquininha via TEF). Timeout curto e duas retentativas.
    Perfil("23456789000101", "Padaria", 0.55, 24, 0.6, 450, True, None, 0.0, 0.01, 5, 3, False),
    # Plugin de e-commerce. Captura quando o pedido e separado no estoque.
    Perfil("12345678000190", "Eletronicos", 0.35, 380, 0.9, 7000, False, (3, 20), 0.04, 0.01, 8, 2, False),
    # Agencia de viagens. Espera mais, nao reenvia e acompanha as transacoes em revisao.
    Perfil("34567890000112", "Viagens", 0.10, 2200, 0.8, 26000, False, (10, 60), 0.06, 0.0, 20, 1, True),
]


def sortear_perfil():
    return random.choices(PERFIS, [p.peso for p in PERFIS])[0]


def sortear_valor(perfil):
    v = math.exp(random.gauss(math.log(perfil.valor_mediano), perfil.valor_sigma))
    v = min(max(v, 3.0), perfil.valor_maximo)
    return "%.2f" % v


class Agenda(threading.Thread):
    """Executa acoes futuras dos lojistas (captura, cancelamento, consulta)."""

    def __init__(self, executor):
        super().__init__(daemon=True)
        self.executor = executor
        self._itens = []
        self._cond = threading.Condition()
        self.ativo = True

    def agendar(self, atraso, fn, *args):
        with self._cond:
            self._itens.append((time.monotonic() + atraso, fn, args))
            self._cond.notify()

    def pendentes(self):
        with self._cond:
            return len(self._itens)

    def run(self):
        while True:
            with self._cond:
                agora = time.monotonic()
                prontos = [i for i in self._itens if i[0] <= agora]
                self._itens = [i for i in self._itens if i[0] > agora]
                if not prontos:
                    self._cond.wait(0.5)
                    continue
            if not self.ativo:
                continue
            for _, fn, args in prontos:
                try:
                    self.executor.submit(fn, *args)
                except RuntimeError:
                    return


class Lojistas:
    """Simula as integracoes dos lojistas chamando o gateway."""

    def __init__(self, api_keys, carteira, metricas, executor):
        self.api_keys = api_keys
        self.carteira = carteira
        self.m = metricas
        self.executor = executor
        self.agenda = Agenda(executor)
        self.agenda.start()

    # ---------- chamadas com a politica de retry do lojista ----------

    def _chamar(self, perfil, operacao, campos, medir=True):
        for tentativa in range(1, perfil.tentativas + 1):
            inicio = time.monotonic()
            try:
                resp = soap.chamar(operacao, campos, perfil.timeout)
                if medir:
                    self.m.latencia(time.monotonic() - inicio)
                return resp
            except soap.Timeout:
                self.m.inc("timeouts")
            except soap.ErroRede:
                self.m.inc("erro_rede")
            if tentativa < perfil.tentativas:
                self.m.inc("retries")
                time.sleep(1)
        self.m.inc("desistencias")
        return None

    # ---------- fluxos ----------

    def compra(self, perfil, cartao=None, valor=None):
        self.m.entrou()
        try:
            self._compra(perfil, cartao, valor)
        finally:
            self.m.saiu()

    def _compra(self, perfil, cartao, valor):
        api_key = self.api_keys[perfil.cnpj]
        pedido = "%s-%s" % (perfil.nome[:3].upper(), uuid.uuid4().hex[:10].upper())
        campos = {
            "apiKey": api_key,
            "pedidoId": pedido,
            "numeroCartao": cartao or self.carteira.sortear(),
            "nomePortador": "CONSUMIDOR SIMULADO",
            "validade": "12/30",
            "cvv": "%03d" % random.randint(0, 999),
            "valor": valor or sortear_valor(perfil),
            "moeda": "BRL",
            "capturaAutomatica": "true" if perfil.captura_automatica else "false",
        }
        self.m.inc("compras")
        resp = self._chamar(perfil, "autorizar", campos)
        if resp is None:
            return
        status = resp.get("status")
        self.m.inc(status or "ERRO")

        transacao = resp.get("transacaoId")
        if status == "AUTORIZADA" and not perfil.captura_automatica:
            if random.random() < perfil.prob_cancelar:
                self.agenda.agendar(random.uniform(*perfil.atraso_captura), self.cancelar, perfil, transacao)
            else:
                self.agenda.agendar(random.uniform(*perfil.atraso_captura), self.capturar, perfil, transacao)
        elif status == "CAPTURADA" and random.random() < perfil.prob_estorno:
            self.agenda.agendar(random.uniform(30, 120), self.cancelar, perfil, transacao)
        elif status == "EM_REVISAO" and perfil.acompanha_revisao:
            self.agenda.agendar(30, self.acompanhar_revisao, perfil, transacao, 1)

    def capturar(self, perfil, transacao):
        resp = self._chamar(perfil, "capturar", {"apiKey": self.api_keys[perfil.cnpj], "transacaoId": transacao},
                            medir=False)
        if resp and resp.get("status") == "CAPTURADA":
            self.m.inc("capturas")
            if random.random() < perfil.prob_estorno:
                self.agenda.agendar(random.uniform(30, 120), self.cancelar, perfil, transacao)

    def cancelar(self, perfil, transacao):
        resp = self._chamar(perfil, "cancelar", {"apiKey": self.api_keys[perfil.cnpj], "transacaoId": transacao},
                            medir=False)
        if resp and resp.get("status") in ("CANCELADA", "ESTORNADA"):
            self.m.inc("cancelamentos")

    def acompanhar_revisao(self, perfil, transacao, vez):
        resp = self._chamar(perfil, "consultar", {"apiKey": self.api_keys[perfil.cnpj], "transacaoId": transacao},
                            medir=False)
        if resp is None:
            return
        status = resp.get("status")
        if status == "AUTORIZADA":
            self.agenda.agendar(random.uniform(*perfil.atraso_captura), self.capturar, perfil, transacao)
        elif status == "EM_REVISAO" and vez < 10:
            self.agenda.agendar(30, self.acompanhar_revisao, perfil, transacao, vez + 1)

    def teste_de_cartao(self):
        """Fraudador testando um cartao roubado: varias compras pequenas em sequencia."""
        eletronicos = PERFIS[1]
        cartao = self.carteira.novo()
        self.m.inc("ataques")
        for _ in range(8):
            self.executor.submit(self.compra, eletronicos, cartao, "%.2f" % random.uniform(1, 15))
            time.sleep(random.uniform(2, 4))
