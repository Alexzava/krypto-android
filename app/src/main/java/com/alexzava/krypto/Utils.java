package com.alexzava.krypto;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.goterl.lazysodium.interfaces.KeyExchange;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;

import qrcode.QRCode;
import qrcode.QRCodeBuilder;
import qrcode.color.Colors;

public class Utils {
    public static String getFileName(Context context, Uri uri) {
        String fileName;
        Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        fileName = cursor.getString(nameIndex);
        cursor.close();
        return fileName;
    }

    public static long getFileSize(Context context, Uri uri) {
        long fileSize;
        Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null);
        int sizeIndex  = cursor.getColumnIndex(OpenableColumns.SIZE);
        cursor.moveToFirst();
        fileSize = cursor.getLong(sizeIndex);
        cursor.close();
        return fileSize;
    }

    public static String toBase64String(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
    }

    public static byte[] fromBase64(String encodedStr) {
        return Base64.decode(encodedStr, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
    }

    public static Bitmap generateQRCodeBitmap(Context context, String content) {
        QRCode qrCode;
        int nightModeFlags =
                context.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        if(nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
            qrCode = QRCode.ofRoundedSquares()
                    .build(content);
        } else {
            qrCode = QRCode.ofRoundedSquares()
                    .withColor(Colors.css("#FFFFFF"))
                    .withBackgroundColor(Colors.css("#000000"))
                    .build(content);
        }
        byte[] qrCodeBytes = qrCode.renderToBytes();
        return BitmapFactory.decodeByteArray(qrCodeBytes, 0, qrCodeBytes.length);
    }

    public static String generateShareLink(boolean isExtLink, String publicKey) {
        if(isExtLink) {
            return Constants.LINK_HAT_SH + publicKey;
        } else {
            return Constants.LINK_APP + publicKey;
        }
    }

    public static void copyToClipboard(Context context, String label, String content) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, content));
    }

    public static void navigateToGitRepo(Context context) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.REPO_URL));
        context.startActivity(browserIntent);
    }
}
