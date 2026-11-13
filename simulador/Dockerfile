FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY pagsim ./pagsim
RUN printf '#!/bin/sh\ncd /app && exec python -m pagsim "$@"\n' > /usr/local/bin/simulador \
    && chmod 755 /usr/local/bin/simulador \
    && useradd --uid 10001 --no-create-home simulador
USER simulador
ENV PYTHONUNBUFFERED=1
# Fica aguardando comandos: docker compose exec simulador simulador <comando>
CMD ["python", "-m", "pagsim", "aguardar"]
