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
 *    LOGIN / QUEUE / MATCH / 턴 관리 정도를 처리하는 매칭 서버.
 */
public class GameServer {

    // 닉네임 -> 해당 클라이언트 출력 스트림
    private static final Map<String, PrintWriter> clientMap = new ConcurrentHashMap<>();

    // 1:1 매칭 대기열 (닉네임만 저장)
    private static final Queue<String> queue1v1 = new ArrayDeque<>();

    // 매칭된 상대 매핑 (양방향)
    private static final Map<String, String> opponentMap = new ConcurrentHashMap<>();

    // 🔥 현재 누구 차례인지 저장 (양쪽 닉네임 모두 같은 값 저장)
    //  - key: 플레이어 닉네임
    //  - value: 현재 턴을 가진 플레이어의 닉네임
    private static final Map<String, String> currentTurnMap = new ConcurrentHashMap<>();

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

    // DoubleMove 시작 알림: from -> 그의 상대에게만
    private static void forwardDoubleMoveStart(String from) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("DOUBLE_MOVE_START");
        }
    }

    // Remove 시작 알림: from -> 그의 상대에게만
    private static void forwardRemoveStart(String from) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("REMOVE_START");
        }
    }

    // Remove 타겟 좌표 전달: from -> 그의 상대에게만
    private static void forwardRemoveTarget(String from, int r, int c) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("REMOVE_TARGET " + r + " " + c);
        }
    }

    // Shield 방어 – Remove 무효화 알림
    private static void forwardShieldBlockRemove(String from) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("SHIELD_BLOCK_REMOVE");
        }
    }

    // Shield 방어 – Swap 무효화 알림
    private static void forwardShieldBlockSwap(String from) {
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println("SHIELD_BLOCK_SWAP");
        }
    }

    // ====================== 턴 관리 유틸 ======================

    /** a, b 한 쌍에 대해 현재 턴을 가진 닉네임을 저장 */
    private static void setCurrentTurnForPair(String a, String b, String turnOwner) {
        currentTurnMap.put(a, turnOwner);
        currentTurnMap.put(b, turnOwner);
    }

    /** a, b 한 쌍에게 현재 턴 주인을 브로드캐스트 (TURN <nickname>) */
    private static void broadcastTurnToPair(String a, String b) {
        String turnOwner = currentTurnMap.get(a); // 양쪽 값이 같으니 a 기준으로
        if (turnOwner == null) return;

        PrintWriter outA = clientMap.get(a);
        PrintWriter outB = clientMap.get(b);

        if (outA != null) outA.println("TURN " + turnOwner);
        if (outB != null) outB.println("TURN " + turnOwner);

        System.out.println("[SERVER] TURN broadcast: " + a + "," + b + " -> " + turnOwner);
    }

    /** TURN_END 를 받은 플레이어 닉네임 기준으로 다음 턴을 상대에게 넘김 */
    private static void handleTurnEnd(String nick) {
        String opp = opponentMap.get(nick);
        if (opp == null) {
            System.out.println("[SERVER] TURN_END from " + nick + " but no opponent.");
            return;
        }

        String cur = currentTurnMap.get(nick);
        if (cur == null || !cur.equals(nick)) {
            System.out.println("[SERVER] WARN: TURN_END from non-turn player: " + nick +
                    " (currentTurn=" + cur + ")");
            return;
        }

        // 다음 턴은 상대
        setCurrentTurnForPair(nick, opp, opp);
        broadcastTurnToPair(nick, opp);
    }

    // ====================== 클라이언트 핸들러 ======================

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

                // 🔥 턴 종료: TURN_END
                if (line.startsWith("TURN_END")) {
                    if (nickname != null) {
                        handleTurnEnd(nickname);
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

                // 🔥 DoubleMove 시작: DOUBLE_MOVE_START
                if (line.startsWith("DOUBLE_MOVE_START")) {
                    if (nickname != null) {
                        forwardDoubleMoveStart(nickname);
                    }
                    continue;
                }

                // 🔥 Remove 시작: REMOVE_START
                if (line.startsWith("REMOVE_START")) {
                    if (nickname != null) {
                        forwardRemoveStart(nickname);
                    }
                    continue;
                }

                // 🔥 Remove 타겟: REMOVE_TARGET r c
                if (line.startsWith("REMOVE_TARGET")) {
                    if (nickname != null) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            int r = Integer.parseInt(parts[1]);
                            int c = Integer.parseInt(parts[2]);
                            forwardRemoveTarget(nickname, r, c);
                        }
                    }
                    continue;
                }

                // 🔥 Shield 방어: SHIELD_BLOCK_REMOVE
                if (line.startsWith("SHIELD_BLOCK_REMOVE")) {
                    if (nickname != null) {
                        forwardShieldBlockRemove(nickname);
                    }
                    continue;
                }

                // 🔥 Shield 방어: SHIELD_BLOCK_SWAP
                if (line.startsWith("SHIELD_BLOCK_SWAP")) {
                    if (nickname != null) {
                        forwardShieldBlockSwap(nickname);
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

                // 상대도 함께 정리
                String opp = opponentMap.remove(nickname);
                if (opp != null) {
                    opponentMap.remove(opp);
                    currentTurnMap.remove(opp);
                }
                currentTurnMap.remove(nickname);
            }
        }
    }

    // ====================== 매칭 로직 ======================

    // 1:1 대기열에 넣고, 2명 모이면 MATCH + 초기 TURN 보내기
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

                // 서로의 상대를 등록 (양방향)
                opponentMap.put(a, b);
                opponentMap.put(b, a);

                // 🔥 선공은 a 로 고정 (players[0] = a)
                setCurrentTurnForPair(a, b, a);
                broadcastTurnToPair(a, b); // TURN a
            }
        }
    }

}
