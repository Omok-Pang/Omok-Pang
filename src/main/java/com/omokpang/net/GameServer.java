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

    // 🔥 매칭된 상대 매핑 (양방향)
    private static final Map<String, String> opponentMap = new ConcurrentHashMap<>();

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

    // 말풍선 전송: from → 그의 상대에게만
    private static void forwardCheer(String from, String text) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("CHEER " + text);
        }
    }

    // 돌 두기 전송: from → 그의 상대에게만
    private static void forwardPlace(String from, int r, int c) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("PLACE " + r + " " + c);
        }
    }

    // SharedStone 시작 알림: from -> 그의 상대에게만
    private static void forwardSharedStoneStart(String from) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("SHARED_STONE_START");
        }
    }

    // SharedStone 타겟 좌표 전달: from -> 그의 상대에게만
    private static void forwardSharedStoneTarget(String from, int r, int c) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("SHARED_STONE_TARGET " + r + " " + c);
        }
    }

    private static void handleClient(Socket socket) {
        String nickname = null;

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("WELCOME OmokPang!");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[SERVER] recv: " + line);

                if (line.startsWith("LOGIN ")) {
                    nickname = line.substring("LOGIN ".length()).trim();
                    clientMap.put(nickname, out);
                    System.out.println("[SERVER] LOGIN: " + nickname);
                    continue;
                }

                if (line.startsWith("QUEUE 1v1")) {
                    String[] parts = line.split("\\s+", 3);
                    if (parts.length >= 3) {
                        String nick = parts[2].trim();
                        enqueue1v1(nick);
                    }
                    continue;
                }

                // 🔥 말풍선: CHEER <text...>
                if (line.startsWith("CHEER ")) {
                    if (nickname != null) {
                        String text = line.substring("CHEER ".length());
                        forwardCheer(nickname, text);
                    }
                    continue;
                }

                // 🔥 돌 두기: PLACE r c   (예: PLACE 7 8)
                if (line.startsWith("PLACE ")) {
                    if (nickname != null) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            int r = Integer.parseInt(parts[1]);
                            int c = Integer.parseInt(parts[2]);
                            forwardPlace(nickname, r, c);
                        }
                    }
                    continue;
                }

                // 🔥 SharedStone 시작: SHARED_STONE_START
                if (line.startsWith("SHARED_STONE_START")) {
                    if (nickname != null) {
                        forwardSharedStoneStart(nickname);
                    }
                    continue;
                }

                // 🔥 SharedStone 타겟: SHARED_STONE_TARGET r c
                if (line.startsWith("SHARED_STONE_TARGET")) {
                    if (nickname != null) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            int r = Integer.parseInt(parts[1]);
                            int c = Integer.parseInt(parts[2]);
                            forwardSharedStoneTarget(nickname, r, c);
                        }
                    }
                    continue;
                }

                // 🔥 Bomb 시작: BOMB_START
                if (line.startsWith("BOMB_START")) {
                    if (nickname != null) {
                        forwardBombStart(nickname);
                    }
                    continue;
                }

                // 🔥 Bomb 타겟: BOMB_TARGET r c
                if (line.startsWith("BOMB_TARGET")) {
                    if (nickname != null) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            int r = Integer.parseInt(parts[1]);
                            int c = Integer.parseInt(parts[2]);
                            forwardBombTarget(nickname, r, c);
                        }
                    }
                    continue;
                }
                // 🔥 Time Lock 시작: TIMELOCK_START
                if (line.startsWith("TIMELOCK_START")) {
                    if (nickname != null) {
                        forwardTimeLockStart(nickname);
                    }
                    continue;
                }

                // 🔥 Swap 시작: SWAP_START
                if (line.startsWith("SWAP_START")) {
                    if (nickname != null) {
                        forwardSwapStart(nickname);
                    }
                    continue;
                }

                // 🔥 Swap 타겟: SWAP_TARGET myR myC oppR oppC
                if (line.startsWith("SWAP_TARGET")) {
                    if (nickname != null) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 5) {
                            int myR  = Integer.parseInt(parts[1]);
                            int myC  = Integer.parseInt(parts[2]);
                            int oppR = Integer.parseInt(parts[3]);
                            int oppC = Integer.parseInt(parts[4]);
                            forwardSwapTarget(nickname, myR, myC, oppR, oppC);
                        }
                    }
                    continue;
                }

                // 기타: 테스트용 에코
                out.println("ECHO: " + line);
            }
        } catch (Exception e) {
            System.out.println("[SERVER] client disconnected: " + socket);
        } finally {
            if (nickname != null) {
                clientMap.remove(nickname);
                opponentMap.remove(nickname);
            }
        }
    }

    // 1:1 대기열에 넣고, 2명 모이면 MATCH 보내기
    private static synchronized void enqueue1v1(String nick) {
        if (queue1v1.contains(nick)) {
            return;
        }

        queue1v1.add(nick);
        System.out.println("[SERVER] QUEUE 1v1: " + nick +
                " (현재 대기: " + queue1v1.size() + ")");

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

                // 🔥 서로의 상대를 등록 (양방향)
                opponentMap.put(a, b);
                opponentMap.put(b, a);
            }
        }
    }

    // Bomb!! 시작 알림
    private static void forwardBombStart(String from) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("BOMB_START");
        }
    }

    // Bomb!! 타겟 좌표 전달
    private static void forwardBombTarget(String from, int r, int c) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("BOMB_TARGET " + r + " " + c);
        }
    }

    // Time Lock 시작 알림: from -> 그의 상대에게만
    private static void forwardTimeLockStart(String from) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("TIMELOCK_START");
        }
    }

    // Swap 시작 알림: from -> 그의 상대에게만
    private static void forwardSwapStart(String from) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("SWAP_START");
        }
    }

    // Swap 타겟 좌표 전달: from -> 그의 상대에게만
    private static void forwardSwapTarget(String from, int myR, int myC, int oppR, int oppC) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("SWAP_TARGET " + myR + " " + myC + " " + oppR + " " + oppC);
        }
    }

}
