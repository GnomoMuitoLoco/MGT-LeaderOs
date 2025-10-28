package br.com.magnatasoriginal.mgtleaderos.modules.connect;

import java.util.ArrayList;
import java.util.List;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Implementação concreta do SocketClient para LeaderOS
 * Sem reconexão manual - gerenciada automaticamente
 */
public class LeaderOSSocketClient extends SocketClient {
    private final ConnectModule connectModule;
    private static final Logger LOGGER = LogUtils.getLogger();

    public LeaderOSSocketClient(String baseUrl, String apiKey, String serverToken, ConnectModule connectModule) {
        super(baseUrl, apiKey, serverToken, connectModule);
        this.connectModule = connectModule;
    }

    @Override
    public void executeCommands(List<String> commands, String username) {
        List<String> validatedCommands = new ArrayList<>();
        List<String> commandBlacklist = java.util.Arrays.asList("op", "deop", "stop", "restart", "reload", "ban");

        for (String command : commands) {
            // Removida substituição de %username% para manter comportamento original do plugin

            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            String commandRoot = command.split(" ")[0];

            if (commandRoot.startsWith("bukkit:")) {
                commandRoot = commandRoot.substring(7);
            }
            if (commandRoot.startsWith("minecraft:")) {
                commandRoot = commandRoot.substring(10);
            }

            if (commandBlacklist.contains(commandRoot.toLowerCase())) {
                // Mantém o comportamento do plugin: log em warn quando for bloqueado
                MGTLeaderos.LOGGER.warn("[LeaderOS] Comando bloqueado: " + command);
                continue;
            }

            validatedCommands.add(command);
        }

        if (validatedCommands.isEmpty()) {
            return;
        }

        ServerPlayer player = connectModule.findOnlinePlayer(username);

        if (player != null && player.getServer() != null && !player.isRemoved()) {
            // Executa os comandos como console no thread do servidor
            player.getServer().execute(() -> {
                for (String command : validatedCommands) {
                    player.getServer().getCommands().performPrefixedCommand(
                        player.getServer().createCommandSourceStack(),
                        command
                    );
                    // Mensagem idêntica ao plugin original
                    MGTLeaderos.LOGGER.info("[LeaderOS] Comando da fila executado: " + command);
                }
            });
        } else {
            // Se jogador offline, adiciona à fila e loga exatamente como plugin original
            ConnectModule.getCommandsQueue().addCommands(username, validatedCommands);
            MGTLeaderos.LOGGER.info("[LeaderOS] Jogador " + username + " offline. " + validatedCommands.size() + " comandos na fila");
        }
    }

    @Override
    public void subscribed() {
        // Chamado quando subscrição é bem-sucedida
    }
}
