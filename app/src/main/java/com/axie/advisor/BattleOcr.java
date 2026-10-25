package com.axie.advisor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.File;

/** On-device OCR. Recognition output is unverified and must not be treated as battle facts. */
public final class BattleOcr {
    public interface Callback { void done(String text); }
    private BattleOcr() {}
    public static void analyze(Context context, Callback callback) {
        File file = new File(context.getFilesDir(), "latest_capture.png");
        if (!file.exists()) { callback.done("No capture found. Start screen capture first."); return; }
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) { callback.done("Could not decode captured image."); return; }
        com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(InputImage.fromBitmap(bitmap,0))
            .addOnSuccessListener(result -> {
                String text=result.getText();
                callback.done(text.trim().isEmpty()?"No readable text detected.":text);
            })
            .addOnFailureListener(e -> callback.done("OCR error: "+e.getMessage()))
            .addOnCompleteListener(task -> {recognizer.close();bitmap.recycle();});
    }
}
