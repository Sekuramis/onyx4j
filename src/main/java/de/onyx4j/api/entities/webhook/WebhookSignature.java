package de.onyx4j.api.entities.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * © Copyright 23.11.2026 - 16:52 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class WebhookSignature {

	private WebhookSignature() {}

	public static boolean verify(String secret, String header, byte[] body) {
		return verify(secret, header, body, Instant.now(), Duration.ofMinutes(5));
	}

	public static boolean verify(String secret, String header, byte[] body, Instant now, Duration tolerance) {
		if (secret == null || secret.isBlank() || header == null || body == null || now == null
				|| tolerance == null || tolerance.isNegative() || header.length() > 8192) return false;
		String timestamp = null;
		List<String> signatures = new ArrayList<>();
		for (String part : header.split(",")) {
			int separator = part.indexOf('=');
			if (separator < 1) continue;
			String key = part.substring(0, separator).trim();
			String value = part.substring(separator + 1).trim();
			if ("t".equals(key)) {
				if (timestamp != null) return false;
				timestamp = value;
			} else if ("v1".equals(key)) signatures.add(value);
		}
		if (timestamp == null || !timestamp.matches("[0-9]{1,12}") || signatures.isEmpty()) return false;
		Instant signed = Instant.ofEpochSecond(Long.parseLong(timestamp));
		if (Duration.between(signed, now).abs().compareTo(tolerance) > 0) return false;
		byte[] expected = digest(secret, timestamp, body);
		boolean valid = false;
		for (String signature : signatures) {
			if (signature.matches("[0-9a-fA-F]{64}"))
				valid |= MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature));
		}
		return valid;
	}

	public static String sign(String secret, byte[] body, Instant timestamp) {
		if (secret == null || secret.isBlank() || body == null || timestamp == null || timestamp.getEpochSecond() < 0)
			throw new IllegalArgumentException("Invalid signature input");
		String t = Long.toString(timestamp.getEpochSecond());
		return "t=" + t + ",v1=" + HexFormat.of().formatHex(digest(secret, t, body));
	}

	private static byte[] digest(String secret, String timestamp, byte[] body) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			mac.update((timestamp + ".").getBytes(StandardCharsets.US_ASCII));
			return mac.doFinal(body);
		} catch (GeneralSecurityException error) {
			throw new IllegalStateException("HMAC-SHA256 is unavailable", error);
		}
	}
}
