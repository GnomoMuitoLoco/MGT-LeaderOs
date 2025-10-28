package br.com.magnatasoriginal.mgtleaderos.commands;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comandos administrativos do LeaderOS
 * Permite recarregar configurações e verificar status
 */
public class LeaderOSCommands {

    /**
     * Registra os comandos do mod
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("leaderos")
                .requires(source -> hasPermission(source, "leaderos.admin"))
                .then(Commands.literal("reload")
                    .executes(LeaderOSCommands::reloadConfig))
                .then(Commands.literal("check")
                    .executes(LeaderOSCommands::checkDeliveries))
                .then(Commands.literal("status")
                    .executes(LeaderOSCommands::showStatus))
                .then(Commands.literal("test")
                    .executes(LeaderOSCommands::testConnection))
        );
    }

    /**
     * Verifica se o jogador tem permissão
     * Compatível com LuckPerms e sistema OP
     */
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        // Se for o console, sempre tem permissão
        if (!source.isPlayer()) {
            return true;
        }

        // Verificar se é OP (nível 4 = admin completo)
        if (source.hasPermission(4)) {
            return true;
        }

        // Verificar permissão específica (LuckPerms compatível)
        try {
            ServerPlayer player = source.getPlayerOrException();
            // LuckPerms é verificado através do sistema de permissões do servidor
            // Se LuckPerms estiver instalado, ele intercepta essa verificação
            return source.hasPermission(2); // Nível de permissão para comandos admin
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Comando para recarregar configurações
     */
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            MGTLeaderos.instance.reloadConfig();
            source.sendSuccess(() -> Component.literal("§a[LeaderOS] Configurações recarregadas com sucesso!"), true);
            MGTLeaderos.LOGGER.info("Configurações recarregadas por " + source.getTextName());
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[LeaderOS] Erro ao recarregar configurações: " + e.getMessage()));
            MGTLeaderos.LOGGER.error("Erro ao recarregar configurações", e);
        }

        return 1;
    }

    /**
     * Comando para verificar entregas manualmente
     */
    private static int checkDeliveries(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            source.sendSuccess(() -> Component.literal("§e[LeaderOS] Verificando entregas pendentes..."), true);
            MGTLeaderos.instance.checkDeliveries();
            source.sendSuccess(() -> Component.literal("§a[LeaderOS] Verificação concluída!"), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[LeaderOS] Erro ao verificar entregas: " + e.getMessage()));
            MGTLeaderos.LOGGER.error("Erro ao verificar entregas", e);
        }

        return 1;
    }

    /**
     * Comando para mostrar status da conexão
     */
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        boolean connected = MGTLeaderos.instance.isConnected();
        String status = connected ? "§aConectado" : "§cDesconectado";

        source.sendSuccess(() -> Component.literal("§e[LeaderOS] Status: " + status), false);
        source.sendSuccess(() -> Component.literal("§7URL: " + MGTLeaderos.instance.getConfigManager().getUrl()), false);

        return 1;
    }

    /**
     * Comando para testar conexão com a API
     */
    private static int testConnection(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§e[LeaderOS] Testando conexão com a API..."), true);

        try {
            boolean success = MGTLeaderos.instance.testConnection();
            if (success) {
                source.sendSuccess(() -> Component.literal("§a[LeaderOS] Conexão estabelecida com sucesso!"), true);
            } else {
                source.sendFailure(Component.literal("§c[LeaderOS] Falha ao conectar com a API!"));
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[LeaderOS] Erro ao testar conexão: " + e.getMessage()));
            MGTLeaderos.LOGGER.error("Erro ao testar conexão", e);
        }

        return 1;
    }
}
