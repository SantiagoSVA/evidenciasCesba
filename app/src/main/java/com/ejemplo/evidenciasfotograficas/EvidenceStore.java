package com.ejemplo.evidenciasfotograficas;

import android.content.Context;
import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class EvidenceStore {
    private final Context context;
    private final AtomicFile index;

    EvidenceStore(Context context) {
        this.context = context.getApplicationContext();
        this.index = new AtomicFile(new File(context.getFilesDir(), "evidencias.json"));
    }

    File photosDir() throws IOException {
        File dir = new File(context.getFilesDir(), "fotos");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de fotografías");
        }
        return dir;
    }

    File photo(Evidence evidence) throws IOException {
        // El nombre proviene de un UUID generado por la app, no de una entrada del usuario.
        if (!evidence.photoName.matches("[a-f0-9-]+\\.jpg")) {
            throw new IOException("Nombre de fotografía inválido");
        }
        return new File(photosDir(), evidence.photoName);
    }

    List<Evidence> all() throws IOException {
        List<Evidence> items = new ArrayList<>();
        byte[] data;
        try {
            data = index.readFully();
        } catch (FileNotFoundException missing) {
            return items;
        }
        try {
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
            for (int i = 0; i < array.length(); i++) {
                items.add(Evidence.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException invalid) {
            throw new IOException("El índice de evidencias está dañado", invalid);
        }
        Collections.sort(items, (a, b) -> Long.compare(b.capturedAt, a.capturedAt));
        return items;
    }

    Evidence find(String id) throws IOException {
        for (Evidence item : all()) {
            if (item.id.equals(id)) return item;
        }
        return null;
    }

    void add(Evidence evidence) throws IOException {
        List<Evidence> items = all();
        items.add(0, evidence);
        write(items);
    }

    void delete(Evidence evidence) throws IOException {
        List<Evidence> items = all();
        boolean removed = items.removeIf(item -> item.id.equals(evidence.id));
        if (!removed) return;
        write(items);
        File photo = photo(evidence);
        if (photo.exists() && !photo.delete()) {
            throw new IOException("La ficha se eliminó, pero no se pudo borrar la fotografía");
        }
    }

    private void write(List<Evidence> items) throws IOException {
        JSONArray array = new JSONArray();
        try {
            for (Evidence item : items) array.put(item.toJson());
        } catch (JSONException error) {
            throw new IOException("No se pudo codificar la evidencia", error);
        }
        FileOutputStream stream = null;
        try {
            stream = index.startWrite();
            stream.write(array.toString().getBytes(StandardCharsets.UTF_8));
            index.finishWrite(stream);
        } catch (IOException error) {
            if (stream != null) index.failWrite(stream);
            throw error;
        }
    }
}
