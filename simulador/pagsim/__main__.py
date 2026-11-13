"""Simulador de uso do gateway da AnyCompany.

Comandos:
  dia-normal     [--tps 1.5] [--minutos N]   trafego de um dia comum (ate Ctrl+C)
  black-friday   [--tps-pico 12] [--fator-tempo 1.0]
                                             aquecimento, pico com adquirente instavel e recuperacao
  adquirente     normal|lenta|instavel|fora  muda o comportamento da adquirente simulada
  sintomas                                   le o banco e mostra os sintomas acumulados
"""
import argparse
import random
import sys
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

from . import adquirente, banco, config
from .backoffice import SondaBackoffice, esperar_recuperacao
from .cartoes import Carteira
from .lojistas import Lojistas, sortear_perfil
from .metricas import Metricas, Painel, fmt_s, percentil

# Fases da Black Friday: (segundos, tps inicial, tps final, modo da adquirente, descricao)
FASES_BLACK_FRIDAY = [
    (60, 0.25, 0.25, "normal", "aquecimento"),
    (90, 0.25, 1.0, "normal", "rampa de acesso"),
    (240, 1.0, 1.0, "instavel", "pico, adquirente instavel"),
    (60, 1.0, 0.25, "normal", "fim do pico, adquirente normalizada"),
    (90, 0.25, 0.25, "normal", "recuperacao"),
]


def aguardar_gateway(limite=300):
    print("Aguardando o gateway em %s ..." % config.URL_APP, flush=True)
    fim = time.monotonic() + limite
    while time.monotonic() < fim:
        try:
            with urllib.request.urlopen(config.URL_SOAP + "?wsdl", timeout=5) as r:
                if r.status == 200:
                    return
        except Exception:  # noqa: BLE001
            pass
        time.sleep(3)
    raise SystemExit("O gateway nao respondeu em %d s." % limite)


class Simulacao:
    def __init__(self):
        aguardar_gateway()
        self.metricas = Metricas()
        self.executor = ThreadPoolExecutor(max_workers=500)
        self.lojistas = Lojistas(banco.api_keys(), Carteira(seed=42), self.metricas, self.executor)
        self.sonda = None
        if config.BACKOFFICE_SENHA:
            self.sonda = SondaBackoffice()
            self.sonda.start()
        self.painel = Painel(self.metricas, self.sonda)
        self.parar = threading.Event()
        self.proximo_ataque = time.monotonic() + random.uniform(120, 300)

    def gerar(self, duracao, tps_inicio, tps_fim, ataques=True):
        inicio = time.monotonic()
        while not self.parar.is_set():
            decorrido = time.monotonic() - inicio
            if duracao is not None and decorrido >= duracao:
                return
            frac = 0 if not duracao else decorrido / duracao
            tps = max(0.05, tps_inicio + (tps_fim - tps_inicio) * frac)
            self.executor.submit(self.lojistas.compra, sortear_perfil())
            if ataques and time.monotonic() >= self.proximo_ataque:
                self.executor.submit(self.lojistas.teste_de_cartao)
                self.proximo_ataque = time.monotonic() + random.uniform(180, 420)
            self.parar.wait(random.expovariate(tps))

    def encerrar(self, espera_max=150):
        self.parar.set()
        self.lojistas.agenda.ativo = False
        print("\nEncerrando: aguardando %d compras em andamento (ate %d s)..."
              % (self.metricas.em_andamento, espera_max), flush=True)
        fim = time.monotonic() + espera_max
        while self.metricas.em_andamento > 0 and time.monotonic() < fim:
            time.sleep(2)
        self.painel.imprimir()
        self.painel.parar.set()
        if self.sonda:
            self.sonda.parar.set()
        self.executor.shutdown(wait=False, cancel_futures=True)
        self.imprimir_resumo()

    def imprimir_resumo(self):
        t, lat = self.metricas.resumo()
        print("\n=== Resumo do lado dos lojistas ===")
        print("- Compras iniciadas: %d" % t["compras"])
        print("- Aprovadas: %d | negadas: %d | em revisao: %d | pendentes: %d | erro: %d"
              % (t["AUTORIZADA"] + t["CAPTURADA"], t["NEGADA"], t["EM_REVISAO"], t["PENDENTE"],
                 t["ERRO"] + t["erro_rede"]))
        print("- Timeouts: %d | retentativas: %d | lojista desistiu: %d"
              % (t["timeouts"], t["retries"], t["desistencias"]))
        print("- Latencia da autorizacao: p50 %s | p95 %s | p99 %s | max %s"
              % (fmt_s(percentil(lat, 50)), fmt_s(percentil(lat, 95)), fmt_s(percentil(lat, 99)),
                 fmt_s(max(lat) if lat else None)))
        print("- Ataques de teste de cartao simulados: %d" % t["ataques"])
        if self.sonda:
            print("- Backoffice: maior espera %.0fs | aberturas sem resposta: %d | sessoes perdidas: %d"
                  % (self.sonda.pior, self.sonda.falhas, self.sonda.sessoes_perdidas))


