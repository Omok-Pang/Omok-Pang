package com.omokpang.net;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * 역할: OmokPang 클라이언트 네트워크 모듈 (싱글톤).
 *  - JavaFX 앱 내에서 한 번만 connect() 호출해서 계속 재사용.
 *  - 서버와 send/receive 담당.
 */
public class OmokClient {

    private static OmokClient instance;

    public static OmokClient getInstance() {
        if (instance == null) {
            instance = new OmokClient();
        }
        return instance;
    }

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean connected = false;

    // 서버 → 클라이언트로 오는 문자열을 UI 쪽으로 전달하기 위한 핸들러
    private Consumer<String> messageHandler;

    private OmokClient() {}

    /** UI(예: GameBoardController)에서 서버 메시지를 받기 위해 등록 */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    /** 서버에 연결. 이미 연결되어 있으면 그냥 리턴. */
    public void connect(String host, int port) throws Exception {
        if (connected) return;

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        connected = true;

        System.out.println("[CLIENT] Connected to " + host + ":" + port);

        // 🔁 서버로부터 계속 읽는 스레드 시작
        Thread listener = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    System.out.println("[CLIENT] recv: " + line);
                    if (messageHandler != null) {
                        String msg = line;  // ✅ 람다에서 쓸 별도 final 변수
                        Platform.runLater(() -> messageHandler.accept(msg));
                    }
                }
            } catch (Exception e) {
                System.out.println("[CLIENT] connection closed.");
                connected = false;
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    /** 간단 문자열 전송 */
    public void send(String msg) {
        if (!connected) return;
        out.println(msg);
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
