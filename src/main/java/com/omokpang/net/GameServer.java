package com.omokpang.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 역할: OmokPang 서버 (콘솔 프로그램)
 *  - 여러 클라이언트의 접속을 받고, 간단한 메시지를 에코하는 예제.
 *  - 실제 게임 로직(매칭, 턴 관리 등)은 여기에서 확장.
 */
public class GameServer {

    public static void main(String[] args) throws Exception {
        int port = 9000;
        System.out.println("[SERVER] OmokPang Server start on port " + port);

        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            // 새 클라이언트 접속 대기
            Socket clientSocket = serverSocket.accept();
            System.out.println("[SERVER] New client connected: " + clientSocket);

            // 클라이언트마다 스레드 분리
            Thread t = new Thread(() -> handleClient(clientSocket));
            t.start();
        }
    }

    private static void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // 간단 테스트: 클라이언트에서 오는 문자열을 그대로 다시 보냄
            out.println("WELCOME OmokPang!");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[SERVER] recv: " + line);
                // TODO: 여기에서 "JOIN", "PLACE x y", "CHEER msg" 등 프로토콜 처리
                out.println("ECHO: " + line);
            }
        } catch (Exception e) {
            System.out.println("[SERVER] client disconnected: " + socket);
        }
    }
}
