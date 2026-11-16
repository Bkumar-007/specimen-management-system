package com.specimen.sms.application;

import java.security.SecureRandom;

final class CodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    static String digits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
