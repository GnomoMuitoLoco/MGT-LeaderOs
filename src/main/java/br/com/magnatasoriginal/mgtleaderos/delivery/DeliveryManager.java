package br.com.magnatasoriginal.mgtleaderos.delivery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import br.com.magnatasoriginal.mgtleaderos.api.LeaderOSAPIClient;
import br.com.magnatasoriginal.mgtleaderos.api.model.Purchase;
import br.com.magnatasoriginal.mgtleaderos.utils.NBTUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Gerenciador de entregas de compras
 * Processa as compras pendentes e entrega os itens aos jogadores
 */
public class DeliveryManager {
    private final LeaderOSAPIClient apiClient;
    private final MinecraftServer server;

    public DeliveryManager(LeaderOSAPIClient apiClient, MinecraftServer server) {
        this.apiClient = apiClient;
        this.server = server;
    }

    /**
     * Verifica e processa as entregas pendentes
     */
    public void checkAndDeliverPurchases() {
        try {
            JsonArray pendingDeliveries = apiClient.getPendingDeliveries();

            if (pendingDeliveries.size() == 0) {
                MGTLeaderos.LOGGER.debug("Nenhuma entrega pendente encontrada.");
                return;
            }

            MGTLeaderos.LOGGER.info("Encontradas " + pendingDeliveries.size() + " entregas pendentes.");

            for (JsonElement element : pendingDeliveries) {
                JsonObject purchase = element.getAsJsonObject();
                processPurchase(purchase);
            }
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("Erro ao verificar entregas pendentes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processa uma compra individual
     */
    private void processPurchase(JsonObject purchase) {
        try {
            int purchaseId = purchase.get("id").getAsInt();
            String playerUuidStr = purchase.get("player_uuid").getAsString();
            String playerName = purchase.get("player_name").getAsString();
            JsonArray items = purchase.has("items") ? purchase.getAsJsonArray("items") : new JsonArray();
            JsonArray commands = purchase.has("commands") ? purchase.getAsJsonArray("commands") : new JsonArray();

            UUID playerUuid = UUID.fromString(playerUuidStr);
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);

            // Se o jogador está online, entregar imediatamente
            if (player != null) {
                boolean success = deliverToPlayer(player, items, commands);

                if (success) {
                    // Confirmar entrega na API
                    if (apiClient.confirmDelivery(purchaseId)) {
                        MGTLeaderos.LOGGER.info("Entrega confirmada para jogador " + playerName + " (ID: " + purchaseId + ")");
                        player.sendSystemMessage(Component.literal("§a[LeaderOS] Você recebeu sua compra!"));
                    } else {
                        MGTLeaderos.LOGGER.error("Falha ao confirmar entrega na API (ID: " + purchaseId + ")");
                    }
                } else {
                    MGTLeaderos.LOGGER.error("Falha ao entregar itens para " + playerName);
                }
            } else {
                MGTLeaderos.LOGGER.info("Jogador " + playerName + " não está online. Entrega será feita quando ele entrar.");
            }
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("Erro ao processar compra: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Entrega os itens e executa comandos para o jogador
     */
    private boolean deliverToPlayer(ServerPlayer player, JsonArray items, JsonArray commands) {
        boolean success = true;

        // Entregar itens
        for (JsonElement itemElement : items) {
            JsonObject itemData = itemElement.getAsJsonObject();
            if (!giveItem(player, itemData)) {
                success = false;
            }
        }

        // Executar comandos
        for (JsonElement cmdElement : commands) {
            String command = cmdElement.getAsString();
            executeCommand(command, player);
        }

        return success;
    }

    /**
     * Dá um item ao jogador
     */
    private boolean giveItem(ServerPlayer player, JsonObject itemData) {
        try {
            String itemId = itemData.get("item").getAsString();
            int amount = itemData.has("amount") ? itemData.get("amount").getAsInt() : 1;

            // Criar o ItemStack
            ResourceLocation itemResource = ResourceLocation.parse(itemId);
            var item = BuiltInRegistries.ITEM.get(itemResource);

            if (item == null) {
                MGTLeaderos.LOGGER.error("Item não encontrado: " + itemId);
                return false;
            }

            ItemStack stack = new ItemStack(item, amount);

            // Se tiver NBT, aplicar
            // Nota: Em 1.21.1, o sistema de NBT foi substituído por DataComponents
            // Implementação futura irá suportar DataComponents completamente
            if (itemData.has("nbt")) {
                String nbtString = itemData.get("nbt").getAsString();
                MGTLeaderos.LOGGER.warn("NBT/DataComponent ainda não implementado. Item será entregue sem customizações: " + nbtString);
                // TODO: Implementar conversão de NBT para DataComponents
            }

            // Adicionar ao inventário do jogador
            if (!player.getInventory().add(stack)) {
                // Se não couber no inventário, dropar no chão
                player.drop(stack, false);
                MGTLeaderos.LOGGER.warn("Inventário cheio, item dropado no chão para " + player.getName().getString());
            }

            MGTLeaderos.LOGGER.info("Item entregue: " + itemId + " x" + amount + " para " + player.getName().getString());
            return true;
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("Erro ao dar item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Executa um comando substituindo placeholders
     */
    private void executeCommand(String command, ServerPlayer player) {
        try {
            // Substituir placeholders
            command = command.replace("{player}", player.getName().getString())
                           .replace("{uuid}", player.getUUID().toString());

            // Executar comando no servidor
            CommandSourceStack source = server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source, command);

            MGTLeaderos.LOGGER.info("Comando executado: " + command);
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("Erro ao executar comando: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

