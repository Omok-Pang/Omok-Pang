package com.omokpang.service;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthService
 * 역할: 로그인 및 회원가입 로직 (임시 메모리 기반)
 * 추후 DB 연동 시 이 로직을 교체하면 됨.
 */
public class AuthService {

    // 임시로 사용자 정보를 저장하는 Map (DB 대신)
    private final Map<String, String> users = new HashMap<>();

    // 회원가입
    public boolean signup(String id, String pw) {
        if (users.containsKey(id)) return false; // 이미 존재
        users.put(id, pw);
        return true;
    }

    // 로그인
    public boolean login(String id, String pw) {
        return users.containsKey(id) && users.get(id).equals(pw);
    }
}
