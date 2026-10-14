FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PYTHONPATH=/sources/core:/sources/level2:/sources/level3:/circle
RUN apt-get update && apt-get install -y --no-install-recommends openssl && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 10001 genesis && useradd -u 10001 -g 10001 -M genesis
COPY --chown=10001:10001 sources/core /sources/core
COPY --chown=10001:10001 sources/level2 /sources/level2
COPY --chown=10001:10001 sources/level3 /sources/level3
COPY --chown=10001:10001 prepare.py check.py /circle/
RUN chmod -R a+rX /sources /circle
USER 10001:10001
WORKDIR /sources/core
CMD ["python", "/circle/check.py"]
