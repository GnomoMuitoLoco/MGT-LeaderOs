package br.com.magnatasoriginal.mgtleaderos;

import br.com.magnatasoriginal.mgtleaderos.api.LeaderOSAPIClient;
import br.com.magnatasoriginal.mgtleaderos.commands.LeaderOSCommands;
import br.com.magnatasoriginal.mgtleaderos.config.ConfigManager;
import br.com.magnatasoriginal.mgtleaderos.modules.connect.ConnectModule;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

/**
 * Classe principal do mod MGT LeaderOS
 * Integra o servidor Minecraft com o sistema LeaderOS para entrega automática de compras via WebSocket
 *
 * Conversão fiel do plugin Bukkit/Spigot original para NeoForge 21.1.211
 * Todas as entregas são feitas via WebSocket (Pusher), sem polling HTTP
 */
@Mod(MGTLeaderos.MODID)
public class MGTLeaderos {

    public static final String MODID = "mgtleaderos";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Torna o singleton público para acesso externo
    public static MGTLeaderos instance;

    /**
     * Retorna a instância singleton do mod
     * Compatível com chamadas MGTLeaderos.getInstance() do código legado
     */
    public static MGTLeaderos getInstance() {
        return instance;
    }

    private ConfigManager configManager;
    private LeaderOSAPIClient apiClient;
    private ConnectModule connectModule;
    private MinecraftServer server;
    private boolean connected = false;

    public MGTLeaderos(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        // Registrar para eventos do NeoForge
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("[MGT-LeaderOS] Mod inicializando...");
    }

    /**
     * Chamado quando o servidor inicia
     * Inicializa o módulo Connect para comunicação via WebSocket com LeaderOS
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.server = event.getServer();

        // Criar diretório de configuração
        File configDir = new File("config/mgtleaderos");

        // Carregar configurações
        configManager = new ConfigManager(configDir);

        // Verificar se o módulo Connect está ativado
        if (!configManager.isConnectEnabled()) {
            LOGGER.warn("Módulo Connect desativado. O mod não funcionará até que seja ativado em modules.json");
            return;
        }

        // Verificar se as configurações estão preenchidas
        if (configManager.getApiKey().isEmpty() || configManager.getServerToken().isEmpty()) {
            LOGGER.warn("API Key ou Server Token não configurados. Configure em config.json e modules.json");
            return;
        }

        // Inicializar cliente da API
        apiClient = new LeaderOSAPIClient(
            configManager.getUrl(),
            configManager.getApiKey(),
            configManager.getServerToken()
        );

        // Inicializar módulo Connect (autenticação via WebSocket, igual ao plugin original)
        LOGGER.info("Inicializando módulo Connect via WebSocket...");
        connectModule = new ConnectModule(server, configDir.getAbsolutePath());
        connectModule.enable(
            configManager.getUrl(), // URL base do site
            configManager.getApiKey(),
            configManager.getServerToken(),
            true // onlyOnline - executar comandos apenas quando jogador estiver online
        );

        // Não registra ConnectModule no event bus, pois não possui métodos @SubscribeEvent
        // A execução de comandos pendentes é tratada via WebSocket e fila interna

        connected = true;
        LOGGER.info("Conexão com LeaderOS estabelecida com sucesso via WebSocket!");

        // Nota: O plugin original NÃO usa polling HTTP para verificar entregas.
        // Todas as entregas/comandos são recebidos via WebSocket (evento send-commands do Pusher).
        // O DeliveryScheduler foi desabilitado para ser fiel ao plugin original.

        LOGGER.info("Módulo Connect ativo! Aguardando comandos via WebSocket...");
    }

    /**
     * Chamado quando comandos são registrados
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LeaderOSCommands.register(event.getDispatcher());
        LOGGER.info("Comandos do LeaderOS registrados.");
    }

    /**
     * Chamado quando o servidor está parando
     * Desconecta do WebSocket e limpa recursos
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[MGT-LeaderOS] Desligando mod...");

        if (connectModule != null) {
            connectModule.disable();
            // Libera recursos do WebSocket para evitar reutilização
            connectModule = null;
        }

        connected = false;
        LOGGER.info("[MGT-LeaderOS] Mod desligado com sucesso.");
    }

    /**
     * Recarrega as configurações
     * Reconecta ao WebSocket se necessário
     */
    public void reloadConfig() {
        if (configManager != null) {
            configManager.reload();
            LOGGER.info("[MGT-LeaderOS] Configurações recarregadas.");

            // Reconectar se necessário
            if (connected) {
                // Desconectar e liberar recursos antigos
                if (connectModule != null) {
                    connectModule.disable();
                    connectModule = null;
                }
                // Criar nova instância do ConnectModule para evitar erro de reutilização do WebSocketClient
                connectModule = new ConnectModule(server, configManager.getConfigDir().getAbsolutePath());
                connectModule.enable(
                    configManager.getUrl(), // URL base do site
                    configManager.getApiKey(),
                    configManager.getServerToken(),
                    true
                );
                // Não registra no event bus (não possui métodos @SubscribeEvent)
                LOGGER.info("[MGT-LeaderOS] Reconectado ao LeaderOS com sucesso.");
            }
        }
    }

