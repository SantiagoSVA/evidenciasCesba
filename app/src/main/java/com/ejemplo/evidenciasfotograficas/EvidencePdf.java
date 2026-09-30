package com.ejemplo.evidenciasfotograficas;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

final class EvidencePdf {
    private static final int WIDTH = 595;
    private static final int HEIGHT = 842;
    private static final int LEFT = 40;
    private static final int RIGHT = 555;

    private EvidencePdf() { }

    static void write(Evidence evidence, File photo, OutputStream output) throws IOException {
        PdfDocument document = new PdfDocument();
        try {
            Writer writer = new Writer(document);
            writer.startPage();
            writer.text("Estado: registrada", 13, false);
            writer.text("Fecha de captura: " + EvidenceFormat.date(evidence.capturedAt), 13, false);
            writer.text("Hora de captura: " + EvidenceFormat.hour(evidence.capturedAt), 13, false);
            writer.gap(12);
            writer.text("Título", 12, true);
            writer.text(evidence.title, 16, true);
            writer.gap(8);
            writer.text("Descripción", 12, true);
            writer.text(evidence.description, 13, false);
            writer.gap(16);
            writer.photo(photo);
            writer.finishPage();
            document.writeTo(output);
        } finally {
            document.close();
        }
    }

    private static final class Writer {
        private final PdfDocument document;
        private PdfDocument.Page page;
        private Canvas canvas;
        private int pageNumber;
        private float y;

        Writer(PdfDocument document) {
            this.document = document;
        }

        void startPage() {
            pageNumber++;
            page = document.startPage(new PdfDocument.PageInfo.Builder(
                    WIDTH, HEIGHT, pageNumber).create());
            canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);
            Paint header = new Paint(Paint.ANTI_ALIAS_FLAG);
            header.setColor(0xFF173552);
            canvas.drawRect(0, 0, WIDTH, 110, header);
            header.setColor(0xFFA9DAD5);
            header.setTextSize(11);
            header.setFakeBoldText(true);
            canvas.drawText("REGISTRO VISUAL", LEFT, 39, header);
            header.setColor(Color.WHITE);
            header.setTextSize(22);
            canvas.drawText("Ficha de evidencia", LEFT, 73, header);
            y = 145;
        }

        void finishPage() {
            if (page == null) return;
            Paint footer = new Paint(Paint.ANTI_ALIAS_FLAG);
            footer.setColor(Color.GRAY);
            footer.setTextSize(10);
            canvas.drawLine(LEFT, HEIGHT - 47, RIGHT, HEIGHT - 47, footer);
            canvas.drawText("Evidencias fotográficas · Página " + pageNumber,
                    LEFT, HEIGHT - 28, footer);
            document.finishPage(page);
            page = null;
        }

        void gap(float amount) {
            y += amount;
        }

        void text(String value, int size, boolean bold) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(bold ? 0xFF173552 : 0xFF263746);
            paint.setTextSize(size);
            if (bold) paint.setFakeBoldText(true);
            float lineHeight = size * 1.45f;
            for (String paragraph : value.split("\\n", -1)) {
                String rest = paragraph.trim();
                if (rest.isEmpty()) {
                    y += lineHeight;
                    continue;
                }
                while (!rest.isEmpty()) {
                    int max = paint.breakText(rest, true, RIGHT - LEFT, null);
                    if (max < 1) max = 1;
                    int cut = max;
                    if (max < rest.length()) {
                        int space = rest.lastIndexOf(' ', max - 1);
                        if (space > 0) cut = space;
                    }
                    if (y + lineHeight > HEIGHT - 55) {
                        finishPage();
                        startPage();
                    }
                    canvas.drawText(rest.substring(0, cut).trim(), LEFT, y, paint);
                    y += lineHeight;
                    rest = rest.substring(cut).trim();
                }
            }
        }

        void photo(File file) throws IOException {
            Bitmap bitmap = PhotoUtils.sampled(file, 1200, 1200);
            try {
                float maxWidth = RIGHT - LEFT;
                float maxHeight = 420;
                float scale = Math.min(maxWidth / bitmap.getWidth(),
                        maxHeight / bitmap.getHeight());
                float width = bitmap.getWidth() * scale;
                float height = bitmap.getHeight() * scale;
                if (y + height + 30 > HEIGHT - 55) {
                    finishPage();
                    startPage();
                }
                text("Fotografía", 12, true);
                gap(8);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(bitmap, null,
                        new RectF(LEFT, y, LEFT + width, y + height), paint);
                y += height + 12;
            } finally {
                bitmap.recycle();
            }
        }
    }
}
