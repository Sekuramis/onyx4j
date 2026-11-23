package de.onyx4j.api.exceptions;

import java.util.Objects;
/**
 * © Copyright 24.11.2026 - 00:05 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class OnyxApiException extends RuntimeException {

	private final int statusCode;
	private final String method;
	private final String path;
	private final String responseBody;

	public OnyxApiException(int statusCode, String method, String path, String responseBody) {
		this(statusCode, method, path, responseBody, null);
	}

	public OnyxApiException(
			int statusCode,
			String method,
			String path,
			String responseBody,
			Throwable cause
	) {
		super(message(statusCode, method, path), cause);
		this.statusCode = statusCode;
		this.method = Objects.requireNonNull(method, "method");
		this.path = safePath(path);
		this.responseBody = responseBody;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public String getResponseBody() {
		return responseBody;
	}

	private static String message(int status, String method, String path) {
		return "Onyx API " + method + " " + safePath(path)
				+ (status == 0 ? " failed due to a connection error" : " failed with HTTP " + status);
	}

	private static String safePath(String raw) {
		return Objects.requireNonNull(raw, "path")
				.replaceAll("(/interactions/[^/]+/)[^/?]+", "$1[REDACTED]");
	}
}