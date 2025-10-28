package br.com.magnatasoriginal.mgtleaderos.api.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Modelo de dados para uma compra/entrega
 * Representa um item ou conjunto de itens a serem entregues ao jogador
 */
public class Purchase {
    private final int id;
    private final String playerUuid;
    private final String playerName;
    private final JsonArray items;
    private final JsonArray commands;
    private final String type;

    public Purchase(JsonObject data) {
        this.id = data.has("id") ? data.get("id").getAsInt() : 0;
        this.playerUuid = data.has("player_uuid") ? data.get("player_uuid").getAsString() : "";
        this.playerName = data.has("player_name") ? data.get("player_name").getAsString() : "";
        this.items = data.has("items") ? data.getAsJsonArray("items") : new JsonArray();
        this.commands = data.has("commands") ? data.getAsJsonArray("commands") : new JsonArray();
        this.type = data.has("type") ? data.get("type").getAsString() : "item";
    }

    public int getId() {
        return id;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public JsonArray getItems() {
        return items;
    }

    public JsonArray getCommands() {
        return commands;
    }

    public String getType() {
        return type;
    }

    public boolean isValid() {
        return id > 0 && !playerUuid.isEmpty();
    }
}

