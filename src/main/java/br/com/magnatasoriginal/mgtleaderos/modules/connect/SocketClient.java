package br.com.magnatasoriginal.mgtleaderos.modules.connect;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente WebSocket simples sem reconexão manual
 * O servidor Pusher gerencia reconexão automaticamente
 * Código 4201 é normal (timeout de inatividade)
 *
 * @author Conversão de Bukkit para NeoForge
 */
public abstract class SocketClient extends WebSocketClient {
    private final String apiKey;
    private final String serverToken;
    private final String baseUrl;
    private final ConnectModule connectModule;
    private volatile boolean subscribed = false;
    private String socketId = null;

    public SocketClient(String baseUrl, String apiKey, String serverToken, ConnectModule connectModule) {
        super(URI.create("wss://connect-socket.leaderos.net:6002/app/leaderos-connect?protocol=7&client=java&version=1.0.0"));
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.serverToken = serverToken;
        this.connectModule = connectModule;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        // Conectado - aguardar socket ID
    }

    @Override
    public void onMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String event = json.getString("event");

            // Socket ID recebido
            if (event.equals("pusher:connection_established")) {
                JSONObject data = new JSONObject(json.getString("data"));
                socketId = data.getString("socket_id");

                // Autenticar canal privado
                authenticateChannel();
            }
            // Autenticação bem-sucedida
            else if (event.equals("pusher_internal:subscription_succeeded")) {
                if (!subscribed) {
                    subscribed = true;
                    MGTLeaderos.LOGGER.info("[LeaderOS Connect] Conectado ao servidor via Pusher");
                    subscribed();
                }
            }
            // Comandos recebidos
            else if (event.equals("send-commands")) {
                handleCommandsEvent(json);
            }
            // Ping do servidor Pusher
            else if (event.equals("pusher:ping")) {
                // Responder com pusher:pong para manter a conexão viva (igual ao cliente oficial)
                try {
                    JSONObject pong = new JSONObject();
                    pong.put("event", "pusher:pong");
                    pong.put("data", new JSONObject());
                    send(pong.toString());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao processar mensagem: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        subscribed = false;
        // Delegar lógica de reconexão ao ConnectModule para controle centralizado
        try {
            if (connectModule != null) {
                connectModule.handleDisconnect(code, reason);
            }
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao notificar desconexão: " + e.getMessage());
        }
    }

    @Override
    public void onError(Exception ex) {
        MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro na conexão: " + ex.getMessage());
    }

    /**
     * Autentica o canal privado do servidor
     */
    private void authenticateChannel() {
        try {
            String channelName = "private-servers." + serverToken;

            // Fazer requisição de autenticação
            String authUrl = "https://connect-api.leaderos.net/broadcasting/auth";
            String postData = "socket_id=" + java.net.URLEncoder.encode(socketId, "UTF-8") +
                            "&channel_name=" + java.net.URLEncoder.encode(channelName, "UTF-8");

            URL url = new URL(authUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("X-Api-Key", apiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject authResponse = new JSONObject(response.toString());
                String auth = authResponse.getString("auth");

                // Subscrever ao canal com autenticação
                JSONObject subscribeMsg = new JSONObject();
                subscribeMsg.put("event", "pusher:subscribe");
                JSONObject subData = new JSONObject();
                subData.put("auth", auth);
                subData.put("channel", channelName);
                subscribeMsg.put("data", subData);

                send(subscribeMsg.toString());
            } else {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Falha na autenticação. Código: {}", responseCode);
            }
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao autenticar: " + e.getMessage());
        }
    }

    /**
     * Processa evento de comandos recebidos
     */
    private void handleCommandsEvent(JSONObject event) {
        try {
            String jsonString = event.getString("data");
            JSONObject data = new JSONObject(jsonString);
            JSONArray logIDs = data.getJSONArray("commands");

            // Validar comandos via API
            StringBuilder formData = new StringBuilder();
            formData.append("token=").append(java.net.URLEncoder.encode(serverToken, "UTF-8"));

            for (int i = 0; i < logIDs.length(); i++) {
                formData.append("&commands[").append(i).append("]=")
                        .append(java.net.URLEncoder.encode(logIDs.getString(i), "UTF-8"));
            }

            String validateUrl = baseUrl + "/api/command-logs/validate";

            try {
                URL url = new URL(validateUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("X-Api-Key", apiKey);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = formData.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201 || responseCode == 202) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject responseObj = new JSONObject(response.toString());
                    JSONArray commandsJSON = responseObj.getJSONArray("commands");
                    String username = "";
                    List<String> commandsList = new ArrayList<>();

                    for (int i = 0; i < commandsJSON.length(); i++) {
                        JSONObject cmdObj = commandsJSON.getJSONObject(i);
                        String command = cmdObj.getString("command");
                        commandsList.add(command);

                        if (username.isEmpty()) {
                            username = cmdObj.getString("username");
                        }
                    }

                    if (!commandsList.isEmpty() && !username.isEmpty()) {
                        executeCommands(commandsList, username);
                    }
                }
            } catch (Exception e) {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao validar comandos: " + e.getMessage());
            }

        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao processar comandos: " + e.getMessage());
        }
    }

    public abstract void executeCommands(List<String> commands, String username);
    public abstract void subscribed();

    public boolean isSubscribed() {
        return subscribed && isOpen();
    }

    // Novo método público para compatibilidade com chamadas legadas
    // Retorna true se a conexão WebSocket estiver aberta
    public boolean isConnected() {
        return isOpen();
    }

    // Expõe o socket ID (útil para debug/validação)
    public String getSocketId() {
        return socketId;
    }
}
