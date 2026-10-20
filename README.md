# TP1 — Comunicação e Sincronização de Processos e Threads

Sistemas Operacionais — Engenharia de Software — PUC Minas
Prof. Diego Silva Caldeira Rocha

## Divisão do grupo

Cada integrante explica no vídeo os problemas pelos quais é responsável.
**A ausência de qualquer integrante nos vídeos zera a nota do grupo.**

| Problema | Responsável | Código | Modelagem | Vídeo |
|---|---|---|---|---|
| (a) Produtor e Consumidor | _a definir_ | | | |
| (b) Barbeiro Dorminhoco | André Almeida Silva | [`src/barbearia/`](src/barbearia/) | [docs/b-barbeiro.md](docs/b-barbeiro.md) | [assistir](https://www.youtube.com/watch?v=dCvOYLgoWlQ) |
| (c) Leitores e Escritores | _a definir_ | | | |
| (d) Fumantes | Felipe Paz | [`src/fumantes/`](src/fumantes/) | [docs/d-fumantes.md](docs/d-fumantes.md) | _pendente_ |
| (e) Jantar dos Filósofos | Felipe Paz | [`src/filosofos/`](src/filosofos/) | [docs/e-filosofos.md](docs/e-filosofos.md) | _pendente_ |
| (f) Jantar dos Selvagens | Petrius |[`src/selvagens/`](src/selvagens/) |[docs/f-jantardosselvagens.md](docs/f-jantardosselvagens.md) | |
| (g) Três Robôs | _a definir_ | | | |

Checklist de entrega (enunciado):
- [ ] Código-fonte completo de todos os problemas
- [ ] Vídeo de cada integrante cobrindo: modelagem, threads e linhas de execução, recursos compartilhados
      e regiões críticas, semáforos, estratégia contra deadlock, execução e testes
- [ ] Links dos vídeos acessíveis (testar em aba anônima)

## Como clonar e executar

Requer Java 21+ (`java -version` para conferir).

```
git clone https://github.com/felipepcbatista/so-tp1-sincronizacao.git
cd so-tp1-sincronizacao
```

No Windows (PowerShell):

```powershell
.\rodar.ps1 barbearia correto 12
.\rodar.ps1 barbearia sem-mutex 12
.\rodar.ps1 barbearia mutex-preso 12
.\rodar.ps1 fumantes correto 10
.\rodar.ps1 fumantes ingenuo 10
.\rodar.ps1 filosofos garcom 3
.\rodar.ps1 filosofos ingenuo 3
.\rodar.ps1 testes
```

Se o PowerShell bloquear o script, rode antes: `Set-ExecutionPolicy -Scope Process Bypass`.

Em qualquer sistema:

```
javac -encoding UTF-8 -d out src/util/*.java src/barbearia/*.java src/fumantes/*.java src/filosofos/*.java
java -cp out barbearia.MainBarbearia correto 12
java -cp out fumantes.MainFumantes correto 10
java -cp out filosofos.MainFilosofos garcom 3
java -cp out barbearia.TesteBarbearia
java -cp out fumantes.TesteFumantes
java -cp out filosofos.TesteFilosofos
```
