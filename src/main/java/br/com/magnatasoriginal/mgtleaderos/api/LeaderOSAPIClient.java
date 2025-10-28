package br.com.magnatasoriginal.mgtleaderos.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente da API do LeaderOS
 * Responsável por fazer as requisições HTTP para o servidor web
 * Implementação fiel ao plugin Bukkit original
 */
public class LeaderOSAPIClient {
    private final String baseUrl;
    private final String apiKey;
    private final String serverToken;

    public LeaderOSAPIClient(String baseUrl, String apiKey, String serverToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.serverToken = serverToken;

        MGTLeaderos.LOGGER.info("API Client inicializado:");
        MGTLeaderos.LOGGER.info("  URL: " + this.baseUrl);
        MGTLeaderos.LOGGER.info("  API Key: " + (apiKey.isEmpty() ? "(vazio)" : apiKey.substring(0, Math.min(8, apiKey.length())) + "..."));
        MGTLeaderos.LOGGER.info("  Server Token: " + (serverToken.isEmpty() ? "(vazio)" : serverToken.substring(0, Math.min(8, serverToken.length())) + "..."));
    }

    /**
     * Testa a conexão com a API
     * ATENÇÃO: O plugin original não faz verificação HTTP para /api/server/verify.
     * Toda autenticação é feita via WebSocket (SocketClient).
     * Este método pode ser removido ou adaptado para uso futuro em módulos que realmente usam HTTP.
     */
    public boolean testConnection() {
        MGTLeaderos.LOGGER.info("[INFO] O plugin original LeaderOS NÃO faz verificação HTTP para /api/server/verify. Autenticação é via WebSocket.");
        // TODO: Implementar autenticação via WebSocket conforme ConnectModule do plugin original
        return true;
    }

    /**
     * Busca as compras pendentes de entrega
     * Plugin original usa /api/server/queue
     */
    public JsonArray getPendingDeliveries() {
        try {
            JsonObject requestData = new JsonObject();
            requestData.addProperty("serverToken", serverToken);

            // Endpoint correto do plugin original
            JsonObject response = makeRequest("/api/server/queue", "POST", requestData);

            if (response != null && response.has("data")) {
                return response.getAsJsonArray("data");
            }
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("Erro ao buscar entregas pendentes: " + e.getMessage());
            e.printStackTrace();
        }
        return new JsonArray();
    }

    /**
     * Confirma a entrega de uma compra
     * Plugin original usa /api/server/queue/complete
     */
    public boolean confirmDelivery(int purchaseId) {
        try {
            JsonObject requestData = new JsonObject();
            requestData.addProperty("serverToken", serverToken);
            requestData.addProperty("id", purchaseId);

            JsonObject response = makeRequest("/api/server/queue/complete", "POST", requestData);
            return response != null && response.has("success") && response.get("success").getAsBoolean();
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("Erro ao confirmar entrega: " + e.getMessage());
            return false;
        }
    }

    /**
     * Faz uma requisição HTTP para a API
     * Implementação idêntica ao plugin Bukkit/Spigot
     */
    private JsonObject makeRequest(String endpoint, String method, JsonObject data) {
        HttpURLConnection connection = null;
        try {
            String fullUrl = baseUrl + endpoint;
            MGTLeaderos.LOGGER.info("[DEBUG] Endpoint: " + fullUrl);
            MGTLeaderos.LOGGER.info("[DEBUG] Payload: " + (data != null ? data.toString() : "(vazio)"));
            MGTLeaderos.LOGGER.info("[DEBUG] Headers: Authorization=" + apiKey + ", Content-Type=application/json, Accept=application/json, User-Agent=LeaderOS-Minecraft/1.0");

            // ABRIR CONEXÃO PRIMEIRO
            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "LeaderOS-Minecraft/1.0");
            connection.setRequestProperty("Authorization", apiKey);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoInput(true);

            // Se houver dados para enviar
            if (data != null) {
                connection.setDoOutput(true);
                String jsonData = data.toString();
                MGTLeaderos.LOGGER.info("Enviando dados: " + jsonData);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Ler resposta
            int responseCode = connection.getResponseCode();
            MGTLeaderos.LOGGER.info("Código de resposta: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    String responseStr = response.toString();
                    MGTLeaderos.LOGGER.info("Resposta da API: " + responseStr);
                    return JsonParser.parseString(responseStr).getAsJsonObject();
                }
            } else {
                // Tentar ler mensagem de erro
                String errorMessage = "Sem mensagem de erro";
                try {
                    if (connection.getErrorStream() != null) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                            StringBuilder error = new StringBuilder();
                            String errorLine;
                            while ((errorLine = br.readLine()) != null) {
                                error.append(errorLine.trim());
                            }
                            errorMessage = error.toString();
                        }
                    }
                } catch (Exception e) {
                    errorMessage = "Erro ao ler mensagem: " + e.getMessage();
                }

                MGTLeaderos.LOGGER.error("===== ERRO HTTP =====");
                MGTLeaderos.LOGGER.error("Código: " + responseCode);
                MGTLeaderos.LOGGER.error("URL: " + fullUrl);
                MGTLeaderos.LOGGER.error("Método: " + method);
                MGTLeaderos.LOGGER.error("Mensagem: " + errorMessage);
                MGTLeaderos.LOGGER.error("====================");
            }
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("Erro ao fazer requisição para API: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }
}
