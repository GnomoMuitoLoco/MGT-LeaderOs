package br.com.magnatasoriginal.mgtleaderos.modules.connect;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Módulo Connect simples sem reconexão manual
 * O WebSocket reconecta automaticamente
 *
 * @author Conversão de Bukkit para NeoForge
 */
public class ConnectModule {
    private SocketClient socketClient;
    private static CommandsQueue commandsQueue;
    private final MinecraftServer server;
    private final String configPath;

    // Variáveis para controle de reconexão
    private int reconnectAttempt;
    private volatile boolean reconnecting;
    private volatile boolean shuttingDown = false; // Flag para parar reconexão
    private Thread reconnectThread;

    // Guardar credenciais para reconexão correta (evitar 403)
    private String baseUrl;
    private String apiKey;
    private String serverToken;

    public ConnectModule(MinecraftServer server, String configPath) {
        this.server = server;
        this.configPath = configPath;
    }

    public void enable(String baseUrl, String apiKey, String serverToken, boolean onlyOnline) {
        if (socketClient != null) {
            socketClient.close();
            socketClient = null;
        }

        // Persistir credenciais
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.serverToken = serverToken;

        socketClient = new LeaderOSSocketClient(baseUrl, apiKey, serverToken, this);
        socketClient.connect();

        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Módulo Connect inicializado");
        commandsQueue = new CommandsQueue(configPath);

        // Inicializa variáveis de reconexão
        this.reconnectAttempt = 0;
        this.reconnecting = false;
    }

    public void disable() {
        // Ativar flag de shutdown para prevenir novas reconexões
        shuttingDown = true;

        // Interromper thread de reconexão se estiver rodando
        if (reconnectThread != null && reconnectThread.isAlive()) {
            reconnectThread.interrupt();
            try {
                reconnectThread.join(2000); // Aguardar até 2 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Fechar socket
        if (socketClient != null) {
            try {
                socketClient.close();
            } catch (Exception e) {
                MGTLeaderos.LOGGER.warn("[LeaderOS Connect] Erro ao fechar socket: " + e.getMessage());
            }
            socketClient = null;
        }

        // Desligar fila de comandos
        if (commandsQueue != null) {
            commandsQueue.shutdown();
            commandsQueue = null;
        }

        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Módulo Connect desligado");
    }

    // Chamado pelo SocketClient quando a conexão fecha
    public synchronized void handleDisconnect(int code, String reason) {
        // Se está desligando, não tentar reconectar
        if (shuttingDown) {
            return;
        }

        // Silenciar logs para códigos comuns (4201 timeout, 1000 fechamento normal)
        if (code != 4201 && code != 1000) {
            MGTLeaderos.LOGGER.warn("[LeaderOS Connect] Conexão fechada. Código: {}, Razão: {}", code, reason);
        }

        // Marcar estado e iniciar reconexão controlada
        if (!reconnecting) {
            reconnecting = true;
            reconnectAttempt = 0;
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        // Política de reconexão com backoff exponencial (até 6 tentativas por ciclo)
        reconnectThread = new Thread(() -> {
            while (reconnectAttempt < 6 && !shuttingDown) {
                try {
                    reconnectAttempt++;
                    int waitSeconds = (int) Math.min(60, Math.pow(2, reconnectAttempt));
                    MGTLeaderos.LOGGER.info("[LeaderOS Connect] Tentando reconectar... Tentativa {}/6 em {}s", reconnectAttempt, waitSeconds);
                    Thread.sleep(waitSeconds * 1000L);

                    // Checar novamente se está desligando após sleep
                    if (shuttingDown) {
                        break;
                    }

                    // Criar nova instância do socket para evitar erro de reutilização
                    if (socketClient != null) {
                        try { socketClient.close(); } catch (Exception ignored) {}
                        socketClient = null;
                    }
                    // Usar credenciais corretas (evitar 403)
                    socketClient = new LeaderOSSocketClient(this.baseUrl, this.apiKey, this.serverToken, this);
                    socketClient.connect();

                    // Aguarda alguns segundos para ver se subscreve
                    Thread.sleep(5000);

                    // Checar novamente se está desligando após sleep
                    if (shuttingDown) {
                        break;
                    }

                    if (socketClient.isSubscribed()) {
                        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Reconexão bem-sucedida!");
                        reconnecting = false;
                        reconnectAttempt = 0;
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (!shuttingDown) {
                        MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao tentar reconectar: " + e.getMessage());
                    }
                }
            }

            if (!shuttingDown && reconnectAttempt >= 6) {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Falha ao reconectar após tentativas. Reinicie o servidor ou verifique suas configurações.");
            }
            reconnecting = false;
        }, "LeaderOS-Reconnect");
        reconnectThread.setDaemon(true); // Thread daemon para não impedir shutdown do servidor
        reconnectThread.start();
    }

    public static CommandsQueue getCommandsQueue() {
        return commandsQueue;
    }

    // Adiciona getter público para socketClient (necessário para verificações em MGTLeaderos)
    public SocketClient getSocketClient() {
        return socketClient;
    }

    public ServerPlayer findOnlinePlayer(String username) {
        if (server == null) return null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(username)) {
                return player;
            }
        }
        return null;
    }
}
