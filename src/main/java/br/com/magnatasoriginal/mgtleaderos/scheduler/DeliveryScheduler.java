package br.com.magnatasoriginal.mgtleaderos.scheduler;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import br.com.magnatasoriginal.mgtleaderos.delivery.DeliveryManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Agendador de tarefas periódicas
 * Executa verificações de entregas pendentes em intervalos regulares
 */
public class DeliveryScheduler {
    private final ScheduledExecutorService scheduler;
    private final DeliveryManager deliveryManager;
    private final int intervalSeconds;

    public DeliveryScheduler(DeliveryManager deliveryManager, int intervalSeconds) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.deliveryManager = deliveryManager;
        this.intervalSeconds = intervalSeconds;
    }

    /**
     * Inicia o agendamento de verificações periódicas
     */
    public void start() {
        MGTLeaderos.LOGGER.info("Iniciando verificação de entregas a cada " + intervalSeconds + " segundos.");

        scheduler.scheduleAtFixedRate(() -> {
            try {
                MGTLeaderos.LOGGER.debug("Verificando entregas pendentes...");
                deliveryManager.checkAndDeliverPurchases();
            } catch (Exception e) {
                MGTLeaderos.LOGGER.error("Erro durante verificação de entregas: " + e.getMessage());
                e.printStackTrace();
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Para o agendador
     */
    public void stop() {
        MGTLeaderos.LOGGER.info("Parando verificação de entregas.");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}

