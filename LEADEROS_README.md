# MGT-LeaderOS

Sistema de integração com LeaderOS para Minecraft NeoForge 1.21.1

## Descrição

Este mod conecta seu servidor Minecraft com o sistema LeaderOS, permitindo que compras feitas no site sejam automaticamente entregues aos jogadores no jogo.

## Funcionalidades

- ✅ Conexão automática com a API do LeaderOS
- ✅ Entrega automática de itens comprados no site
- ✅ Execução de comandos personalizados
- ✅ Verificação periódica de entregas pendentes
- ✅ Entrega automática quando jogador entra no servidor
- ✅ Comandos administrativos para controle e debug
- ✅ Compatível com LuckPerms e sistema OP

## Instalação

1. Coloque o arquivo `.jar` do mod na pasta `mods` do servidor
2. Inicie o servidor pela primeira vez
3. O mod criará os arquivos de configuração em `config/mgtleaderos/`
4. Configure os arquivos conforme instruções abaixo
5. Reinicie o servidor ou use `/leaderos reload`

## Configuração

### config.json

```json
{
  "url": "https://servidormagnatas.com.br",
  "api-key": "sua-chave-api-aqui",
  "debug": false,
  "check-interval": 30
}
```

- **url**: URL do seu site LeaderOS
- **api-key**: Chave da API fornecida no painel LeaderOS
- **debug**: Ativa logs detalhados (use apenas para diagnóstico)
- **check-interval**: Intervalo em segundos entre verificações de entregas

### modules.json

```json
{
  "Connect": {
    "status": true,
    "serverToken": "seu-token-do-servidor"
  }
}
```

- **status**: Ativa/desativa o módulo de conexão
- **serverToken**: Token único do servidor fornecido no painel LeaderOS

## Comandos

Todos os comandos requerem permissão `leaderos.admin` ou nível OP 2+

- `/leaderos reload` - Recarrega as configurações
- `/leaderos check` - Verifica manualmente entregas pendentes
- `/leaderos status` - Mostra status da conexão
- `/leaderos test` - Testa conexão com a API

## Como Funciona

1. O mod se conecta à API do LeaderOS ao iniciar o servidor
2. Verifica periodicamente (a cada X segundos) se há compras pendentes
3. Quando encontra uma compra pendente:
   - Verifica se o jogador está online
   - Entrega os itens ao inventário do jogador
   - Executa comandos configurados (se houver)
   - Confirma a entrega na API
4. Se o jogador estiver offline, a entrega é feita quando ele entrar

## Permissões

### LuckPerms
```
leaderos.admin - Acesso total aos comandos administrativos
```

### Sistema OP
- Nível OP 2 ou superior tem acesso automático

## Formato de Entregas

O mod espera entregas no seguinte formato da API:

```json
{
  "id": 123,
  "player_uuid": "uuid-do-jogador",
  "player_name": "NomeDoJogador",
  "items": [
    {
      "item": "minecraft:diamond",
      "amount": 64
    }
  ],
  "commands": [
    "give {player} minecraft:iron_ingot 32"
  ]
}
```

## Troubleshooting

### Mod não conecta à API
- Verifique se a URL está correta em `config.json`
- Verifique se a API Key está correta
- Use `/leaderos test` para diagnosticar

### Entregas não chegam
- Use `/leaderos check` para forçar verificação
- Verifique os logs do servidor
- Ative debug mode em `config.json`

### Permissões não funcionam
- Certifique-se que LuckPerms está instalado (opcional)
- Ou use sistema OP: `/op NomeDoJogador`

## Desenvolvimento

### Estrutura do Código

```
br.com.magnatasoriginal.mgtleaderos/
├── MGTLeaderos.java          # Classe principal
├── api/
│   └── LeaderOSAPIClient.java    # Cliente HTTP da API
├── commands/
│   └── LeaderOSCommands.java     # Comandos administrativos
├── config/
│   └── ConfigManager.java        # Gerenciador de configs
├── delivery/
│   └── DeliveryManager.java      # Lógica de entrega
├── events/
│   └── PlayerEventHandler.java   # Eventos de jogador
└── scheduler/
    └── DeliveryScheduler.java    # Agendador de tarefas
```

## Changelog

### v1.0.0
- Release inicial
- Integração completa com LeaderOS API
- Sistema de entrega automática
- Comandos administrativos
- Suporte a LuckPerms

## Suporte

Para suporte, entre em contato através do Discord do servidor Magnatas.

## Licença

All Rights Reserved © 2025

