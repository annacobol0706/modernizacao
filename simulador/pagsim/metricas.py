"""Contadores do simulador e a linha de status impressa periodicamente."""
import threading
import time
from collections import Counter


def percentil(valores, p):
    if not valores:
        return None
    ordenados = sorted(valores)
    idx = min(len(ordenados) - 1, int(round(p / 100.0 * (len(ordenados) - 1))))
    return ordenados[idx]


class Metricas:
    def __init__(self):
        self._lock = threading.Lock()
        self.total = Counter()
        self._janela = Counter()
        self._latencias = []
        self._latencias_total = []
        self.em_andamento = 0

    def inc(self, nome, n=1):
        with self._lock:
            self.total[nome] += n
            self._janela[nome] += n

    def latencia(self, segundos):
        with self._lock:
            self._latencias.append(segundos)
            self._latencias_total.append(segundos)

    def entrou(self):
        with self._lock:
            self.em_andamento += 1

    def saiu(self):
        with self._lock:
            self.em_andamento -= 1

    def fechar_janela(self):
        with self._lock:
            janela, lat = self._janela, self._latencias
            self._janela, self._latencias = Counter(), []
            return janela, lat, self.em_andamento

    def resumo(self):
        with self._lock:
            return Counter(self.total), list(self._latencias_total)


def fmt_s(valor):
    return "-" if valor is None else "%.1fs" % valor


class Painel(threading.Thread):
    """Imprime uma linha de status a cada `intervalo` segundos."""

    def __init__(self, metricas, sonda=None, intervalo=10):
        super().__init__(daemon=True)
        self.m = metricas
        self.sonda = sonda
        self.intervalo = intervalo
        self.parar = threading.Event()

    def run(self):
        while not self.parar.wait(self.intervalo):
            self.imprimir()

    def imprimir(self):
        j, lat, andamento = self.m.fechar_janela()
        enviadas = j["compras"]
        linha = (
            "%s  compras/s %4.1f | aprov %3d  neg %3d  rev %3d  pend %2d  erro %2d | "
            "timeout %3d  retry %3d  desist %2d | p50 %5s  p95 %5s | em voo %3d"
            % (
                time.strftime("%H:%M:%S"),
                enviadas / float(self.intervalo),
                j["AUTORIZADA"] + j["CAPTURADA"],
                j["NEGADA"],
                j["EM_REVISAO"],
                j["PENDENTE"],
                j["ERRO"] + j["erro_rede"],
                j["timeouts"],
                j["retries"],
                j["desistencias"],
                fmt_s(percentil(lat, 50)),
                fmt_s(percentil(lat, 95)),
                andamento,
            )
        )
        if self.sonda is not None:
            linha += " | backoffice " + self.sonda.estado()
        print(linha, flush=True)
