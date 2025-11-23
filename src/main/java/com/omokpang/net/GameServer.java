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

    // 4인 FFA 큐
    private static final Queue<String> queueFfa4 = new ArrayDeque<>();

    // 2:2 팀전 큐
    private static final Queue<String> queue2v2 = new ArrayDeque<>();

    // "어떤 닉네임이 어떤 방에 속해 있는지"
    private static final Map<String, Room> roomMap = new ConcurrentHashMap<>();

    // 간단한 Room 구조
    private static class Room {
        String mode;           // "1v1" 또는 "1v1v1v1"
        String[] players;      // 방에 속한 닉네임들 (2 or 4)
        int turnIndex;         // 현재 턴 플레이어 인덱스 (0~n-1)

        Room(String mode, String[] players, int turnIndex) {
            this.mode = mode;
            this.players = players;
            this.turnIndex = turnIndex;
        }
    }

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

    // 방 안의 from 을 제외한 모든 플레이어에게 message 전송
    private static void broadcastToRoomExcept(Room room, String from, String message) {
        for (String p : room.players) {
            if (p.equals(from)) continue; // 나 자신은 제외
            PrintWriter out = clientMap.get(p);
            if (out != null) {
                out.println(message);
            }
        }
    }

    // 말풍선 전송: from → 같은 방의 다른 모든 플레이어 or 1:1 상대
    private static void forwardCheer(String from, String text) {
        Room room = roomMap.get(from);
        String msg = "CHEER " + from + " " + text;

        // 방이 있으면: 같은 방의 나를 제외한 모두에게
        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        // 방이 없으면 기존 1:1
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // 돌 두기 전송: from → 같은 방의 다른 모든 플레이어 or 1:1 상대
    private static void forwardPlace(String from, int r, int c) {
        Room room = roomMap.get(from);
        String msg = "PLACE " + r + " " + c;

        // 🔥 방이 있으면 방 전체(나 제외)에게 브로드캐스트
        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        // 👉 방이 없으면 기존 1:1
        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // SharedStone 시작 알림: from -> 같은 방의 다른 플레이어 or 1:1 상대
    private static void forwardSharedStoneStart(String from) {
        Room room = roomMap.get(from);
        String msg = "SHARED_STONE_START";

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // SharedStone 타겟 좌표 전달: from -> 같은 방의 다른 플레이어 or 1:1 상대
    private static void forwardSharedStoneTarget(String from, int r, int c) {
        Room room = roomMap.get(from);
        String msg = "SHARED_STONE_TARGET " + r + " " + c;

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Bomb!! 시작 알림
    private static void forwardBombStart(String from) {
        Room room = roomMap.get(from);
        String msg = "BOMB_START";

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Bomb!! 타겟 좌표 전달
    private static void forwardBombTarget(String from, int r, int c) {
        Room room = roomMap.get(from);
        String msg = "BOMB_TARGET " + r + " " + c;

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Time Lock 시작 알림: from -> 같은 방의 다른 플레이어 or 1:1 상대
    private static void forwardTimeLockStart(String from) {
        Room room = roomMap.get(from);
        String msg = "TIMELOCK_START";

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Swap 시작 알림: from -> 같은 방의 다른 플레이어 or 1:1 상대
    private static void forwardSwapStart(String from) {
        Room room = roomMap.get(from);
        String msg = "SWAP_START";

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Swap 타겟 좌표 전달: from -> 같은 방의 다른 플레이어 or 1:1 상대
    private static void forwardSwapTarget(String from, int myR, int myC, int oppR, int oppC) {
        Room room = roomMap.get(from);
        String msg = "SWAP_TARGET " + myR + " " + myC + " " + oppR + " " + oppC;

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // DoubleMove 시작 알림: from -> 같은 방의 다른 플레이어 or 1:1 상대
    private static void forwardDoubleMoveStart(String from) {
        Room room = roomMap.get(from);
        String msg = "DOUBLE_MOVE_START";

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Remove 시작 알림: from -> 같은 방의 다른 플레이어 or 1:1 상대
    private static void forwardRemoveStart(String from) {
        Room room = roomMap.get(from);
        String msg = "REMOVE_START";

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Remove 타겟 좌표 전달: from -> 같은 방의 다른 플레이어 or 1:1 상대
    private static void forwardRemoveTarget(String from, int r, int c) {
        Room room = roomMap.get(from);
        String msg = "REMOVE_TARGET " + r + " " + c;

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Shield 방어 – Remove 무효화 알림
    private static void forwardShieldBlockRemove(String from) {
        Room room = roomMap.get(from);
        String msg = "SHIELD_BLOCK_REMOVE";

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
        }
    }

    // Shield 방어 – Swap 무효화 알림
    private static void forwardShieldBlockSwap(String from) {
        Room room = roomMap.get(from);
        String msg = "SHIELD_BLOCK_SWAP";

        if (room != null) {
            broadcastToRoomExcept(room, from, msg);
            return;
        }

        String opp = opponentMap.get(from);
        if (opp == null) return;

        PrintWriter outOpp = clientMap.get(opp);
        if (outOpp != null) {
            outOpp.println(msg);
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
        // 먼저 4인용 방부터 확인
        Room room = roomMap.get(nick);
        if (room != null) {
            // 방 안에서만 턴 교체
            if (!nick.equals(room.players[room.turnIndex])) {
                System.out.println("[SERVER] WARN: TURN_END from non-turn player in room: " + nick);
                return;
            }
            room.turnIndex = (room.turnIndex + 1) % room.players.length;
            broadcastTurn(room);
            return;
        }

        // 👉 room 이 없다는 건 1:1 매치(구 방식)를 쓰고 있다는 뜻이니
        //    기존 currentTurnMap + opponentMap 로직을 그대로 둠
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

                if (line.startsWith("QUEUE ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        String mode = parts[1];   // "1v1" / "1v1v1v1" / "2v2"
                        String nick = parts[2];

                        if ("1v1".equals(mode)) {
                            enqueue1v1(nick);
                        } else if ("1v1v1v1".equals(mode)) {
                            enqueueFfa4(nick);
                        } else if ("2v2".equals(mode)) {     // ✅ 추가
                            enqueue2v2(nick);
                        }
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

    // 4인 FFA 대기열
    private static synchronized void enqueueFfa4(String nick) {
        if (queueFfa4.contains(nick)) return;

        queueFfa4.add(nick);
        System.out.println("[SERVER] QUEUE 1v1v1v1: " + nick +
                " (현재 대기: " + queueFfa4.size() + ")");

        if (queueFfa4.size() >= 4) {
            String a = queueFfa4.poll();
            String b = queueFfa4.poll();
            String c = queueFfa4.poll();
            String d = queueFfa4.poll();

            PrintWriter outA = clientMap.get(a);
            PrintWriter outB = clientMap.get(b);
            PrintWriter outC = clientMap.get(c);
            PrintWriter outD = clientMap.get(d);

            if (outA != null && outB != null && outC != null && outD != null) {
                String playersStr = a + "," + b + "," + c + "," + d;
                String matchMsg = "MATCH 1v1v1v1 " + playersStr;

                outA.println(matchMsg);
                outB.println(matchMsg);
                outC.println(matchMsg);
                outD.println(matchMsg);

                System.out.println("[SERVER] MATCHED 1v1v1v1: " + matchMsg);

                // 🔥 방 생성 (선공은 a, 그 다음 b,c,d 순으로 턴)
                String[] players = {a, b, c, d};
                Room room = new Room("1v1v1v1", players, 0);

                for (String p : players) {
                    roomMap.put(p, room);
                }

                // 첫 턴 브로드캐스트
                broadcastTurn(room);
            }
        }
    }

    // 해당 방의 현재 턴을 모든 플레이어에게 알리기
    private static void broadcastTurn(Room room) {
        String curNick = room.players[room.turnIndex];

        for (String p : room.players) {
            PrintWriter out = clientMap.get(p);
            if (out != null) out.println("TURN " + curNick);
        }

        System.out.println("[SERVER] TURN broadcast(room=" + room.mode +
                "): " + curNick);
    }

    // 2:2 팀전 대기열
    private static synchronized void enqueue2v2(String nick) {
        // 이미 큐에 있으면 중복 방지
        if (queue2v2.contains(nick)) return;

        queue2v2.add(nick);
        System.out.println("[SERVER] QUEUE 2v2: " + nick +
                " (현재 대기: " + queue2v2.size() + ")");

        // 4명 모이면 매칭
        if (queue2v2.size() >= 4) {
            String a = queue2v2.poll();
            String b = queue2v2.poll();
            String c = queue2v2.poll();
            String d = queue2v2.poll();

            PrintWriter outA = clientMap.get(a);
            PrintWriter outB = clientMap.get(b);
            PrintWriter outC = clientMap.get(c);
            PrintWriter outD = clientMap.get(d);

            if (outA != null && outB != null && outC != null && outD != null) {
                String playersStr = a + "," + b + "," + c + "," + d;

                // ✅ 모드명을 "2v2" 로 보냄
                String matchMsg = "MATCH 2v2 " + playersStr;

                outA.println(matchMsg);
                outB.println(matchMsg);
                outC.println(matchMsg);
                outD.println(matchMsg);

                System.out.println("[SERVER] MATCHED 2v2: " + matchMsg);

                // ✅ 방 생성 (턴 순서는 a → b → c → d 순으로 진행)
                String[] players = { a, b, c, d };
                Room room = new Room("2v2", players, 0);

                for (String p : players) {
                    roomMap.put(p, room);
                }

                // 첫 턴 브로드캐스트
                broadcastTurn(room);
            }
        }
    }

}
