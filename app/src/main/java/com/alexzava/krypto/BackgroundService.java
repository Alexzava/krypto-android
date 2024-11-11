package com.alexzava.krypto;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.goterl.lazysodium.LazySodium;
import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.SodiumAndroid;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.interfaces.SecretStream;
import com.goterl.lazysodium.utils.Key;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BackgroundService extends Service {
    private final String LOG_TAG = this.getClass().getSimpleName();
    LazySodium sodium;

    Thread processThread;
    Intent progressBroadcastIntent;
    NotificationManager notificationManager;

    // Args
    Uri fileUri;
    String action;
    String keyHex;
    byte[] keySalt;
    String keyMode;

    // File info
    String fileName;
    long fileSize;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sodium = new LazySodiumAndroid(new SodiumAndroid());

        // Get args (from CompleteActionFragment)
        Bundle args = intent.getExtras();
        if(args == null ||
                !args.containsKey(Constants.ARG_SERVICE_URI) ||
                !args.containsKey(Constants.ARG_SERVICE_ACTION) ||
                !args.containsKey(Constants.ARG_SERVICE_KEY_HEX) ||
                !args.containsKey(Constants.ARG_SERVICE_SALT_HEX) ||
                !args.containsKey(Constants.ARG_SERVICE_KEY_MODE)) {
            Log.e(LOG_TAG, "Invalid args");
            return super.onStartCommand(intent, flags, startId);
        }

        fileUri = Uri.parse(args.getString(Constants.ARG_SERVICE_URI));
        action = args.getString(Constants.ARG_SERVICE_ACTION);
        keyHex = args.getString(Constants.ARG_SERVICE_KEY_HEX);
        keySalt = sodium.sodiumHex2Bin(args.getString(Constants.ARG_SERVICE_SALT_HEX));
        keyMode = args.getString(Constants.ARG_SERVICE_KEY_MODE);

        fileName = Utils.getFileName(getApplicationContext(), fileUri);
        fileSize = Utils.getFileSize(getApplicationContext(), fileUri);

        // Set progress broadcast
        progressBroadcastIntent = new Intent(Constants.PROGRESS_BROADCAST_NAME);

        // Notification manager
        notificationManager = getSystemService(NotificationManager.class);

        // Perform encryption or decryption
        processThread = new Thread(processThreadRunnable());
        processThread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Send progress update to activity broadcast and notification
    private void updateProgress(long bytesRead, boolean isDone, boolean error) {
        if(error) {
            // Send error to activity broadcast
            progressBroadcastIntent.putExtra(Constants.BROADCAST_RECEIVER_PROGRESS, 0);
            progressBroadcastIntent.putExtra(Constants.BROADCAST_RECEIVER_IS_DONE, true);
            progressBroadcastIntent.putExtra(Constants.BROADCAST_RECEIVER_DECRYPTION_ERROR, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(progressBroadcastIntent);

            // Notification
            notifyProgress(0, true, true);
        } else {
            int progress = (int) ((bytesRead * 100L) / fileSize);

            // Broadcast
            progressBroadcastIntent.putExtra(Constants.BROADCAST_RECEIVER_PROGRESS, progress);
            if(isDone) {
                progressBroadcastIntent.putExtra(Constants.BROADCAST_RECEIVER_IS_DONE, true);
            }
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(progressBroadcastIntent);

            // Notification
            notifyProgress(progress, isDone, false);
        }
    }

    // Create a notification
    private void notifyProgress(int progress, boolean isDone, boolean error) {
        String title;
        String content;
        int icon;

        if(error) {
            title = "Error";
            content = getString(R.string.complete_fragment_wrong_password);
            icon = R.drawable.ic_round_error_24;
        } else {
            if(isDone) {
                title = getString(R.string.notification_done_title);
                content = getString(R.string.notification_done_content);
                icon = R.drawable.ic_round_done_24;
            } else {
                title = getString(R.string.notification_processing_title);
                content = progress + "%";
                icon = R.drawable.ic_round_enhanced_encryption_24;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_PROGRESS_CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if(!isDone) {
            builder.setProgress(100, progress, false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(1, builder.build());
            }
        }
    }

    private Runnable processThreadRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if(action.equals(Constants.ACTION_ENCRYPT)) {
                    encryptFile();
                } else {
                    decryptFile();
                }
            }
        };
    }

    private void encryptFile() {
        // Init header and secret stream
        byte[] header = sodium.randomBytesBuf(SecretStream.HEADERBYTES);
        SecretStream.State state;
        try {
            state = sodium.cryptoSecretStreamInitPush(header, Key.fromHexString(keyHex));
        } catch (SodiumException e) {
            throw new RuntimeException(e);
        }

        String outputFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + fileName + ".enc";
        try {
            InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(fileUri);
            OutputStream outputStream = Files.newOutputStream(Paths.get(outputFileName));

            // Add Signature, Salt and Header to output stream
            if(keyMode.equals(Constants.MODE_PASSWORD)) {
                outputStream.write(Constants.SIGNATURE_SYMMETRIC.getBytes());
                outputStream.write(keySalt); // Write key salt to file
            } else {
                outputStream.write(Constants.SIGNATURE_ASYMMETRIC.getBytes());
            }

            outputStream.write(header);

            // Encrypt chunk
            byte[] buffer = new byte[Constants.CHUNK_SIZE];
            int bytesRead = 0;
            long progress = 0;
            while((bytesRead = inputStream.read(buffer)) != -1) {

                byte[] cipher = new byte[SecretStream.ABYTES + bytesRead];

                byte tag = SecretStream.TAG_MESSAGE;
                if(bytesRead < Constants.CHUNK_SIZE) {
                    tag = SecretStream.TAG_FINAL;
                }

                sodium.cryptoSecretStreamPush(state, cipher, buffer, bytesRead, tag);
                outputStream.write(cipher);

                // Update progress
                progress += bytesRead;
                updateProgress(progress, false, false);
            }
            outputStream.close();
            inputStream.close();
            updateProgress(progress, true, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void decryptFile() {
        // Init header and secret stream
        byte[] header = new byte[SecretStream.HEADERBYTES];

        String outputFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + fileName.replace(".enc", "");
        try {
            InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(fileUri);
            OutputStream outputStream = Files.newOutputStream(Paths.get(outputFileName));

            // Skip Signature and Salt
            if(keyMode.equals(Constants.MODE_PASSWORD)) {
                inputStream.skip(Constants.SIGNATURE_SYMMETRIC.getBytes().length + keySalt.length);
            } else {
                inputStream.skip(Constants.SIGNATURE_ASYMMETRIC.getBytes().length);
            }

            // Read header
            inputStream.read(header);

            SecretStream.State state = sodium.cryptoSecretStreamInitPull(header, Key.fromHexString(keyHex));

            // Decrypt chunk
            byte[] buffer = new byte[Constants.CHUNK_SIZE + SecretStream.ABYTES];
            int bytesRead = 0;
            long progress = 0;
            boolean decryptionError = false;
            while((bytesRead = inputStream.read(buffer)) != -1) {

                byte[] message = new byte[bytesRead - SecretStream.ABYTES];

                byte tag = SecretStream.TAG_MESSAGE;
                if(bytesRead < Constants.CHUNK_SIZE) {
                    tag = SecretStream.TAG_FINAL;
                }

                if(!sodium.cryptoSecretStreamPull(state, message, new byte[]{tag}, buffer, bytesRead)) {
                    decryptionError = true;
                    break;
                }
                outputStream.write(message);

                // Update progress
                progress += bytesRead;
                updateProgress(progress, false, false);
            }
            inputStream.close();
            outputStream.close();

            if(decryptionError) {
                // Delete output file
                new File(outputFileName).delete();
                // Send error to broadcast
                updateProgress(progress, true, true);
            } else {
                updateProgress(progress, true, false);
            }

        } catch (IOException | SodiumException e) {
            throw new RuntimeException(e);
        }
    }
}
