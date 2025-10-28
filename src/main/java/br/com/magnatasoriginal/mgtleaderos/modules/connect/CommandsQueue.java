package br.com.magnatasoriginal.mgtleaderos.modules.connect;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gerenciador de fila de comandos pendentes
 * Armazena comandos para jogadores offline e executa quando entram
 * Implementação fiel ao plugin Bukkit/Spigot original
 *
 * @author Conversão de Bukkit para NeoForge
 */
public class CommandsQueue {
    private final File queueFile;
    private final Gson gson;
    private final ExecutorService executor;
    private Map<String, List<String>> commandsMap;

    /**
     * Construtor do CommandsQueue
     * @param dataFolder Pasta de dados do mod
     */
    public CommandsQueue(String dataFolder) {
        this.queueFile = new File(dataFolder, "commands_queue.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.executor = Executors.newSingleThreadExecutor();
        this.commandsMap = new HashMap<>();

        // Criar arquivo se não existir
        if (!queueFile.exists()) {
            try {
                queueFile.getParentFile().mkdirs();
                queueFile.createNewFile();
                saveQueue();
                MGTLeaderos.LOGGER.info("[LeaderOS] Arquivo de fila de comandos criado: " + queueFile.getAbsolutePath());
            } catch (Exception e) {
                MGTLeaderos.LOGGER.error("[LeaderOS] Erro ao criar arquivo de fila de comandos: " + e.getMessage());
            }
        }

        // Carregar fila existente
        loadQueue();
    }

    /**
     * Carrega a fila de comandos do arquivo JSON
     */
    private void loadQueue() {
        try (FileReader reader = new FileReader(queueFile)) {
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                commandsMap = loaded;
                MGTLeaderos.LOGGER.info("[LeaderOS] Fila de comandos carregada: " + commandsMap.size() + " jogadores com comandos pendentes");
            }
        } catch (Exception e) {
            MGTLeaderos.LOGGER.warn("[LeaderOS] Não foi possível carregar fila de comandos (arquivo pode estar vazio): " + e.getMessage());
            commandsMap = new HashMap<>();
        }
    }

    /**
     * Salva a fila de comandos no arquivo JSON
     */
    private void saveQueue() {
        try (FileWriter writer = new FileWriter(queueFile)) {
            gson.toJson(commandsMap, writer);
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS] Erro ao salvar fila de comandos: " + e.getMessage());
        }
    }

    /**
     * Adiciona comandos à fila de um jogador
     * @param playerName Nome do jogador
     * @param commands Lista de comandos a adicionar
     */
    public void addCommands(String playerName, List<String> commands) {
        executor.execute(() -> {
            List<String> existingCommands = commandsMap.getOrDefault(playerName, new ArrayList<>());
            existingCommands.addAll(commands);
            commandsMap.put(playerName, existingCommands);
            saveQueue();
            MGTLeaderos.LOGGER.info("[LeaderOS] Adicionados " + commands.size() + " comandos à fila de " + playerName);
        });
    }

    /**
     * Obtém os comandos pendentes de um jogador
     * @param playerName Nome do jogador
     * @return Lista de comandos ou null se não houver
     */
    public List<String> getCommands(String playerName) {
        return commandsMap.get(playerName);
    }

    /**
     * Remove os comandos da fila de um jogador
     * @param playerName Nome do jogador
     */
    public void removeCommands(String playerName) {
        executor.execute(() -> {
            commandsMap.remove(playerName);
            saveQueue();
            MGTLeaderos.LOGGER.info("[LeaderOS] Comandos de " + playerName + " removidos da fila");
        });
    }

    /**
     * Retorna o executor de tarefas assíncronas
     * @return ExecutorService
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Finaliza o executor
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            MGTLeaderos.LOGGER.info("[LeaderOS] Executor de fila de comandos finalizado");
        }
    }

    /**
     * Retorna uma lista dos jogadores com comandos pendentes
     * @return Lista de nomes de jogadores
     */
    public List<String> getPlayersWithPendingCommands() {
        return new ArrayList<>(commandsMap.keySet());
    }
}
