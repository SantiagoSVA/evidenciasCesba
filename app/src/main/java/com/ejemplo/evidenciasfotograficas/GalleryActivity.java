package com.ejemplo.evidenciasfotograficas;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private EvidenceStore store;
    private LinearLayout list;
    private TextView summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        store = new EvidenceStore(this);
        list = findViewById(R.id.listaEvidencias);
        summary = findViewById(R.id.txtResumenGaleria);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        list.removeAllViews();
        try {
            List<Evidence> items = store.all();
            summary.setText("Fotografías registradas: " + items.size());
            if (items.isEmpty()) {
                LinearLayout emptyCard = new LinearLayout(this);
                emptyCard.setOrientation(LinearLayout.VERTICAL);
                emptyCard.setPadding(dp(22), dp(24), dp(22), dp(24));
                emptyCard.setBackgroundResource(R.drawable.bg_card);
                emptyCard.addView(label("Aún no hay evidencias", 19, true));
                emptyCard.addView(label("Toma una fotografía y guarda su título y descripción para verla aquí.", 14, false));
                list.addView(emptyCard);
                return;
            }
            for (Evidence evidence : items) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setPadding(dp(14), dp(14), dp(14), dp(14));
                card.setBackgroundResource(R.drawable.bg_card);
                card.setElevation(dp(2));
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.bottomMargin = dp(12);
                list.addView(card, cardParams);

                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setBackgroundResource(R.drawable.photo_frame);
                image.setContentDescription("Fotografía de " + evidence.title);
                card.addView(image, new LinearLayout.LayoutParams(dp(90), dp(90)));
                try {
                    PhotoUtils.show(store.photo(evidence), image);
                } catch (IOException error) {
                    image.setBackgroundColor(0xFFCCD8E3);
                }

                LinearLayout content = new LinearLayout(this);
                content.setOrientation(LinearLayout.VERTICAL);
                content.setPadding(dp(14), 0, 0, 0);
                card.addView(content, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                TextView title = label(evidence.title, 18, true);
                content.addView(title);
                content.addView(label(EvidenceFormat.date(evidence.capturedAt) + " · "
                        + EvidenceFormat.hour(evidence.capturedAt), 13, false));
                content.addView(label("Estado: registrada", 13, false));
                TextView open = label("Ver ficha  ›", 14, true);
                open.setTextColor(0xFF167B74);
                content.addView(open);

                card.setClickable(true);
                card.setFocusable(true);
                card.setOnClickListener(view -> {
                    Intent detail = new Intent(this, DetailActivity.class);
                    detail.putExtra(DetailActivity.EXTRA_ID, evidence.id);
                    startActivity(detail);
                });
            }
        } catch (IOException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private TextView label(String text, int sizeSp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(bold ? 0xFF173552 : 0xFF526273);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        view.setPadding(0, 0, 0, dp(7));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
