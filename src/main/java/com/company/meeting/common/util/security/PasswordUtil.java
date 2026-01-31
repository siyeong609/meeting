package com.company.meeting.common.util.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtil
 * - PBKDF2 기반 비밀번호 해시/검증 유틸
 *
 * 저장 포맷:
 *  pbkdf2$<iterations>$<saltBase64>$<hashBase64>
 *
 * 정책:
 * - DB에 평문이 남아있을 수 있으므로(시드 데이터 등)
 *   stored 값이 pbkdf2$ 로 시작하지 않으면 "평문 비교"를 허용한다.
 * - 로그인 성공 시 평문이면 자동으로 해시로 업그레이드하도록 서비스에서 처리한다.
 */
public class PasswordUtil {

    private static final String PREFIX = "pbkdf2$";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // 개발/학습 환경 기준(운영에서는 더 높여도 됨)
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    /**
     * 저장 문자열이 해시 포맷인지 판별
     */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    /**
     * 평문을 해시 문자열로 변환
     */
    public static String hash(String plain) {
        if (plain == null) plain = "";

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(plain.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        return PREFIX
                + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 입력 비밀번호가 저장된 값과 일치하는지 확인
     *
     * - 해시 포맷이면 PBKDF2 검증
     * - 평문이면 평문 비교(마이그레이션 단계용)
     */
    public static boolean verify(String plain, String stored) {
        if (plain == null) plain = "";
        if (stored == null) stored = "";

        // 해시가 아니면 평문 비교(초기 데이터/마이그레이션 단계)
        if (!isHashed(stored)) {
            return stored.equals(plain);
        }

        try {
            // pbkdf2$iterations$salt$hash
            String body = stored.substring(PREFIX.length());
            String[] parts = body.split("\\$");
            if (parts.length != 3) return false;

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            byte[] actualHash = pbkdf2(plain.toCharArray(), salt, iterations, expectedHash.length * 8);

            // 상수시간 비교
            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}
