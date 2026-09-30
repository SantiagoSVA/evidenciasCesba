package com.ejemplo.evidenciasfotograficas;

import org.json.JSONException;
import org.json.JSONObject;

final class Evidence {
    final String id;
    final String title;
    final String description;
    final String photoName;
    final long capturedAt;

    Evidence(String id, String title, String description, String photoName, long capturedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.photoName = photoName;
        this.capturedAt = capturedAt;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("description", description);
        json.put("photoName", photoName);
        json.put("capturedAt", capturedAt);
        return json;
    }

    static Evidence fromJson(JSONObject json) throws JSONException {
        return new Evidence(json.getString("id"), json.getString("title"),
                json.getString("description"), json.getString("photoName"),
                json.getLong("capturedAt"));
    }
}
