package br.com.magnatasoriginal.mgtleaderos.modules.connect;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer; // Corrige import para NeoForge

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Módulo Connect do LeaderOS
 * Gerencia a conexão via WebSocket e execução de comandos
 * Implementação fiel ao plugin Bukkit/Spigot original
 *
 * @author Conversão de Bukkit para NeoForge
 */
public class ConnectModule {
    private SocketClient socketClient;
    private String apiKey;
    private String serverToken;
    private String baseUrl; // URL base do site para validação de comandos
    private boolean onlyOnline;
    private ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean reconnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 100;
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private static CommandsQueue commandsQueue;
    private final MinecraftServer server;
    private final String configPath;

    /**
     * Construtor do ConnectModule
     * @param server Instância do servidor Minecraft
     * @param configPath Caminho da pasta de configuração
     */
    public ConnectModule(MinecraftServer server, String configPath) {
        this.server = server;
        this.configPath = configPath;
    }

    /**
     * Inicializa o módulo Connect
     * @param baseUrl URL base do site (ex: https://servidormagnatas.com.br)
     * @param apiKey API Key do LeaderOS
     * @param serverToken Token do servidor
     * @param onlyOnline Se true, comandos só são executados quando jogador está online
     */
    public void enable(String baseUrl, String apiKey, String serverToken, boolean onlyOnline) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.serverToken = serverToken;
        this.onlyOnline = onlyOnline;
        reconnecting = false;
        reconnectAttempts = 0;
        if (socketClient != null) {
            socketClient.disconnect();
            socketClient = null;
        }
        // Instancia a implementação concreta
        socketClient = new LeaderOSSocketClient(apiKey, serverToken, onlyOnline, this);
        socketClient.connect();
        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Inicializando módulo Connect...");
        commandsQueue = new CommandsQueue(configPath);
        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Módulo Connect inicializado com sucesso!");
    }

    /**
     * Desabilita o módulo Connect
     */
    public void disable() {
        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Desabilitando módulo Connect...");

        if (socketClient != null) {
            socketClient.disconnect();
        }

        if (commandsQueue != null) {
            commandsQueue.shutdown();
        }

        reconnectScheduler.shutdownNow(); // Encerra scheduler ao desabilitar

        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Módulo Connect desabilitado!");
    }

    /**
     * Retorna a instância do SocketClient
     * @return SocketClient
     */
    public SocketClient getSocketClient() {
        return socketClient;
    }

    /**
     * Retorna a instância do CommandsQueue
     * @return CommandsQueue
     */
    public static CommandsQueue getCommandsQueue() {
        return commandsQueue;
    }

    /**
     * Retorna a URL base do site para validação de comandos
     * @return URL base (ex: https://servidormagnatas.com.br)
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Busca um jogador online pelo nome (NeoForge)
     * @param username Nome do jogador
     * @return ServerPlayer se online, senão null
     */
    public ServerPlayer findOnlinePlayer(String username) {
        // Usa a instância do servidor já presente no ConnectModule
        if (server == null) return null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Substituir getProfile() por getGameProfile() para obter o nome do jogador
            if (player.getGameProfile().getName().equalsIgnoreCase(username)) {
                return player;
            }
        }
        return null;
    }

    // Método chamado pelo SocketClient ao fechar conexão
    public void onWebSocketClosed(int code, String reason) {
        MGTLeaderos.LOGGER.warn("[LeaderOS Connect] Conexão fechada. Código: " + code + ", Razão: " + reason);
        if (!reconnecting) {
            reconnecting = true;
            reconnectAttempts = 1;
            scheduleReconnect();
        }
    }

    // Agenda reconexão assíncrona com backoff
    private void scheduleReconnect() {
        if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Falha ao reconectar após " + MAX_RECONNECT_ATTEMPTS + " tentativas.");
            reconnecting = false;
            return;
        }
        reconnectScheduler.schedule(() -> attemptReconnect(), RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    // Tenta reconectar criando sempre um novo SocketClient
    private void attemptReconnect() {
        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Tentando reconectar... Tentativa " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS);
        if (socketClient != null) {
            socketClient.disconnect();
            socketClient = null;
        }
        socketClient = new LeaderOSSocketClient(apiKey, serverToken, onlyOnline, this);
        socketClient.connect();
        reconnectAttempts++;
        reconnecting = false;
    }
}
