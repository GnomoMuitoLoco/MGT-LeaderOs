package br.com.magnatasoriginal.mgtleaderos.config;

// Usar SnakeYAML original - Shadow fará o relocate automaticamente no JAR final
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gerenciador de configurações do mod - Conversão fiel do plugin Bukkit
 * Carrega config.yml e modules.yml no formato OkaeriConfig do plugin original
 *
 * @author Conversão de Bukkit para NeoForge
 */
public class ConfigManager {
    private final File configDir;
    private final Yaml yaml;
    private Map<String, Object> config;
    private Map<String, Object> modules;

    public ConfigManager(File configDir) {
        this.configDir = configDir;
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // Configurar YAML para formato idêntico ao Bukkit
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);

        loadConfigs();
    }

    /**
     * Carrega os arquivos de configuração
     */
    private void loadConfigs() {
        config = loadOrCreateConfig("config.yml", createDefaultConfig());
        modules = loadOrCreateConfig("modules.yml", createDefaultModules());
    }

    /**
     * Carrega ou cria um arquivo de configuração
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadOrCreateConfig(String fileName, Map<String, Object> defaultConfig) {
        File file = new File(configDir, fileName);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Map<String, Object> loaded = yaml.load(fis);
                return loaded != null ? loaded : defaultConfig;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Criar arquivo padrão
        saveConfig(file, defaultConfig);
        return defaultConfig;
    }

    /**
     * Salva a configuração em arquivo
     */
    private void saveConfig(File file, Map<String, Object> config) {
        try (FileWriter writer = new FileWriter(file)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cria a configuração padrão (config.yml)
     * Formato idêntico ao plugin Bukkit
     */
    private Map<String, Object> createDefaultConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        // Settings (igual ao plugin original)
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("lang", "en");
        settings.put("url", "https://yourwebsite.com");
        settings.put("apiKey", "YOUR_API_KEY");
        settings.put("debugMode", "ONLY_ERRORS");
        settings.put("timeFormat", "yyyy-MM-dd HH:mm:ss");

        config.put("settings", settings);
        return config;
    }

    /**
     * Cria a configuração padrão de módulos (modules.yml)
     * Formato idêntico ao plugin Bukkit
     */
    private Map<String, Object> createDefaultModules() {
        Map<String, Object> modules = new LinkedHashMap<>();

        Map<String, Object> connect = new LinkedHashMap<>();
        connect.put("status", true);
        connect.put("serverToken", "");
        modules.put("Connect", connect);

        return modules;
    }

    // Getters para configurações
    @SuppressWarnings("unchecked")
    public String getUrl() {
        if (config.containsKey("settings")) {
            Map<String, Object> settings = (Map<String, Object>) config.get("settings");
            return settings.getOrDefault("url", "").toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public String getApiKey() {
        if (config.containsKey("settings")) {
            Map<String, Object> settings = (Map<String, Object>) config.get("settings");
            return settings.getOrDefault("apiKey", "").toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public boolean isConnectEnabled() {
        if (modules.containsKey("Connect")) {
            Map<String, Object> connect = (Map<String, Object>) modules.get("Connect");
            return (Boolean) connect.getOrDefault("status", false);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public String getServerToken() {
        if (modules.containsKey("Connect")) {
            Map<String, Object> connect = (Map<String, Object>) modules.get("Connect");
            return connect.getOrDefault("serverToken", "").toString();
        }
        return "";
    }

    /**
     * Recarrega as configurações dos arquivos
     */
    public void reload() {
        loadConfigs();
    }

    /**
     * Retorna o diretório de configuração
     */
    public File getConfigDir() {
        return configDir;
    }
}

