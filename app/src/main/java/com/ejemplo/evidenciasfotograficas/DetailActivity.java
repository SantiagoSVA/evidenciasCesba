package com.ejemplo.evidenciasfotograficas;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class DetailActivity extends AppCompatActivity {
    static final String EXTRA_ID = "evidence_id";

    private EvidenceStore store;
    private Evidence evidence;

    private final ActivityResultLauncher<String> savePhoto =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("image/jpeg"), uri -> {
                if (uri != null && evidence != null) copyPhoto(uri);
            });

    private final ActivityResultLauncher<String> savePdf =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                if (uri == null || evidence == null) return;
                try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                    if (output == null) throw new IOException("No se pudo abrir el destino");
                    EvidencePdf.write(evidence, store.photo(evidence), output);
                    toast("Ficha PDF generada");
                } catch (IOException error) {
                    showError(error);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        store = new EvidenceStore(this);
        String id = getIntent().getStringExtra(EXTRA_ID);
        if (id == null) {
            finish();
            return;
        }
        try {
            evidence = store.find(id);
            if (evidence == null) {
                toast("La evidencia ya no existe");
                finish();
                return;
            }
            File file = store.photo(evidence);
            PhotoUtils.show(file, (ImageView) findViewById(R.id.imgDetalle));
            ((TextView) findViewById(R.id.txtTituloDetalle)).setText(evidence.title);
            ((TextView) findViewById(R.id.txtDescripcionDetalle)).setText(evidence.description);
            ((TextView) findViewById(R.id.txtFechaDetalle)).setText(
                    "Fecha de captura: " + EvidenceFormat.date(evidence.capturedAt));
            ((TextView) findViewById(R.id.txtHoraDetalle)).setText(
                    "Hora de captura: " + EvidenceFormat.hour(evidence.capturedAt));
        } catch (IOException error) {
            showError(error);
            finish();
            return;
        }
        findViewById(R.id.btnExportarFoto).setOnClickListener(view ->
                savePhoto.launch("evidencia_" + evidence.id + ".jpg"));
        findViewById(R.id.btnCompartir).setOnClickListener(view -> share());
        findViewById(R.id.btnFichaPdf).setOnClickListener(view ->
                savePdf.launch("ficha_" + evidence.id + ".pdf"));
        findViewById(R.id.btnEliminarEvidencia).setOnClickListener(view -> confirmDelete());
    }

    private void copyPhoto(Uri destination) {
        try (InputStream input = Files.newInputStream(store.photo(evidence).toPath());
             OutputStream output = getContentResolver().openOutputStream(destination)) {
            if (output == null) throw new IOException("No se pudo abrir el destino");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            toast("Fotografía guardada en el destino elegido");
        } catch (IOException error) {
            showError(error);
        }
    }

    private void share() {
        try {
            EvidenceShare.send(this, store, evidence);
        } catch (Exception error) {
            showError(error);
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar evidencia")
                .setMessage("Se borrarán la ficha y su fotografía. Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    try {
                        store.delete(evidence);
                        toast("Evidencia eliminada");
                        finish();
                    } catch (IOException error) {
                        showError(error);
                        if (!stillExists()) finish();
                    }
                }).show();
    }

    private boolean stillExists() {
        try {
            return store.find(evidence.id) != null;
        } catch (IOException ignored) {
            return true;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showError(Exception error) {
        toast(error.getMessage() == null ? "Ocurrió un error" : error.getMessage());
    }
}
