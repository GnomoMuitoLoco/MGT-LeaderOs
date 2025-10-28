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
 * Cliente WebSocket que replica o protocolo Pusher do plugin original
 * Conecta ao LeaderOS usando os mesmos parâmetros e fluxo do plugin Bukkit/Spigot
 *
 * Configuração igual ao plugin:
 * - Host: connect-socket.leaderos.net
 * - Porta: 6002 (WSS)
 * - AppKey: leaderos-connect
 * - Cluster: eu
 * - Canal: private-servers.{serverToken}
 * - Eventos: send-commands, ping
 */
public abstract class SocketClient {
    private WebSocketClient client;
    private final String apiKey;
    private final String serverToken;
    private final PusherAuth pusherAuth;
    private final ConnectModule connectModule;
    private String socketId;
    private boolean connected = false;
    private boolean subscribed = false;

    // Configurações do Pusher (igual ao plugin)
    private static final String APP_KEY = "leaderos-connect";
    private static final String HOST = "connect-socket.leaderos.net";
    private static final int WSS_PORT = 6002;
    private static final String PROTOCOL_VERSION = "7";

    public SocketClient(String apiKey, String serverToken, boolean onlyOnline, ConnectModule connectModule) {
        this.apiKey = apiKey;
        this.serverToken = serverToken;
        this.pusherAuth = new PusherAuth(apiKey);
        this.connectModule = connectModule;
    }

