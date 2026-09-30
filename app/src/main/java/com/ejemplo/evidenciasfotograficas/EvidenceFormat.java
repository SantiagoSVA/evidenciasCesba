package com.ejemplo.evidenciasfotograficas;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class EvidenceFormat {
    private EvidenceFormat() { }

    static String date(long time) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(time));
    }

    static String hour(long time) {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(time));
    }
}
