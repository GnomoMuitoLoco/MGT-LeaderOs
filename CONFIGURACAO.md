# Configuração do MGT-LeaderOS

## Arquivos de Configuração

Após a primeira inicialização do servidor, o mod criará automaticamente os arquivos de configuração em:
```
config/mgtleaderos/
├── config.json
└── modules.json
```

## config.json

Este arquivo contém as configurações principais de conexão com a API do LeaderOS.

### Exemplo Completo:
```json
{
  "url": "https://servidormagnatas.com.br",
  "api-key": "b90b6b3d9d2e050a003dbdd2715277a3",
  "debug": false,
  "check-interval": 30
}
```

### Parâmetros:

- **url** (string, obrigatório)
  - URL base do seu site LeaderOS
  - Exemplo: `"https://servidormagnatas.com.br"`
  - Não adicione barra no final

- **api-key** (string, obrigatório)
  - Chave de API fornecida no painel administrativo do LeaderOS
  - Você encontra esta chave em: Painel Admin > Configurações > API
  - Exemplo: `"b90b6b3d9d2e050a003dbdd2715277a3"`

- **debug** (boolean, opcional)
  - Ativa logs detalhados para diagnóstico
  - Valores: `true` ou `false`
  - Padrão: `false`
  - **Atenção:** Não deixe ativado em produção, gera muitos logs

- **check-interval** (number, opcional)
  - Intervalo em segundos entre verificações automáticas de entregas pendentes
  - Valor mínimo recomendado: 10 segundos
  - Valor máximo recomendado: 300 segundos (5 minutos)
  - Padrão: `30`
  - **Dica:** Valores muito baixos podem sobrecarregar a API

## modules.json

Este arquivo controla quais módulos do LeaderOS estão ativos.

### Exemplo Completo:
```json
{
  "Connect": {
    "status": true,
    "serverToken": "0b7b0fa6da5ca972900296fc14c92834"
  }
}
```

### Módulos:

#### Connect (obrigatório)
Módulo principal que gerencia a conexão e entrega de compras.

- **status** (boolean, obrigatório)
  - Ativa/desativa o módulo
  - Valores: `true` ou `false`
  - Se `false`, o mod não funcionará

- **serverToken** (string, obrigatório)
  - Token único que identifica seu servidor no sistema LeaderOS
  - Você encontra este token em: Painel Admin > Servidores > [Seu Servidor]
  - Exemplo: `"0b7b0fa6da5ca972900296fc14c92834"`

## Configuração Inicial

### Passo 1: Obter as Credenciais

1. Acesse o painel administrativo do LeaderOS
2. Vá em **Configurações > API**
3. Copie sua **API Key**
4. Vá em **Servidores**
5. Selecione ou crie seu servidor
6. Copie o **Server Token**

### Passo 2: Configurar o Mod

1. Inicie o servidor Minecraft pela primeira vez com o mod instalado
2. O mod criará os arquivos de configuração automaticamente
3. Pare o servidor
4. Edite `config/mgtleaderos/config.json`:
   ```json
   {
     "url": "https://seu-site.com.br",
     "api-key": "sua-api-key-aqui",
     "debug": false,
     "check-interval": 30
   }
   ```
5. Edite `config/mgtleaderos/modules.json`:
   ```json
   {
     "Connect": {
       "status": true,
       "serverToken": "seu-server-token-aqui"
     }
   }
   ```
6. Salve os arquivos
7. Inicie o servidor novamente

### Passo 3: Verificar Conexão

1. Entre no servidor
2. Execute o comando: `/leaderos test`
3. Se a conexão for bem-sucedida, você verá: `§a[LeaderOS] Conexão estabelecida com sucesso!`

## Recarregar Configurações

Você pode recarregar as configurações sem reiniciar o servidor:

```
/leaderos reload
```

Isso irá:
- Recarregar ambos arquivos de configuração
- Reconectar com a API usando as novas credenciais
- Reiniciar o agendador de verificações

## Formato de Itens na API

O mod espera que a API retorne entregas no seguinte formato:

```json
{
  "id": 123,
  "player_uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "player_name": "Steve",
  "items": [
    {
      "item": "minecraft:diamond",
      "amount": 64,
      "nbt": "{display:{Name:'[{\"text\":\"Diamante Especial\"}]'}}"
    },
    {
      "item": "minecraft:iron_ingot",
      "amount": 32
    }
  ],
  "commands": [
    "give {player} minecraft:emerald 16",
    "tellraw {player} {\"text\":\"Obrigado pela compra!\",\"color\":\"green\"}"
  ]
}
```

### Campos:

- **id**: ID único da compra no sistema
- **player_uuid**: UUID do jogador (formato com hífens)
- **player_name**: Nome do jogador (para logs)
- **items**: Array de itens a serem entregues
  - **item**: ID do item no formato `namespace:path`
  - **amount**: Quantidade (1-64)
  - **nbt**: (opcional) Dados NBT em formato string
- **commands**: Array de comandos a serem executados
  - Placeholders disponíveis: `{player}`, `{uuid}`

## Troubleshooting

### "Falha ao conectar com a API"

**Possíveis causas:**
- URL incorreta
- API Key inválida
- Servidor LeaderOS offline
- Firewall bloqueando a conexão

**Solução:**
1. Verifique a URL em `config.json`
2. Verifique a API Key no painel LeaderOS
3. Teste a URL no navegador
4. Use `/leaderos test` para diagnóstico
5. Ative `"debug": true` e verifique os logs

### "Entregas não chegam"

**Possíveis causas:**
- Server Token incorreto
- Módulo Connect desativado
- Jogador com inventário cheio

**Solução:**
1. Verifique o Server Token em `modules.json`
2. Certifique-se que `"status": true` em Connect
3. Use `/leaderos check` para forçar verificação
4. Verifique os logs do servidor

### "Item não encontrado"

**Causa:**
- ID do item incorreto ou mod não instalado

**Solução:**
1. Verifique se o ID está no formato correto: `namespace:path`
2. Exemplos válidos: `minecraft:diamond`, `modid:custom_item`
3. Se for item de mod, certifique-se que o mod está instalado

## Segurança

⚠️ **IMPORTANTE:**

1. **Nunca compartilhe** sua API Key ou Server Token
2. **Não commite** os arquivos de configuração com credenciais em repositórios públicos
3. Use **HTTPS** na URL do LeaderOS
4. Mantenha o mod **atualizado**

## Suporte

Para suporte técnico:
- Discord do Servidor Magnatas
- GitHub Issues (se aplicável)

