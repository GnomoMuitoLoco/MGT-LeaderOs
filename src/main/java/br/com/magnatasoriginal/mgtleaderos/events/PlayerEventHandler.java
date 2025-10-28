package br.com.magnatasoriginal.mgtleaderos.events;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Manipulador de eventos de jogador
 * Verifica entregas pendentes quando um jogador entra no servidor
 */
@EventBusSubscriber(modid = MGTLeaderos.MODID)
public class PlayerEventHandler {

    /**
     * Chamado quando um jogador entra no servidor
     * Verifica se há entregas pendentes para este jogador
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MGTLeaderos.LOGGER.info("Jogador " + player.getName().getString() + " entrou. Verificando entregas pendentes...");

            // Agendar verificação de entregas após 3 segundos (dar tempo do jogador carregar)
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    MGTLeaderos.getInstance().checkDeliveries();
                } catch (InterruptedException e) {
                    MGTLeaderos.LOGGER.error("Erro ao aguardar para verificar entregas", e);
                }
            }).start();
        }
    }
}

