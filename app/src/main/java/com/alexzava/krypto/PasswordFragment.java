package com.alexzava.krypto;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alexzava.krypto.databinding.FragmentPasswordBinding;
import com.google.android.material.tabs.TabLayout;
import com.goterl.lazysodium.LazySodium;
import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.SodiumAndroid;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.interfaces.KeyExchange;
import com.goterl.lazysodium.interfaces.PwHash;
import com.goterl.lazysodium.interfaces.Stream;
import com.goterl.lazysodium.utils.Key;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;
import com.nareshchocha.filepickerlibrary.ui.FilePicker;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

public class PasswordFragment extends Fragment {
    private final String LOG_TAG = this.getClass().getSimpleName();
    FragmentPasswordBinding binding;
    LazySodium sodium;

    Uri uri;
    String scannedPublicKey;
    String action;
    String keyMode;

    public PasswordFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentPasswordBinding.inflate(getLayoutInflater());
        sodium = new LazySodiumAndroid(new SodiumAndroid());
        keyMode = Constants.MODE_PASSWORD;
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

        // Get args
        uri = Uri.parse(PasswordFragmentArgs.fromBundle(getArguments()).getUriStr());
        action = PasswordFragmentArgs.fromBundle(getArguments()).getAction();
        scannedPublicKey = PasswordFragmentArgs.fromBundle(getArguments()).getScannedPublicKey();

        // Read public key from app link
        Intent intent = requireActivity().getIntent();
        Uri data = intent.getData();
        if(data != null || !scannedPublicKey.isEmpty()) {
            // Show public key tab
            keyMode = Constants.MODE_PUBLIC_KEY;
            binding.viewInputPublicKey.getRoot().setVisibility(View.VISIBLE);
            binding.viewInputPassword.getRoot().setVisibility(View.INVISIBLE);

            if(data != null) {
                binding.viewInputPublicKey.recipientPublicKeyText.setText(data.getLastPathSegment());
            } else {
                binding.viewInputPublicKey.recipientPublicKeyText.setText(scannedPublicKey);
            }

            TabLayout.Tab publicKeyTab = binding.modeTabSelector.getTabAt(1);
            binding.modeTabSelector.selectTab(publicKeyTab);
        }

        if(action.equals(Constants.ACTION_ENCRYPT)) {
            binding.nextActionButtonHintText.setText(getString(R.string.password_fragment_proceed_to_encrypt));
        } else {
            binding.nextActionButtonHintText.setText(getString(R.string.password_fragment_proceed_to_decrypt));

            binding.viewInputPublicKey.personalPublicKeyLayout.setEnabled(false);
            binding.viewInputPublicKey.personalPrivateKeyLayout.setHelperText(getString(R.string.personal_private_key_input_helper_disabled));
        }

        // Set listeners
        binding.modeTabSelector.addOnTabSelectedListener(tabSelectorListener());
        binding.nextActionButton.setOnClickListener(generateKeyThenNavigateToNextFragment());
        binding.viewInputPassword.generatePasswordButton.setOnClickListener(generateRandomPasswordAction());
        binding.viewInputPassword.passwordTextInput.addTextChangedListener(passwordInputTextWatcher());
        binding.viewInputPublicKey.generateButton.setOnClickListener(generateKeyPairAction());
        binding.viewInputPublicKey.personalPublicKeyLayout.setEndIconOnClickListener(copyPublicKeyAction());
        binding.viewInputPublicKey.personalPrivateKeyLayout.setEndIconOnClickListener(importPrivateKeyAction());
        binding.viewInputPublicKey.recipientPublicKeyLayout.setEndIconOnClickListener(scanQrCodeAction());