    public void connect() {
        try {
            if (client != null) {
                try { client.closeBlocking(); } catch (Exception ignored) {}
                client = null;
            }

            // URL do Pusher igual ao plugin
            String wsUrl = String.format("wss://%s:%d/app/%s?protocol=%s&client=java&version=1.0.0",
                HOST, WSS_PORT, APP_KEY, PROTOCOL_VERSION);

            MGTLeaderos.LOGGER.info("[LeaderOS Connect] Conectando ao Pusher: " + wsUrl);

            client = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    MGTLeaderos.LOGGER.info("[LeaderOS Connect] Conexão WebSocket estabelecida!");
                    connected = true;
                    subscribed = false;
                }

                @Override
                public void onMessage(String message) {
                    handlePusherMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    subscribed = false;
                    MGTLeaderos.LOGGER.warn("[LeaderOS Connect] Conexão fechada. Código: " + code + ", Razão: " + reason);
                    // Chama reconexão no ConnectModule
                    if (connectModule != null) {
                        connectModule.onWebSocketClosed(code, reason);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro na conexão: " + ex.getMessage());
                }
            };

            client.connect();

        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao conectar: " + e.getMessage());
            if (MGTLeaderos.LOGGER.isDebugEnabled()) {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Stack trace detalhado:", e);
            }
        }
    }

    /**
     * Processa mensagens do Pusher (igual ao plugin original)
     */
    private void handlePusherMessage(String message) {
        try {
            JSONObject data = new JSONObject(message);
            String event = data.optString("event", "");

            switch (event) {
                case "pusher:connection_established":
                    // Conexão estabelecida, extrair socketId
                    JSONObject connData = new JSONObject(data.getString("data"));
                    socketId = connData.getString("socket_id");
                    MGTLeaderos.LOGGER.info("[LeaderOS Connect] Socket ID: " + socketId);

                    // Subscrever ao canal privado
                    subscribeToChannel();
                    break;

                case "pusher_internal:subscription_succeeded":
                    // Subscrição bem-sucedida
                    subscribed = true;
                    MGTLeaderos.LOGGER.info("[LeaderOS Connect] Subscrição ao canal bem-sucedida!");
                    subscribed();
                    break;

                case "send-commands":
                    // Evento de comandos recebidos
                    handleCommandsEvent(data);
                    break;

                case "ping":
                    // Evento de ping (checagem de conexão)
                    MGTLeaderos.LOGGER.info("[LeaderOS Connect] Ping recebido do servidor");
                    subscribed();
                    break;

                case "pusher:error":
                    MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro do Pusher: " + data.toString());
                    break;
            }

        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao processar mensagem: " + e.getMessage());
        }
    }

    /**
     * Subscreve ao canal privado do servidor (igual ao plugin)
     */
    private void subscribeToChannel() {
        try {
            String channelName = "private-servers." + serverToken;

            // Autenticar canal privado usando PusherAuth (igual ao plugin)
            String auth = pusherAuth.authorize(channelName, socketId);

            if (auth == null) {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Falha ao autenticar canal privado");
                return;
            }

            // Montar mensagem de subscrição com auth
            JSONObject subscribeMsg = new JSONObject();
            subscribeMsg.put("event", "pusher:subscribe");

            JSONObject subData = new JSONObject();
            subData.put("channel", channelName);
            subData.put("auth", auth);

            subscribeMsg.put("data", subData);

            client.send(subscribeMsg.toString());
            MGTLeaderos.LOGGER.info("[LeaderOS Connect] Subscrevendo ao canal: " + channelName);

        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao subscrever: " + e.getMessage());
        }
    }

    /**
     * Processa evento de comandos (igual ao plugin original)
     * Valida os IDs de comando via API e extrai comandos formatados + username
     */
    private void handleCommandsEvent(JSONObject event) {
        try {
            String jsonString = event.getString("data");
            JSONObject data = new JSONObject(jsonString);
            JSONArray logIDs = data.getJSONArray("commands");

            // Validar comandos via API (igual ao plugin original)
            // Construir form data para validação (não JSON!)
            StringBuilder formData = new StringBuilder();
            formData.append("token=").append(java.net.URLEncoder.encode(serverToken, "UTF-8"));

            for (int i = 0; i < logIDs.length(); i++) {
                formData.append("&commands[").append(i).append("]=")
                        .append(java.net.URLEncoder.encode(logIDs.getString(i), "UTF-8"));
            }

            // URL correta: usar URL base do site configurado
            String baseUrl = connectModule.getBaseUrl(); // Deve retornar a URL configurada (ex: https://servidormagnatas.com.br)
            String validateUrl = baseUrl + "/api/command-logs/validate";

            MGTLeaderos.LOGGER.info("[LeaderOS Connect] Validando {} comandos via API...", logIDs.length());

            try {
                URL url = new URL(validateUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // FORM, não JSON!
                conn.setRequestProperty("X-Api-Key", apiKey);
                conn.setDoOutput(true);

                // Enviar form data
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = formData.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Ler resposta
                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201 || responseCode == 202) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Parsear resposta e extrair comandos + username
                    JSONObject responseObj = new JSONObject(response.toString());
                    JSONArray commandsJSON = responseObj.getJSONArray("commands");
                    String username = "";
                    List<String> commandsList = new ArrayList<>();

                    for (int i = 0; i < commandsJSON.length(); i++) {
                        JSONObject cmdObj = commandsJSON.getJSONObject(i);
                        String command = cmdObj.getString("command");
                        commandsList.add(command);

                        // Extrair username (todos os comandos têm o mesmo username)
                        if (username.isEmpty()) {
                            username = cmdObj.getString("username");
                        }
                    }

                    // Executar comandos validados
                    if (!commandsList.isEmpty() && !username.isEmpty()) {
                        MGTLeaderos.LOGGER.info("[LeaderOS Connect] Comandos validados para jogador: {}", username);
                        executeCommands(commandsList, username);
                    } else {
                        MGTLeaderos.LOGGER.warn("[LeaderOS Connect] Comandos validados mas sem username ou lista vazia");
                    }
                } else {
                    MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao validar comandos. Código: {}", responseCode);
                    // Ler mensagem de erro
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String errLine;
                    StringBuilder errResponse = new StringBuilder();
                    while ((errLine = errReader.readLine()) != null) {
                        errResponse.append(errLine);
                    }
                    errReader.close();
                    MGTLeaderos.LOGGER.error("[LeaderOS Connect] Resposta de erro: {}", errResponse.toString());
                }
            } catch (Exception e) {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao validar comandos via API: " + e.getMessage());
                if (MGTLeaderos.LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("[LeaderOS Connect] Erro ao processar comandos: " + e.getMessage());
            if (MGTLeaderos.LOGGER.isDebugEnabled()) {
                MGTLeaderos.LOGGER.error("[LeaderOS Connect] Stack trace detalhado:", e);
            }
        }
    }

    public abstract void executeCommands(List<String> commands, String username);
    public abstract void subscribed();

    public boolean isConnected() {
        return connected && subscribed;
    }

    public void disconnect() {
        if (client != null) {
            try {
                client.closeBlocking(); // Aguarda fechamento completo
            } catch (Exception ignored) {}
            client = null;
            connected = false;
            subscribed = false;
        }
    }
}
