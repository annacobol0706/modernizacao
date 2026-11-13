"""Sonda que abre a tela de transacoes do backoffice, como um analista faria."""
import http.cookiejar
import threading
import time
import urllib.parse
import urllib.request

from . import config


class SondaBackoffice(threading.Thread):
    def __init__(self, intervalo=10, timeout=120):
        super().__init__(daemon=True)
        self.intervalo = intervalo
        self.timeout = timeout
        self.parar = threading.Event()
        self._lock = threading.Lock()
        self._inicio_requisicao = None
        self._ultimo = "-"
        self.pior = 0.0
        self.falhas = 0
        self.sessoes_perdidas = 0
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def _login(self):
        """Retorna False quando o login e aceito mas a pagina seguinte volta para o login."""
        dados = urllib.parse.urlencode({"login": "admin", "senha": config.BACKOFFICE_SENHA}).encode()
        with self.opener.open(config.URL_APP + "/login", data=dados, timeout=self.timeout) as resp:
            resp.read()
            return "/login" not in resp.geturl()

    def estado(self):
        with self._lock:
            if self._inicio_requisicao is not None:
                espera = time.monotonic() - self._inicio_requisicao
                if espera > 5:
                    return "esperando ha %.0fs" % espera
            return self._ultimo

    def run(self):
        logado = False
        while not self.parar.is_set():
            try:
                if not logado:
                    if not self._login():
                        with self._lock:
                            self.sessoes_perdidas += 1
                            self._ultimo = "SESSAO PERDIDA"
                        self.parar.wait(self.intervalo)
                        continue
                    logado = True
                with self._lock:
                    self._inicio_requisicao = time.monotonic()
                with self.opener.open(config.URL_APP + "/transacoes", timeout=self.timeout) as resp:
                    resp.read()
                    voltou_login = "/login" in resp.geturl()
                with self._lock:
                    duracao = time.monotonic() - self._inicio_requisicao
                    self._inicio_requisicao = None
                    self.pior = max(self.pior, duracao)
                    if voltou_login:
                        self.sessoes_perdidas += 1
                        self._ultimo = "SESSAO PERDIDA"
                        logado = False
                    else:
                        self._ultimo = "%.1fs" % duracao
            except Exception as e:  # noqa: BLE001 - a sonda nunca derruba o simulador
                with self._lock:
                    if self._inicio_requisicao is not None:
                        self.pior = max(self.pior, time.monotonic() - self._inicio_requisicao)
                        self.falhas += 1
                    self._inicio_requisicao = None
                    self._ultimo = "SEM RESPOSTA (%s)" % type(e).__name__
                logado = False
            self.parar.wait(self.intervalo)


def esperar_recuperacao(limite=600):
    """Abre a tela de transacoes ate ela responder em menos de 5 s. Retorna os segundos de espera."""
    inicio = time.monotonic()
    while time.monotonic() - inicio < limite:
        sonda = SondaBackoffice(timeout=30)
        t0 = time.monotonic()
        try:
            if sonda._login():
                with sonda.opener.open(config.URL_APP + "/transacoes", timeout=30) as resp:
                    resp.read()
                if time.monotonic() - t0 < 5:
                    return time.monotonic() - inicio
        except Exception:  # noqa: BLE001
            pass
        time.sleep(5)
    return None
