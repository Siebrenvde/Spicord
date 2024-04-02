package org.spicord.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class UpdateNotifier {

    private String url;
    private Logger log;
    private boolean logError;

    public UpdateNotifier(String url, Logger log, boolean logError) {
        this.url = url;
        this.log = log;
        this.logError = logError;
    }

    public void checkForVersionAsync(ExecutorService executorService, String version, Map<String, String> extra) {
        Runnable r = () -> {
            try {
                checkForVersion(version, extra);
            } catch (Exception e) {
                if (logError) {
                    e.printStackTrace();
                }
            }
        };
        executorService.submit(r);
    }

    public void checkForVersion(String version, Map<String, String> extra) throws IOException {
        final HttpURLConnection con = (HttpURLConnection) new URL(
            url.replace("%version%", version)
        ).openConnection();

        con.setConnectTimeout(10_000);
        con.setReadTimeout(10_000);

        con.addRequestProperty("User-Agent", "Mozilla/5.0");
        con.addRequestProperty("X-Updater-Version", "1.0");
        con.addRequestProperty("X-Plugin-Version", version);

        if (extra != null) {
            for (Entry<String, String> entry : extra.entrySet()) {
                con.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        if (con.getResponseCode() == 200) {
            final JsonObject response = new Gson().fromJson(new InputStreamReader(con.getInputStream()), JsonObject.class);

            if (response.has("message")) {
                final JsonElement messageJson = response.get("message");

                if (messageJson.isJsonArray()) {
                    final JsonArray messages = messageJson.getAsJsonArray();

                    for (int i = 0; i < messages.size(); i++) {
                        final String message = messages.get(i).getAsString();

                        log.info(message);
                    }
                } else {
                    final String message = messageJson.getAsString();

                    log.info(message);
                }
            }
        }
    }
}
