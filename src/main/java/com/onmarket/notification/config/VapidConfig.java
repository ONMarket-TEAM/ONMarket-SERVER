package com.onmarket.notification.config;

import com.zerodeplibs.webpush.VAPIDKeyPair;
import com.zerodeplibs.webpush.VAPIDKeyPairs;
import com.zerodeplibs.webpush.key.PrivateKeySources;
import com.zerodeplibs.webpush.key.PublicKeySources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Configuration
public class VapidConfig {

    @Value("${vapid.private-key-file:.keys/vapidPrivateKey.pem}")
    private String privateKeyFile;

    @Value("${vapid.public-key-file:.keys/vapidPublicKey.pem}")
    private String publicKeyFile;

    @Bean
    public VAPIDKeyPair vapidKeyPair() throws Exception {
        Path priv = Path.of(privateKeyFile);
        Path pub  = Path.of(publicKeyFile);

        // 키가 없으면 생성해서 저장 (단 한번)
        if (!Files.exists(priv) || !Files.exists(pub)) {
            generateAndWriteVapidKeys(priv, pub);
        }

        // 파일에서 읽어 VAPIDKeyPair 생성
        return VAPIDKeyPairs.of(
                PrivateKeySources.ofPEMFile(priv),
                PublicKeySources.ofPEMFile(pub)
        );
    }

    private static void generateAndWriteVapidKeys(Path priv, Path pub) throws Exception {
        // P-256 (secp256r1) 키쌍 생성
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1")); // prime256v1
        KeyPair kp = kpg.generateKeyPair();

        // 디렉터리 보장
        if (priv.getParent() != null) Files.createDirectories(priv.getParent());
        if (pub.getParent()  != null) Files.createDirectories(pub.getParent());

        // PKCS#8 (Private), X.509 SPKI (Public) → PEM으로 저장
        writePem(priv,  "PRIVATE KEY", kp.getPrivate().getEncoded());
        writePem(pub,   "PUBLIC KEY",  kp.getPublic().getEncoded());
    }

    private static void writePem(Path path, String type, byte[] der) throws IOException {
        String header = "-----BEGIN " + type + "-----\n";
        String footer = "\n-----END " + type + "-----\n";
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(der);
        Files.writeString(path, header + body + footer,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }
}