        // App toolbar
        binding.viewAppToolBar.topAppBar.setNavigationOnClickListener(goBackAction());
        binding.viewAppToolBar.topAppBar.setOnMenuItemClickListener(menuItemClickListener());
    }

    // Import private key launcher
    private final ActivityResultLauncher<Intent> activityImportPrivateKeyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if(uri == null) {
                            Toast.makeText(requireContext(), R.string.error_generic_unable_to_open_file, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Read private key from file
                        byte[] buffer = new byte[1024];
                        int bytesRead = 0;
                        try {
                            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                            if(inputStream == null) {
                                Toast.makeText(requireContext(), R.string.error_generic_unable_to_open_file, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            bytesRead = inputStream.read(buffer);
                            inputStream.close();
                        } catch (IOException e) {
                            Toast.makeText(requireContext(), R.string.error_generic_unable_to_open_file, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Decode private key
                        String privateKeyEncoded = new String(Arrays.copyOfRange(buffer, 0, bytesRead));
                        byte[] privateKeyBytes = Utils.fromBase64(privateKeyEncoded);
                        if(privateKeyBytes.length != KeyExchange.SECRETKEYBYTES) {
                            Toast.makeText(requireContext(), R.string.error_generic_invalid_private_key, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Key privateKey = Key.fromBytes(privateKeyBytes);
                        Key publicKey = sodium.cryptoScalarMultBase(privateKey);

                        binding.viewInputPublicKey.personalPrivateKeyText.setText(privateKeyEncoded);
                        binding.viewInputPublicKey.personalPublicKeyText.setText(Utils.toBase64String(publicKey.getAsBytes()));
                    }
                }
            });

    // Register barcode launcher
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            new ActivityResultCallback<ScanIntentResult>() {
                @Override
                public void onActivityResult(ScanIntentResult result) {
                    if (result.getContents() == null) {
                        Toast.makeText(requireContext(), "Scan cancelled", Toast.LENGTH_LONG).show();
                    } else {
                        String scannedContent = result.getContents();
                        Uri uri = Uri.parse(scannedContent);
                        if (uri != null && uri.getHost() != null) {
                            if (uri.getHost().equals(Constants.HAT_SH_HOST) && uri.getQueryParameter(Constants.QR_CODE_PUBLIC_KEY_PARAM) != null) {
                                binding.viewInputPublicKey.recipientPublicKeyText.setText(uri.getQueryParameter(Constants.QR_CODE_PUBLIC_KEY_PARAM));
                            } else if (uri.getHost().equals(Constants.APP_HOST)) {
                                binding.viewInputPublicKey.recipientPublicKeyText.setText(uri.getLastPathSegment());
                            }
                            Toast.makeText(requireContext(), "Public key loaded!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    // Tab selector listener
    private TabLayout.OnTabSelectedListener tabSelectorListener() {
        return new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getText() == null) {
                    return;
                }
                if(tab.getText().equals(getString(R.string.password_fragment_password_tab_title))) {
                    keyMode = Constants.MODE_PASSWORD;
                    binding.viewInputPublicKey.getRoot().setVisibility(View.INVISIBLE);
                    binding.viewInputPassword.getRoot().setVisibility(View.VISIBLE);
                } else if(tab.getText().equals(getString(R.string.password_fragment_public_key_tab_title))) {
                    keyMode = Constants.MODE_PUBLIC_KEY;
                    binding.viewInputPassword.getRoot().setVisibility(View.INVISIBLE);
                    binding.viewInputPublicKey.getRoot().setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        };
    }

    private TextWatcher passwordInputTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                binding.viewInputPassword.passwordTextField.setError(null);
            }
        };
    }

    private View.OnClickListener generateKeyThenNavigateToNextFragment() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String secretKeyHex;
                String personalPublicKey = "";
                byte[] salt = new byte[PwHash.SALTBYTES];

                if(keyMode.equals(Constants.MODE_PASSWORD)) {
                    // Read or generate key salt
                    if(action.equals(Constants.ACTION_ENCRYPT)) {
                        // Generate random salt
                        salt = sodium.randomBytesBuf(PwHash.SALTBYTES);
                    } else {
                        // Read salt from file
                        try {
                            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                            if(inputStream == null) {
                                Toast.makeText(requireContext(), R.string.error_generic_unable_to_open_file, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Skip signature
                            inputStream.skip(Constants.SIGNATURE_SYMMETRIC.getBytes().length);
                            inputStream.read(salt);
                            inputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // Get password input
                    String password = binding.viewInputPassword.passwordTextInput.getText().toString();

                    // Check password
                    if(action.equals(Constants.ACTION_ENCRYPT) && (password.isEmpty() || password.length() < Constants.PASSWORD_LEN_MIN)) {
                        binding.viewInputPassword.passwordTextField.setError(getString(R.string.error_generic_password_too_short));
                        return;
                    }

                    try {
                        secretKeyHex = sodium.cryptoPwHash(password, Stream.CHACHA20_IETF_KEYBYTES, salt, PwHash.OPSLIMIT_INTERACTIVE, PwHash.MEMLIMIT_INTERACTIVE, PwHash.Alg.PWHASH_ALG_ARGON2ID13);
                    } catch (SodiumException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    byte[] privateKeyBytes = Utils.fromBase64(binding.viewInputPublicKey.personalPrivateKeyText.getText().toString());
                    byte[] publicKeyBytes = Utils.fromBase64(binding.viewInputPublicKey.recipientPublicKeyText.getText().toString());
                    personalPublicKey = binding.viewInputPublicKey.personalPublicKeyText.getText().toString();

                    if(privateKeyBytes.length != KeyExchange.SECRETKEYBYTES) {
                        Toast.makeText(requireContext(), R.string.error_generic_invalid_private_key, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(publicKeyBytes.length != KeyExchange.PUBLICKEYBYTES) {
                        Toast.makeText(requireContext(), R.string.error_generic_invalid_public_key, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(Arrays.equals(privateKeyBytes, publicKeyBytes)) {
                        Toast.makeText(requireContext(), R.string.error_generic_invalid_keys, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Key privateKey = Key.fromBytes(privateKeyBytes);
                    Key recipientPublicKey = Key.fromBytes(publicKeyBytes);
                    try {
                        if(action.equals(Constants.ACTION_ENCRYPT)) {
                            secretKeyHex = sodium.cryptoKxClientSessionKeys(sodium.cryptoScalarMultBase(privateKey), privateKey, recipientPublicKey).getTxString();
                        } else {
                            secretKeyHex = sodium.cryptoKxServerSessionKeys(sodium.cryptoScalarMultBase(privateKey), privateKey, recipientPublicKey).getRxString();
                        }
                    } catch (SodiumException e) {
                        throw new RuntimeException(e);
                    }
                }

                Navigation.findNavController(requireView()).navigate(
                        PasswordFragmentDirections.actionPasswordFragmentToActionFragment(
                            uri.toString(),
                            action,
                            secretKeyHex,
                            sodium.toHexStr(salt),
                            keyMode,
                            personalPublicKey
                        )
                );
            }
        };
    }

    // Scan public key qr code
    private View.OnClickListener scanQrCodeAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanOptions scanOptions = new ScanOptions();
                scanOptions.setPrompt("Scan Public Key Qr Code");
                scanOptions.setOrientationLocked(true);
                scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
                barcodeLauncher.launch(scanOptions);
            }
        };
    }

    // Generate random password
    private View.OnClickListener generateRandomPasswordAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random rnd = new Random();
                StringBuilder randomPassword = new StringBuilder();
                for(int i = 0; i < Constants.PASSWORD_DEFAULT_LEN; i++) {
                    char c = Constants.PASSWORD_GENERATOR_ALLOWED_CHARACTERS.charAt(rnd.nextInt(Constants.PASSWORD_GENERATOR_ALLOWED_CHARACTERS.length()));
                    if(i % 2 == 0 && Character.isLetter(c)) {
                        c = Character.toUpperCase(c);
                    }
                    randomPassword.append(c);
                }
                binding.viewInputPassword.passwordTextInput.setText(randomPassword.toString());
            }
        };
    }

    // Navigate to generate key pair fragment
    private View.OnClickListener generateKeyPairAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(requireView()).navigate(PasswordFragmentDirections.actionPasswordFragmentToNewKeyPairFragment());
            }
        };
    }

    private View.OnClickListener copyPublicKeyAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(binding.viewInputPublicKey.personalPublicKeyText.getText() != null && !binding.viewInputPublicKey.personalPublicKeyText.getText().toString().isEmpty()) {
                    Utils.copyToClipboard(requireContext(), getString(R.string.public_key_input_view_personal_public_key_input_hint), binding.viewInputPublicKey.personalPublicKeyText.getText().toString());
                    Toast.makeText(requireContext(), R.string.public_key_copied, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private View.OnClickListener importPrivateKeyAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityImportPrivateKeyLauncher.launch(new FilePicker.Builder(requireContext()).addPickDocumentFile(null).build());
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
}