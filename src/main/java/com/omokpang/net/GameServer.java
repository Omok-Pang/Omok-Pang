package com.omokpang.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 역할: OmokPang 서버 (콘솔 프로그램)
 *  - 여러 클라이언트의 접속을 받고,
 *    LOGIN / QUEUE / MATCH 정도만 처리하는 간단 매칭 서버.
 */
public class GameServer {

    // 닉네임 -> 해당 클라이언트 출력 스트림
    private static final Map<String, PrintWriter> clientMap = new ConcurrentHashMap<>();

    // 1:1 매칭 대기열 (닉네임만 저장)
    private static final Queue<String> queue1v1 = new ArrayDeque<>();

    public static void main(String[] args) throws Exception {
        int port = 9000;
        System.out.println("[SERVER] OmokPang Server start on port " + port);

        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("[SERVER] New client connected: " + clientSocket);

            Thread t = new Thread(() -> handleClient(clientSocket));
            t.start();
        }
    }

    private static void handleClient(Socket socket) {
        String nickname = null;

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("WELCOME OmokPang!");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[SERVER] recv: " + line);

                // --------------------
                //  LOGIN 처리
                // --------------------
                if (line.startsWith("LOGIN ")) {
                    nickname = line.substring("LOGIN ".length()).trim();
                    clientMap.put(nickname, out);
                    System.out.println("[SERVER] LOGIN: " + nickname);
                    continue;
                }

                // --------------------
                //  QUEUE 1v1 처리
                //  형식: QUEUE 1v1 닉네임
                // --------------------
                if (line.startsWith("QUEUE 1v1")) {
                    String[] parts = line.split("\\s+", 3);
                    if (parts.length >= 3) {
                        String nick = parts[2].trim();
                        enqueue1v1(nick);
                    }
                    continue;
                }

                // 기타: 테스트용 에코
                out.println("ECHO: " + line);
            }
        } catch (Exception e) {
            System.out.println("[SERVER] client disconnected: " + socket);
        } finally {
            // 연결 종료 시 맵에서 제거
            if (nickname != null) {
                clientMap.remove(nickname);
            }
        }
    }

    // 1:1 대기열에 넣고, 2명 모이면 MATCH 보내기
    private static synchronized void enqueue1v1(String nick) {
        // 이미 대기열에 있는지 간단히 체크
        if (queue1v1.contains(nick)) {
            return;
        }

        queue1v1.add(nick);
        System.out.println("[SERVER] QUEUE 1v1: " + nick +
                " (현재 대기: " + queue1v1.size() + ")");

        // 두 명 이상 모이면 매칭
        if (queue1v1.size() >= 2) {
            String a = queue1v1.poll();
            String b = queue1v1.poll();

            PrintWriter outA = clientMap.get(a);
            PrintWriter outB = clientMap.get(b);

            if (outA != null && outB != null) {
                String matchMsg = "MATCH 1v1 " + a + "," + b;
                outA.println(matchMsg);
                outB.println(matchMsg);
                System.out.println("[SERVER] MATCHED 1v1: " + matchMsg);
            }
        }
    }
}
