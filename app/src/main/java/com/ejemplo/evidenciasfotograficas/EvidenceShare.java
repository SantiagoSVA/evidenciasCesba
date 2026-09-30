package com.ejemplo.evidenciasfotograficas;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.IOException;

final class EvidenceShare {
    private EvidenceShare() { }

    static void send(AppCompatActivity activity, EvidenceStore store, Evidence evidence)
            throws IOException {
        Uri photoUri = FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".fileprovider", store.photo(evidence));
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, photoUri);
        intent.putExtra(Intent.EXTRA_TEXT,
                evidence.title + "\n" + evidence.description + "\nCapturada: "
                        + EvidenceFormat.date(evidence.capturedAt) + " "
                        + EvidenceFormat.hour(evidence.capturedAt));
        intent.setClipData(ClipData.newUri(activity.getContentResolver(), "Evidencia", photoUri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(intent, "Compartir evidencia"));
    }
}
