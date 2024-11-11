package com.alexzava.krypto;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.alexzava.krypto.databinding.FragmentNewKeyPairBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.goterl.lazysodium.LazySodium;
import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.SodiumAndroid;
import com.goterl.lazysodium.interfaces.KeyExchange;
import com.goterl.lazysodium.utils.KeyPair;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewKeyPairFragment extends Fragment {
    private final String LOG_TAG = this.getClass().getSimpleName();

    FragmentNewKeyPairBinding binding;
    LazySodium sodium;
    BottomSheetBehavior bottomSheetBehavior;

    public NewKeyPairFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentNewKeyPairBinding.inflate(getLayoutInflater());
        sodium = new LazySodiumAndroid(new SodiumAndroid());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set listeners
        binding.generateButton.setOnClickListener(generateKeyPairAction());
        binding.personalPrivateKeyLayout.setEndIconOnClickListener(copyPrivateKeyAction());
        binding.exportPrivateKeyButton.setOnClickListener(exportPrivateKeyAction());
        binding.personalPublicKeyLayout.setEndIconOnClickListener(copyPublicKeyButtonAction());
        binding.shareButton.setOnClickListener(shareButtonAction());

        // Bottom Sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.viewShareBottomSheet.bottomSheet);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.bottomSheetBackground.setAlpha(0);
        binding.viewShareBottomSheet.copyLinkButton.setOnClickListener(copyLinkAction());
        binding.viewShareBottomSheet.copyPublicKeyButton.setOnClickListener(copyPublicKeyAction());
        binding.viewShareBottomSheet.linkSwitch.setOnCheckedChangeListener(switchOnCheckedChanged());
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback());

        // App toolbar
        binding.viewAppToolBar.topAppBar.setNavigationOnClickListener(goBackAction());
        binding.viewAppToolBar.topAppBar.setOnMenuItemClickListener(menuItemClickListener());

        // Generate new key pair on startup
        generateKeyPair();
    }

    // Generate random X25519 key pair
    private void generateKeyPair() {
        // Generate new key pair
        KeyPair keyPair = sodium.cryptoKxKeypair();
        String privateKey = Utils.toBase64String(keyPair.getSecretKey().getAsBytes());
        String publicKey = Utils.toBase64String(keyPair.getPublicKey().getAsBytes());

        binding.personalPrivateKeyText.setText(privateKey);
        binding.personalPublicKeyText.setText(publicKey);

        // Generate QR code
        generateShareQRCode();

        // Show share button
        binding.shareButton.setVisibility(View.VISIBLE);
        binding.exportPrivateKeyButton.setVisibility(View.VISIBLE);
    }

    private View.OnClickListener generateKeyPairAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateKeyPair();
            }
        };
    }

    // Open bottom sheet
    private View.OnClickListener shareButtonAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show bottom sheet
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        };
    }

    // Copy public key to clipboard
    private View.OnClickListener copyPublicKeyButtonAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(binding.personalPublicKeyText.getText() != null && !binding.personalPublicKeyText.getText().toString().isEmpty()) {
                    Utils.copyToClipboard(requireContext(), getString(R.string.public_key_input_view_personal_public_key_input_hint), binding.personalPublicKeyText.getText().toString());
                    Toast.makeText(requireContext(), R.string.public_key_copied, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    // Export private key to download folder
    private View.OnClickListener exportPrivateKeyAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(binding.personalPrivateKeyText.getText() == null ||
                        binding.personalPrivateKeyText.getText().toString().isEmpty()) {
                    Toast.makeText(requireContext(), R.string.error_generic_invalid_private_key, Toast.LENGTH_SHORT).show();
                    return;
                }

                String privateKey = binding.personalPrivateKeyText.getText().toString();

                if(Utils.fromBase64(privateKey).length != KeyExchange.SECRETKEYBYTES) {
                    Toast.makeText(requireContext(), R.string.error_generic_invalid_private_key, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Generate file name
                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
                String outputFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "private_" + dateFormat.format(new Date()) + ".key";

                try {
                    OutputStream outputStream = Files.newOutputStream(Paths.get(outputFileName));
                    outputStream.write(privateKey.getBytes());
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, e.toString());
                    Toast.makeText(requireContext(), R.string.error_generic_unable_to_export_private_key, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(requireContext(), R.string.private_key_export_success, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private View.OnClickListener goBackAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().popBackStackImmediate();
            }
        };
    }

    private Toolbar.OnMenuItemClickListener menuItemClickListener() {
        return new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.aboutMenuItem) {
                    Utils.navigateToGitRepo(requireContext());
                    return true;
                }
                return false;
            }
        };
    }

    /* Share Bottom Sheet */

    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback() {
        return new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if(newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                binding.bottomSheetBackground.setAlpha(slideOffset);
            }
        };
    }

    // Generate QR code
    private void generateShareQRCode() {
        if(binding.personalPublicKeyText.getText() != null && !binding.personalPublicKeyText.getText().toString().isEmpty()) {
            String shareLink = Utils.generateShareLink(binding.viewShareBottomSheet.linkSwitch.isChecked(), binding.personalPublicKeyText.getText().toString());
            Bitmap qrCode = Utils.generateQRCodeBitmap(requireContext(), shareLink);
            if(qrCode != null) {
                binding.viewShareBottomSheet.shareQrCodeImage.setImageBitmap(qrCode);
            }
        }
    }

    // Copy link
    private View.OnClickListener copyLinkAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shareLink = Utils.generateShareLink(binding.viewShareBottomSheet.linkSwitch.isChecked(), binding.personalPublicKeyText.getText().toString());
                Utils.copyToClipboard(requireContext(), "Share link", shareLink);
                Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private View.OnClickListener copyPrivateKeyAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.copyToClipboard(requireContext(), "Private Key", binding.personalPrivateKeyText.getText().toString());
                Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private View.OnClickListener copyPublicKeyAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.copyToClipboard(requireContext(), "Public Key", binding.personalPublicKeyText.getText().toString());
                Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private CompoundButton.OnCheckedChangeListener switchOnCheckedChanged() {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                generateShareQRCode();
            }
        };
    }
}