package com.jobtracker.backend.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class GmailService {

    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(GmailScopes.GMAIL_READONLY);
    private static final String USER = "me";
    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    @Value("${gmail.credentials.path}")
    private String credentialsPath;

    @Value("${gmail.tokens.path}")
    private String tokensPath;

    private NetHttpTransport httpTransport;

    private GoogleAuthorizationCodeFlow buildFlow() throws IOException, GeneralSecurityException {
        NetHttpTransport transport = getHttpTransport();

        InputStream in = new FileInputStream(credentialsPath);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(in));

        return new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(tokensPath)))
                .setAccessType("offline")
                .build();
    }

    private NetHttpTransport getHttpTransport() throws GeneralSecurityException, IOException {
        if (httpTransport == null) {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        }
        return httpTransport;
    }

    public String getAuthorizationUrl(String redirectUri) throws IOException, GeneralSecurityException {
        return buildFlow().newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .build();
    }

    public void exchangeCodeForToken(String code, String redirectUri)
            throws IOException, GeneralSecurityException {
        GoogleAuthorizationCodeFlow flow = buildFlow();
        TokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();
        flow.createAndStoreCredential(tokenResponse, USER);
        log.info("Gmail token stored successfully");
    }

    public Gmail getGmailClient() throws IOException, GeneralSecurityException {
        GoogleAuthorizationCodeFlow flow = buildFlow();
        Credential credential = flow.loadCredential(USER);
        if (credential == null) {
            throw new IllegalStateException("Gmail not connected");
        }
        return new Gmail.Builder(getHttpTransport(), JSON_FACTORY, credential)
                .setApplicationName("Job Tracker")
                .build();
    }

    public boolean isConnected() {
        try {
            return buildFlow().loadCredential(USER) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getConnectedEmail() {
        try {
            return getGmailClient().users().getProfile(USER).execute().getEmailAddress();
        } catch (Exception e) {
            return null;
        }
    }

    public void disconnect() {
        revokeToken();

        try {
            File tokenDir = new File(tokensPath);
            if (tokenDir.exists()) {
                for (File file : Objects.requireNonNull(tokenDir.listFiles())) {
                    file.delete();
                }
            }
            log.info("Gmail disconnected successfully");
        } catch (Exception e) {
            log.error("Error deleting local Gmail tokens", e);
        }
    }

    private void revokeToken() {
        try {
            GoogleAuthorizationCodeFlow flow = buildFlow();
            Credential credential = flow.loadCredential(USER);
            if (credential == null) {
                return;
            }

            String token = credential.getRefreshToken() != null
                    ? credential.getRefreshToken()
                    : credential.getAccessToken();
            if (token == null) {
                return;
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(REVOKE_URL + "?token=" + token))
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Gmail token revocation response: {}", response.statusCode());
        } catch (Exception e) {
            log.warn("Could not revoke Gmail token with Google (continuing to delete local token): {}",
                    e.getMessage());
        }
    }

    public List<Message> fetchRecentMessages(int maxResults) {
        try {
            Gmail gmail = getGmailClient();
            ListMessagesResponse response = gmail.users().messages()
                    .list(USER)
                    .setQ("subject:(application OR interview OR offer OR rejected OR assessment OR hiring)")
                    .setMaxResults((long) maxResults)
                    .execute();

            if (response.getMessages() == null)
                return List.of();

            List<Message> messages = new ArrayList<>();
            for (Message msg : response.getMessages()) {
                Message full = gmail.users().messages()
                        .get(USER, msg.getId())
                        .setFormat("full")
                        .execute();
                messages.add(full);
            }
            return messages;
        } catch (Exception e) {
            log.error("Error fetching Gmail messages", e);
            return List.of();
        }
    }

    public String getSubject(Message message) {
        return getHeader(message, "Subject");
    }

    public String getFrom(Message message) {
        return getHeader(message, "From");
    }

    public String getBody(Message message) {
        try {
            MessagePart payload = message.getPayload();
            if (payload == null)
                return "";

            // Single part
            if (payload.getBody() != null && payload.getBody().getData() != null) {
                return decodeBase64(payload.getBody().getData());
            }

            // Multipart
            if (payload.getParts() != null) {
                for (MessagePart part : payload.getParts()) {
                    if ("text/plain".equals(part.getMimeType()) && part.getBody() != null) {
                        return decodeBase64(part.getBody().getData());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting email body", e);
        }
        return "";
    }

    private String getHeader(Message message, String name) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null)
            return "";
        return message.getPayload().getHeaders().stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }

    private String decodeBase64(String data) {
        byte[] decoded = Base64.getUrlDecoder().decode(data);
        return new String(decoded, StandardCharsets.UTF_8);
    }
}
