# MGT-LeaderOS (NeoForge 1.21.1 / 21.1.211)

Integração do servidor Minecraft com o LeaderOS para entrega automática de compras. Este projeto é uma conversão fiel do plugin LeaderOS (Bukkit/Spigot) para mod NeoForge, mantendo nomes, fluxo e mensagens originais sempre que possível — com foco total no módulo de compras (Connect) que transforma resgates no site em comandos executados no console do servidor.

- Minecraft: 1.21.1
- NeoForge: 21.1.211
- Java: 21

## Principais recursos
- Conexão WebSocket com LeaderOS (canal privado por serverToken) para receber eventos de compra/resgate em tempo real.
- Validação dos comandos via API antes da execução (segurança/consistência).
- Entrega no jogo: comandos são executados via console; se o jogador não estiver online e a política exigir, o comando é enfileirado e executado quando ele entrar.
- Fila de comandos persistente em disco (para tolerância a quedas/desligamentos).
- Mensagens de log em PT-BR, equivalentes ao plugin original.

## Requisitos
- Servidor dedicado/host com Java 21.
- NeoForge 21.1.211 para Minecraft 1.21.1.
- Acesso ao painel LeaderOS para obter: URL do site, API Key e Server Token.

## Instalação
1) Baixe o JAR do mod (mgtleaderos-*.jar) e coloque em `./mods` do servidor NeoForge.
2) Inicie o servidor uma vez. Serão gerados os arquivos de configuração em `config/mgtleaderos`.
3) Edite os arquivos:
   - `config.yml` — preencha `settings.url` e `settings.apiKey`.
   - `modules.yml` — em `Connect` preencha `status` e `serverToken`.
4) Reinicie o servidor.

## Configuração (compatível com o plugin original)
- `config/mgtleaderos/config.yml`
  - `settings.url` — URL base do seu site (ex.: `https://seusite.com`).
  - `settings.apiKey` — API Key do LeaderOS.
- `config/mgtleaderos/modules.yml`
  - `Connect.status` — true/false para habilitar o módulo de entregas.
  - `Connect.serverToken` — token do servidor (Dashboard > Store > Game Servers).

Exemplo mínimo:
```
settings:
  url: "https://seusite.com"
  apiKey: "SUA_API_KEY"

Connect:
  status: true
  serverToken: "SEU_SERVER_TOKEN"
```

## Comandos
- `/leaderos reload` — recarrega `config.yml` e `modules.yml` e restabelece a conexão quando necessário.

Obs.: comandos adicionais podem existir conforme evolução do projeto, mas o foco aqui é o fluxo de compras → execução de comandos.

## Logs esperados
Ao iniciar com sucesso:
- `[LeaderOS Connect] Módulo Connect inicializado`
- `[LeaderOS Connect] Conectado ao servidor via Pusher`
- `Módulo Connect ativo! Aguardando comandos via WebSocket...`

Ao executar comandos:
- `[LeaderOS] Comando da fila executado: <comando>`
- Se o jogador estiver offline e a política exigir fila: `Jogador <nome> offline. N comandos na fila`

Reconexão (quando necessário):
- Mensagens de reconexão aparecem apenas quando há perda real de conexão. Timeouts comuns são silenciosos.

## Resolução de problemas
- 403 na autenticação: verifique `settings.apiKey` e `Connect.serverToken`.
- 404 na API: confirme a `settings.url` (use `https://...`) e o caminho correto do endpoint no LeaderOS.
- Comando inválido/desconhecido: revise o comando configurado no site (ex.: `darvip %username% vipbasico 30d`) e certifique-se de que o plugin alvo/comando exista no servidor.
- Reconexões frequentes: a conexão responde a `pusher:ping` com `pusher:pong` automaticamente. Se persistir, verifique firewall/latência.

## Build (desenvolvedores)
- Windows (PowerShell/CMD):
```
./gradlew.bat build
```
- Saída: `build/libs/mgtleaderos-*.jar`

## Créditos e agradecimentos
- Conversão para NeoForge 1.21.1 (21.1.211): Servidor Magnatas.
- Projeto original (plugin LeaderOS) e autores: LeaderOS e contribuidores.

## Licença
Consulte o arquivo de licença do projeto original e/ou este repositório (se aplicável). Caso não exista, considere este mod de uso interno do servidor que realizou a portabilidade.
