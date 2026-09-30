package com.ejemplo.evidenciasfotograficas;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_DRAFT = "draft";
    private static final String KEY_CAMERA = "camera";
    private static final String KEY_TIME = "time";
    private static final String KEY_PDF_ID = "pdf_id";

    private ImageView imgFoto;
    private EditText edtTitulo;
    private EditText edtDescripcion;
    private TextView txtEstado;
    private TextView txtFecha;
    private TextView txtHora;
    private TextView txtContador;
    private TextView txtSinFoto;
    private Button btnEliminarFoto;
    private Button btnGuardarFoto;
    private Button btnCompartirEvidencias;
    private Button btnGenerarFicha;
    private EvidenceStore store;
    private File draftFile;
    private File cameraFile;
    private long capturedAt;
    private String selectedPdfId;

    private final ActivityResultLauncher<Uri> launcherCamara =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraFile != null && cameraFile.length() > 0) {
                    if (draftFile != null && !draftFile.equals(cameraFile)) draftFile.delete();
                    draftFile = cameraFile;
                    capturedAt = System.currentTimeMillis();
                    txtEstado.setText("Estado: fotografía lista para registrar");
                    toast("Fotografía recibida");
                } else {
                    if (cameraFile != null) cameraFile.delete();
                    txtEstado.setText(draftFile == null
                            ? "Estado: captura cancelada" : "Estado: se conserva la foto anterior");
                    toast("La captura se canceló o no produjo una imagen");
                }
                cameraFile = null;
                showDraft();
            });

    private final ActivityResultLauncher<String> exportPhoto =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("image/jpeg"), uri -> {
                if (uri == null || draftFile == null) return;
                try (InputStream input = Files.newInputStream(draftFile.toPath());
                     OutputStream output = getContentResolver().openOutputStream(uri)) {
                    if (output == null) throw new IOException("No se pudo abrir el destino");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    toast("Fotografía guardada en el destino elegido");
                } catch (IOException error) {
                    showError(error);
                }
            });

    private final ActivityResultLauncher<String> exportPdf =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                String id = selectedPdfId;
                selectedPdfId = null;
                if (uri == null || id == null) return;
                try {
                    Evidence selected = store.find(id);
                    if (selected == null) throw new IOException("La evidencia ya no existe");
                    try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                        if (output == null) throw new IOException("No se pudo abrir el destino");
                        EvidencePdf.write(selected, store.photo(selected), output);
                    }
                    toast("Ficha PDF generada");
                } catch (IOException error) {
                    showError(error);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        store = new EvidenceStore(this);

        imgFoto = findViewById(R.id.imgFoto);
        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        txtEstado = findViewById(R.id.txtEstado);
        txtFecha = findViewById(R.id.txtFecha);
        txtHora = findViewById(R.id.txtHora);
        txtContador = findViewById(R.id.txtContador);
        txtSinFoto = findViewById(R.id.txtSinFoto);
        btnEliminarFoto = findViewById(R.id.btnEliminarFoto);
        btnGuardarFoto = findViewById(R.id.btnGuardarFoto);
        btnCompartirEvidencias = findViewById(R.id.btnCompartirEvidencias);
        btnGenerarFicha = findViewById(R.id.btnGenerarFicha);

        if (savedInstanceState != null) {
            String draftPath = savedInstanceState.getString(KEY_DRAFT);
            String cameraPath = savedInstanceState.getString(KEY_CAMERA);
            if (draftPath != null) draftFile = new File(draftPath);
            if (cameraPath != null) cameraFile = new File(cameraPath);
            capturedAt = savedInstanceState.getLong(KEY_TIME);
            selectedPdfId = savedInstanceState.getString(KEY_PDF_ID);
        }

        findViewById(R.id.btnTomarFoto).setOnClickListener(view -> abrirCamara());
        findViewById(R.id.btnTomarOtra).setOnClickListener(view -> abrirCamara());
        btnEliminarFoto.setOnClickListener(view -> confirmarEliminarFoto());
        findViewById(R.id.btnGuardar).setOnClickListener(view -> guardarEvidencia());
        btnGuardarFoto.setOnClickListener(view -> {
            if (draftFile != null) exportPhoto.launch("fotografia_" + capturedAt + ".jpg");
        });
        findViewById(R.id.btnGaleria).setOnClickListener(view ->
                startActivity(new Intent(this, GalleryActivity.class)));
        btnCompartirEvidencias.setOnClickListener(view -> chooseEvidence(false));
        btnGenerarFicha.setOnClickListener(view -> chooseEvidence(true));
        showDraft();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (txtContador != null) refreshCount();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_DRAFT, draftFile == null ? null : draftFile.getAbsolutePath());
        outState.putString(KEY_CAMERA, cameraFile == null ? null : cameraFile.getAbsolutePath());
        outState.putLong(KEY_TIME, capturedAt);
        outState.putString(KEY_PDF_ID, selectedPdfId);
        super.onSaveInstanceState(outState);
    }

    private void abrirCamara() {
        Intent check = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (check.resolveActivity(getPackageManager()) == null) {
            toast("No se encontró una aplicación de cámara");
            return;
        }
        try {
            cameraFile = new File(store.photosDir(), UUID.randomUUID() + ".jpg");
            if (!cameraFile.createNewFile()) throw new IOException("No se pudo crear el archivo");
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", cameraFile);
            launcherCamara.launch(uri);
        } catch (IOException | ActivityNotFoundException error) {
            if (cameraFile != null) cameraFile.delete();
            cameraFile = null;
            showError(error);
        }
    }

    private void guardarEvidencia() {
        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();
        if (titulo.isEmpty()) {
            edtTitulo.setError("Escribe un título");
            edtTitulo.requestFocus();
            return;
        }
        if (descripcion.isEmpty()) {
            edtDescripcion.setError("Escribe una descripción");
            edtDescripcion.requestFocus();
            return;
        }
        if (draftFile == null || !draftFile.isFile()) {
            toast("Primero debes tomar una fotografía");
            return;
        }
        Evidence evidence = new Evidence(UUID.randomUUID().toString(), titulo,
                descripcion, draftFile.getName(), capturedAt);
        try {
            store.add(evidence);
            draftFile = null;
            capturedAt = 0;
            edtTitulo.setText("");
            edtDescripcion.setText("");
            txtEstado.setText("Estado: evidencia registrada correctamente");
            showDraft();
            refreshCount();
            toast("Evidencia guardada");
        } catch (IOException error) {
            showError(error);
        }
    }

    private void confirmarEliminarFoto() {
        if (draftFile == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Eliminar foto actual")
                .setMessage("Se descartará la fotografía que aún no está registrada.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    if (!draftFile.delete()) {
                        toast("No se pudo eliminar la fotografía");
                        return;
                    }
                    draftFile = null;
                    capturedAt = 0;
                    txtEstado.setText("Estado: fotografía eliminada");
                    showDraft();
                }).show();
    }

    private void showDraft() {
        boolean hasPhoto = draftFile != null && draftFile.isFile();
        txtSinFoto.setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        btnEliminarFoto.setEnabled(hasPhoto);
        btnGuardarFoto.setEnabled(hasPhoto);
        if (hasPhoto) {
            try {
                PhotoUtils.show(draftFile, imgFoto);
                txtFecha.setText("Fecha de captura: " + EvidenceFormat.date(capturedAt));
                txtHora.setText("Hora de captura: " + EvidenceFormat.hour(capturedAt));
            } catch (IOException error) {
                imgFoto.setImageDrawable(null);
                showError(error);
            }
        } else {
            imgFoto.setImageDrawable(null);
            txtFecha.setText("Fecha de captura: —");
            txtHora.setText("Hora de captura: —");
        }
    }

    private void refreshCount() {
        try {
            int count = store.all().size();
            txtContador.setText(String.valueOf(count));
            btnCompartirEvidencias.setEnabled(count > 0);
            btnGenerarFicha.setEnabled(count > 0);
        } catch (IOException error) {
            showError(error);
        }
    }

    private void chooseEvidence(boolean pdf) {
        try {
            List<Evidence> items = store.all();
            if (items.isEmpty()) {
                toast("Primero guarda una evidencia");
                return;
            }
            String[] choices = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                Evidence item = items.get(i);
                choices[i] = item.title + "  ·  " + EvidenceFormat.date(item.capturedAt);
            }
            new AlertDialog.Builder(this)
                    .setTitle(pdf ? "Elegir evidencia para la ficha" : "Elegir evidencia para compartir")
                    .setItems(choices, (dialog, which) -> {
                        Evidence selected = items.get(which);
                        if (pdf) {
                            selectedPdfId = selected.id;
                            exportPdf.launch("ficha_" + selected.id + ".pdf");
                        } else {
                            try {
                                EvidenceShare.send(this, store, selected);
                            } catch (Exception error) {
                                showError(error);
                            }
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } catch (IOException error) {
            showError(error);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showError(Exception error) {
        toast(error.getMessage() == null ? "Ocurrió un error" : error.getMessage());
    }
}
