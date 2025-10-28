package br.com.magnatasoriginal.mgtleaderos.modules.connect;

/**
 * Stub para PusherSocketClient
 * A versão atual do mod utiliza `SocketClient` + `LeaderOSSocketClient` (implementação WebSocket)
 * que fazem autenticação e execução de comandos sem depender da biblioteca oficial Pusher.
 *
 * Se desejar usar a biblioteca oficial do Pusher, reimplemente esta classe com a API
 * correta e ajuste as dependências no build.gradle.
 *
 * TODO (PT-BR): Reimplementar cliente oficial Pusher se necessário.
 */
public abstract class PusherSocketClient {
    // Classe stub - mantida para compatibilidade de nomes no código legado
    public abstract boolean isConnected();
    public abstract void disconnect();
}