def cmd_dia_normal(args):
    sim = Simulacao()
    print("Dia normal: %.1f compras/s. Ctrl+C para parar.\n" % args.tps, flush=True)
    sim.painel.start()
    try:
        sim.gerar(args.minutos * 60 if args.minutos else None, args.tps, args.tps)
    except KeyboardInterrupt:
        pass
    sim.encerrar()


def cmd_black_friday(args):
    sim = Simulacao()
    total = sum(f[0] for f in FASES_BLACK_FRIDAY) * args.fator_tempo
    print("Black Friday: pico de %.0f compras/s, cerca de %.0f minutos. Ctrl+C para parar.\n"
          % (args.tps_pico, total / 60), flush=True)
    sim.painel.start()
    try:
        for duracao, ini, fim, modo, descricao in FASES_BLACK_FRIDAY:
            adquirente.definir_modo(modo)
            print(">>> %s  fase: %s (%.0f -> %.0f compras/s, adquirente %s)"
                  % (time.strftime("%H:%M:%S"), descricao, ini * args.tps_pico, fim * args.tps_pico, modo),
                  flush=True)
            sim.gerar(duracao * args.fator_tempo, ini * args.tps_pico, fim * args.tps_pico)
    except KeyboardInterrupt:
        pass
    finally:
        adquirente.definir_modo("normal")
    sim.encerrar()
    if config.BACKOFFICE_SENHA:
        print("\nTrafego encerrado. Aguardando o backoffice voltar a responder...", flush=True)
        espera = esperar_recuperacao()
        if espera is None:
            print("- O backoffice nao voltou em 10 minutos.")
        else:
            print("- O backoffice voltou a responder %.0fs depois do fim do trafego." % espera)
    banco.relatorio_sintomas()


def cmd_adquirente(args):
    adquirente.definir_modo(args.modo)
    print("Adquirente simulada agora esta no modo '%s'." % args.modo)


def cmd_sintomas(_args):
    banco.relatorio_sintomas()


def cmd_aguardar(_args):
    import signal
    signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))
    print("Simulador pronto. Use: docker compose exec simulador simulador <comando>", flush=True)
    while True:
        signal.pause()


def main(argv=None):
    parser = argparse.ArgumentParser(prog="simulador", description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="comando", required=True)

    p = sub.add_parser("dia-normal", help="trafego de um dia comum")
    p.add_argument("--tps", type=float, default=1.5, help="compras por segundo (padrao 1.5)")
    p.add_argument("--minutos", type=float, default=None, help="duracao; sem valor roda ate Ctrl+C")
    p.set_defaults(func=cmd_dia_normal)

    p = sub.add_parser("black-friday", help="pico de trafego com adquirente instavel")
    p.add_argument("--tps-pico", type=float, default=12, help="compras por segundo no pico (padrao 12)")
    p.add_argument("--fator-tempo", type=float, default=1.0,
                   help="multiplica a duracao das fases (0.5 = metade do tempo)")
    p.set_defaults(func=cmd_black_friday)

    p = sub.add_parser("adquirente", help="muda o comportamento da adquirente simulada")
    p.add_argument("modo", choices=sorted(adquirente.MODOS))
    p.set_defaults(func=cmd_adquirente)

    p = sub.add_parser("sintomas", help="relatorio de sintomas lido do banco")
    p.set_defaults(func=cmd_sintomas)

    p = sub.add_parser("aguardar")
    p.set_defaults(func=cmd_aguardar)

    args = parser.parse_args(argv)
    args.func(args)


if __name__ == "__main__":
    sys.exit(main())
