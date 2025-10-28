package br.com.magnatasoriginal.mgtleaderos.modules.connect;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Implementação concreta do SocketClient para LeaderOS
 * Chama reconexão no ConnectModule ao perder conexão
 */
public class LeaderOSSocketClient extends SocketClient {
    private final ConnectModule connectModule;
    private static final Logger LOGGER = LogUtils.getLogger();

    public LeaderOSSocketClient(String apiKey, String serverToken, boolean onlyOnline, ConnectModule connectModule) {
        super(apiKey, serverToken, onlyOnline, connectModule);
        this.connectModule = connectModule;
    }

    /**
     * Executa comandos recebidos do LeaderOS
     * Se o jogador estiver online, executa imediatamente; senão, adiciona à fila
     * Validação e limpeza de comandos igual ao plugin original
     */
    @Override
    public void executeCommands(List<String> commands, String username) {
        // Validar e limpar comandos (igual ao plugin original)
        List<String> validatedCommands = new ArrayList<>();
        List<String> commandBlacklist = java.util.Arrays.asList("op", "deop", "stop", "restart", "reload", "ban");

        for (String command : commands) {
            // Remover barra inicial se houver
            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            // Extrair comando raiz (primeira palavra)
            String commandRoot = command.split(" ")[0];

            // Limpar prefixos bukkit: e minecraft:
            if (commandRoot.startsWith("bukkit:")) {
                commandRoot = commandRoot.substring(7);
            }
            if (commandRoot.startsWith("minecraft:")) {
                commandRoot = commandRoot.substring(10);
            }

            // Verificar blacklist
            if (commandBlacklist.contains(commandRoot.toLowerCase())) {
                LOGGER.warn("[LeaderOS] Comando bloqueado por blacklist: {}", command);
                continue;
            }

            // Adicionar comando validado
            validatedCommands.add(command);
        }

        // Se não há comandos válidos, retornar
        if (validatedCommands.isEmpty()) {
            LOGGER.warn("[LeaderOS] Nenhum comando válido para executar");
            return;
        }

        // Buscar jogador online
        ServerPlayer player = connectModule.findOnlinePlayer(username);

        if (player != null && player.getServer() != null && !player.isRemoved()) {
            // Jogador online: executar imediatamente
            player.getServer().execute(() -> {
                for (String command : validatedCommands) {
                    // Executar como console (igual ao plugin: Bukkit.dispatchCommand)
                    player.getServer().getCommands().performPrefixedCommand(
                        player.getServer().createCommandSourceStack(),
                        command
                    );
                    LOGGER.info("[LeaderOS] Comando executado: {}", command);
                }
            });
        } else {
            // Jogador offline: adicionar à fila
            ConnectModule.getCommandsQueue().addCommands(username, validatedCommands);
            LOGGER.info("[LeaderOS] Jogador {} offline. {} comandos adicionados à fila", username, validatedCommands.size());
        }
    }

    @Override
    public void subscribed() {
        // Loga sucesso de conexão
        // Pode adicionar lógica extra se necessário
    }

    /**
     * Método chamado pelo WebSocket ao fechar conexão
     * Chama reconexão no ConnectModule
     */
    public void notifyWebSocketClosed(int code, String reason) {
        connectModule.onWebSocketClosed(code, reason);
    }
}
