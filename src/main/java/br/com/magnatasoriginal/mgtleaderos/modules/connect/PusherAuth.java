package br.com.magnatasoriginal.mgtleaderos.modules.connect;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Classe para autenticar canais privados do Pusher
 * Replica o fluxo do CustomHttpAuthorizer do plugin original
 */
public class PusherAuth {
    private static final String AUTH_ENDPOINT = "https://connect-api.leaderos.net/broadcasting/auth";
    private final String apiKey;
    private final HttpClient httpClient;

    public PusherAuth(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Autentica um canal privado no Pusher
     * Igual ao CustomHttpAuthorizer do plugin original
     * 
     * @param channelName Nome do canal (ex: private-servers.{serverToken})
     * @param socketId ID do socket fornecido pelo Pusher
     * @return String de autenticação (auth) ou null se falhar
     */
    public String authorize(String channelName, String socketId) {
        try {
            // Montar payload igual ao plugin
            String payload = String.format("socket_id=%s&channel_name=%s", socketId, channelName);
            
            // Fazer requisição POST igual ao plugin
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("X-Api-Key", apiKey)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String auth = jsonResponse.optString("auth", null);
                
                if (auth != null) {
                    MGTLeaderos.LOGGER.info("[LeaderOS Connect] Autenticação do canal bem-sucedida!");
                    return auth;
                } else {
                    MGTLeaderos.LOGGER.error("[LeaderOS Connect] Resposta de autenticação sem 'auth': " + response.body());
                }
            } else {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Falha na autenticação. Status: " + response.statusCode() + ", Body: " + response.body());
            }
            
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao autenticar canal: " + e.getMessage());
            if (MGTLeaderos.LOGGER.isDebugEnabled()) {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Stack trace:", e);
            }
        }
        
        return null;
    }
}

