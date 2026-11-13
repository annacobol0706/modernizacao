"""Geracao de cartoes de teste (numeros validos pelo algoritmo de Luhn)."""
import random

# BINs comuns por bandeira, com o peso aproximado de cada bandeira na base
BINS_COMUNS = [
    ("453201", 0.15), ("401288", 0.12), ("455182", 0.10), ("438857", 0.08),  # Visa
    ("542523", 0.12), ("552240", 0.10), ("515590", 0.08), ("222300", 0.05),  # Mastercard
    ("636368", 0.08), ("504175", 0.06), ("451416", 0.06),                    # Elo
]

# BINs cadastrados na tabela BinRisco
BINS_RISCO = ["529999", "491761", "650499"]

# Cartoes cadastrados na lista de bloqueio
CARTOES_BLOQUEADOS = ["4000000000000069", "5162920000000002"]


def digito_luhn(parcial):
    soma = 0
    for i, c in enumerate(reversed(parcial)):
        d = int(c)
        if i % 2 == 0:
            d *= 2
            if d > 9:
                d -= 9
        soma += d
    return str((10 - soma % 10) % 10)


def gerar(bin_):
    corpo = bin_ + "".join(random.choice("0123456789") for _ in range(15 - len(bin_)))
    return corpo + digito_luhn(corpo)


class Carteira:
    """Conjunto fixo de cartoes que os consumidores simulados usam."""

    def __init__(self, tamanho=4000, seed=None):
        rnd = random.Random(seed)
        bins, pesos = zip(*BINS_COMUNS)
        self.comuns = [gerar(rnd.choices(bins, pesos)[0]) for _ in range(tamanho)]
        self.risco = [gerar(rnd.choice(BINS_RISCO)) for _ in range(tamanho // 50)]

    def sortear(self):
        x = random.random()
        if x < 0.003:
            return random.choice(CARTOES_BLOQUEADOS)
        if x < 0.023:
            return random.choice(self.risco)
        return random.choice(self.comuns)

    @staticmethod
    def novo():
        bins, pesos = zip(*BINS_COMUNS)
        return gerar(random.choices(bins, pesos)[0])
