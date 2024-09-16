package com.ibm.bmcshell;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.util.Scanner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TotpService {

    private static final int BITS_PER_BASE32_CHAR = 5;
    private static final int VERIFICATION_CODE_MODULUS = 1000000;
    private static final int SHA1_DIGEST_LENGTH = 20;
    private static final long STEP_SIZE = 30;

    private String secret;

    public TotpService loadSecret(String userName) throws IOException {
        String filePath = String.format("/home/%s/.google_authenticator", userName);
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            this.secret = reader.readLine();
        }
        return this;
    }

    public TotpService loadSecretString(String sercreString) throws IOException {
        this.secret = sercreString;
        return this;
    }

    public String now(long offset) throws NoSuchAlgorithmException, InvalidKeyException {
        if (secret == null || secret.isEmpty()) {
            return "Secret not found";
        }
        return String.format("%06d", generateCode(secret, totpTimeNow(offset)));
    }

    public String after(long sec) throws NoSuchAlgorithmException, InvalidKeyException {
        return String.format("%06d", generateCode(secret, totpTimeNow(0) + sec));
    }

    public boolean verify(String totp) throws NoSuchAlgorithmException, InvalidKeyException {
        long windowsize = 3;
        for (long i = -(windowsize - 1) / 2; i <= windowsize / 2; i++) {
            String expectedTotp = now(i * STEP_SIZE);
            if (totp.equals(expectedTotp)) {
                return true;
            }
        }
        System.err.println("TOTP verification failed for " + totp);
        return false;
    }

    private long totpTimeNow(long offset) {
        long secondsSinceEpoch = Instant.now().getEpochSecond() + offset;
        return secondsSinceEpoch / STEP_SIZE;
    }

    private static int generateCode(String key, long tm) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] challenge = new byte[8];
        for (int i = 8; i-- > 0; tm >>= 8) {
            challenge[i] = (byte) tm;
        }

        byte[] secret = Base32.decode(key);

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec macKey = new SecretKeySpec(secret, "RAW");
        mac.init(macKey);
        byte[] hash = mac.doFinal(challenge);

        int offset = hash[SHA1_DIGEST_LENGTH - 1] & 0xF;

        int truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= VERIFICATION_CODE_MODULUS;

        return truncatedHash;
    }

    public static class Base32 {
        private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        public static byte[] decode(String encoded) {
            encoded = encoded.replaceAll(" ", "").replaceAll("-", "").toUpperCase();
            int encodedLength = encoded.length();
            int outputLength = encodedLength * BITS_PER_BASE32_CHAR / 8;
            byte[] result = new byte[outputLength];

            int buffer = 0;
            int bitsLeft = 0;
            int count = 0;

            for (char c : encoded.toCharArray()) {
                int value = BASE32_CHARS.indexOf(c);
                if (value == -1) {
                    throw new IllegalArgumentException("Invalid Base32 character: " + c);
                }

                buffer <<= BITS_PER_BASE32_CHAR;
                buffer |= value & 31;
                bitsLeft += BITS_PER_BASE32_CHAR;

                if (bitsLeft >= 8) {
                    result[count++] = (byte) (buffer >> (bitsLeft - 8));
                    bitsLeft -= 8;
                }
            }

            return result;
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the secret: ");
            String secret = scanner.nextLine();

            TotpService totpService = new TotpService().loadSecretString(secret);
            String totp = totpService.now(0);
            System.out.println("Current TOTP: " + totp);

            boolean isValid = totpService.verify(totp);
            System.out.println("TOTP is valid: " + isValid);
            isValid = totpService.verify(totpService.now(30));
            System.out.println("TOTP  after 30 is valid: " + isValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