    /**
     * Testa conexão com a API
     * Nota: O plugin original não usa teste HTTP, toda autenticação é via WebSocket
     */
    public boolean testConnection() {
        return connected && connectModule != null && connectModule.getSocketClient() != null &&
               connectModule.getSocketClient().isConnected();
    }

    /**
     * Verifica entregas pendentes manualmente (comando ou evento)
     * Adaptação do plugin: verifica fila de comandos e executa se possível
     */
    public void checkDeliveries() {
        if (connectModule != null && connectModule.getCommandsQueue() != null && server != null) {
            // Obtém jogadores com comandos pendentes de forma segura
            List<String> jogadores = connectModule.getCommandsQueue().getPlayersWithPendingCommands();
            for (String playerName : jogadores) {
                List<String> commands = connectModule.getCommandsQueue().getCommands(playerName);
                if (commands == null || commands.isEmpty()) continue;
                LOGGER.info("[LeaderOS] Executando " + commands.size() + " comandos pendentes para " + playerName);
                server.execute(() -> {
                    commands.forEach(cmd -> {
                        server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack(),
                            cmd
                        );
                        LOGGER.info("[LeaderOS] Comando da fila executado: " + cmd);
                    });
                    connectModule.getCommandsQueue().removeCommands(playerName);
                });
            }
        } else {
            LOGGER.warn("[LeaderOS] Não foi possível verificar entregas: módulo Connect ou fila não inicializados.");
        }
    }

    /**
     * Chamado quando um jogador entra no servidor
     * Executa comandos pendentes da fila se houver
     */
    @SubscribeEvent
    public void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (connectModule == null || ConnectModule.getCommandsQueue() == null) return;

        String playerName = event.getEntity().getName().getString();
        LOGGER.info("Jogador {} entrou. Verificando entregas pendentes...", playerName);

        // Verificar se há comandos pendentes para este jogador de forma assíncrona
        ConnectModule.getCommandsQueue().getExecutor().execute(() -> {
            List<String> commands = ConnectModule.getCommandsQueue().getCommands(playerName);

            if (commands == null || commands.isEmpty()) {
                return;
            }

            LOGGER.info("[LeaderOS] Executando {} comandos pendentes para {}", commands.size(), playerName);

            // Aguardar 5 segundos antes de executar (conforme plugin original)
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOGGER.warn("[LeaderOS Connect] Execução de comandos interrompida para jogador: " + playerName);
                Thread.currentThread().interrupt();
                return;
            }

            // Executar comandos no thread do servidor (igual ao plugin: dispatchCommand do console)
            server.execute(() -> {
                commands.forEach(cmd -> {
                    server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        cmd
                    );
                    LOGGER.info("[LeaderOS] Comando da fila executado: {}", cmd);
                });

                // Remover comandos da fila
                ConnectModule.getCommandsQueue().removeCommands(playerName);
                LOGGER.info("[LeaderOS] Comandos de {} removidos da fila", playerName);
            });
        });
    }

    // Getters
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public boolean isConnected() {
        return connected;
    }

    // Adicionar método público getServer() em MGTLeaderos
    public MinecraftServer getServer() {
        return server;
    }
}
