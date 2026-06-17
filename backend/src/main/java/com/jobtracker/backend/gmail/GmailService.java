package com.jobtracker.backend.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
@Slf4j
public class GmailService {

    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(GmailScopes.GMAIL_READONLY);
    private static final String USER = "me";

    @Value("${gmail.credentials.path}")
    private String credentialsPath;

    @Value("${gmail.tokens.path}")
    private String tokensPath;

    private Gmail gmailClient;

    public Gmail getGmailClient() throws IOException, GeneralSecurityException {
        if (gmailClient == null) {
            gmailClient = buildGmailClient();
        }
        return gmailClient;
    }

    private Gmail buildGmailClient() throws IOException, GeneralSecurityException {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Load credentials
        InputStream in = new FileInputStream(credentialsPath);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(in));

        // Build flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(tokensPath)))
                .setAccessType("offline")
                .build();

        // Authorize
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
                .authorize("user");

        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("Job Tracker")
                .build();
    }

    public void resetClient() {
        gmailClient = null;
    }

    public boolean isConnected() {
        try {
            File tokenDir = new File(tokensPath);
            return tokenDir.exists() && Objects.requireNonNull(tokenDir.listFiles()).length > 0;
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
        try {
            // Revoke token
            if (gmailClient != null) {
                gmailClient = null;
            }
            // Delete token files
            File tokenDir = new File(tokensPath);
            if (tokenDir.exists()) {
                for (File file : Objects.requireNonNull(tokenDir.listFiles())) {
                    file.delete();
                }
            }
            log.info("Gmail disconnected successfully");
        } catch (Exception e) {
            log.error("Error disconnecting Gmail", e);
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