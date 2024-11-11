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

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.alexzava.krypto.databinding.FragmentPickFileBinding;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;
import com.nareshchocha.filepickerlibrary.ui.FilePicker;

public class PickFileFragment extends Fragment {
    private final String LOG_TAG = this.getClass().getSimpleName();
    FragmentPickFileBinding binding;

    Animation rotateOpenAnimation;
    Animation rotateCloseAnimation;
    Animation fromBottomAnimation;
    Animation toBottomAnimation;

    boolean isPickFileActionButtonClicked = false;
    String selectedAction; // Encrypt or Decrypt

    Uri sharedFileUri;
    String scannedPublicKey;

    public PickFileFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentPickFileBinding.inflate(getLayoutInflater());
        sharedFileUri = null;
        scannedPublicKey = "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_pick_file, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide action buttons
        binding.scanQRCodeActionButton.setVisibility(View.INVISIBLE);
        binding.scanQRCodeActionButtonIcon.setVisibility(View.INVISIBLE);
        binding.generateKeypairActionButton.setVisibility(View.INVISIBLE);
        binding.generateKeypairActionButtonIcon.setVisibility(View.INVISIBLE);
        binding.encryptActionButton.setVisibility(View.INVISIBLE);
        binding.encryptActionButtonIcon.setVisibility(View.INVISIBLE);
        binding.decryptActionButton.setVisibility(View.INVISIBLE);
        binding.decryptActionButtonIcon.setVisibility(View.INVISIBLE);
        isPickFileActionButtonClicked = false;

        // Set listeners
        binding.pickFileActionButton.setOnClickListener(toggleActionButton());
        binding.encryptActionButton.setOnClickListener(navigateToNextFragmentAction(Constants.ACTION_ENCRYPT));
        binding.encryptActionButtonIcon.setOnClickListener(navigateToNextFragmentAction(Constants.ACTION_ENCRYPT));
        binding.decryptActionButton.setOnClickListener(navigateToNextFragmentAction(Constants.ACTION_DECRYPT));
        binding.decryptActionButtonIcon.setOnClickListener(navigateToNextFragmentAction(Constants.ACTION_DECRYPT));
        binding.generateKeypairActionButton.setOnClickListener(newKeyPairButtonAction());
        binding.generateKeypairActionButtonIcon.setOnClickListener(newKeyPairButtonAction());
        binding.scanQRCodeActionButton.setOnClickListener(scanQRCodeButtonAction());
        binding.scanQRCodeActionButtonIcon.setOnClickListener(scanQRCodeButtonAction());

        // App toolbar
        binding.viewAppToolBar.topAppBar.setNavigationIcon(null);
        binding.viewAppToolBar.topAppBar.setOnMenuItemClickListener(menuItemClickListener());

        // Set animations
        rotateOpenAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_open_anim);
        rotateCloseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_close_anim);
        fromBottomAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.from_bottom_anim);
        toBottomAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.to_bottom_anim);

        // Handle app link or shared file
        Intent intent = requireActivity().getIntent();
        String intentAction = intent.getAction();
        String intentType = intent.getType();

        if(intentAction!= null && intentAction.equals(Intent.ACTION_SEND) && intentType != null) {
            sharedFileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedFileUri != null) {
                // Show encrypt/decrypt button
                binding.pickFileActionButton.callOnClick();
                binding.helperTextView.setText(getString(R.string.pick_file_fragment_text_helper_document_loaded));
                Toast.makeText(requireContext(), "Document loaded", Toast.LENGTH_SHORT).show();
            }
        }

        if(intent.getData() != null) {
            binding.helperTextView.setText(getString(R.string.pick_file_fragment_text_helper_public_key_loaded));
            Toast.makeText(requireContext(), "Public Key loaded", Toast.LENGTH_SHORT).show();
        }
    }

    // File picker activity result
    private final ActivityResultLauncher<Intent> activityFilePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        Navigation.findNavController(requireView()).navigate(PickFileFragmentDirections.actionPickFileFragmentToPasswordFragment(uri.toString(), selectedAction, scannedPublicKey));
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
                                scannedPublicKey = uri.getQueryParameter(Constants.QR_CODE_PUBLIC_KEY_PARAM);
                            } else if (uri.getHost().equals(Constants.APP_HOST)) {
                                scannedPublicKey = uri.getLastPathSegment();
                            }
                            binding.helperTextView.setText(getString(R.string.pick_file_fragment_text_helper_public_key_loaded));
                            Toast.makeText(requireContext(), "Public key loaded", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private View.OnClickListener navigateToNextFragmentAction(String action) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedAction = action;
                if(sharedFileUri == null) {
                    activityFilePickerLauncher.launch(new FilePicker.Builder(requireContext()).addPickDocumentFile(null).build());
                } else {
                    Navigation.findNavController(requireView()).navigate(PickFileFragmentDirections.actionPickFileFragmentToPasswordFragment(sharedFileUri.toString(), action, scannedPublicKey));
                }
            }
        };
    }

    private View.OnClickListener newKeyPairButtonAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(requireView()).navigate(PickFileFragmentDirections.actionPickFileFragmentToNewKeyPairFragment());
            }
        };
    }

    // Scan public key QR code
    private View.OnClickListener scanQRCodeButtonAction() {
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

    private View.OnClickListener toggleActionButton() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPickFileActionButtonClicked) {
                    binding.scanQRCodeActionButton.setVisibility(View.INVISIBLE);
                    binding.scanQRCodeActionButtonIcon.setVisibility(View.INVISIBLE);
                    binding.generateKeypairActionButton.setVisibility(View.INVISIBLE);
                    binding.generateKeypairActionButtonIcon.setVisibility(View.INVISIBLE);
                    binding.encryptActionButton.setVisibility(View.INVISIBLE);
                    binding.encryptActionButtonIcon.setVisibility(View.INVISIBLE);
                    binding.decryptActionButton.setVisibility(View.INVISIBLE);
                    binding.decryptActionButtonIcon.setVisibility(View.INVISIBLE);

                    binding.scanQRCodeActionButton.startAnimation(toBottomAnimation);
                    binding.scanQRCodeActionButtonIcon.startAnimation(toBottomAnimation);
                    binding.generateKeypairActionButton.startAnimation(toBottomAnimation);
                    binding.generateKeypairActionButtonIcon.startAnimation(toBottomAnimation);
                    binding.encryptActionButton.startAnimation(toBottomAnimation);
                    binding.encryptActionButtonIcon.startAnimation(toBottomAnimation);
                    binding.decryptActionButton.startAnimation(toBottomAnimation);
                    binding.decryptActionButtonIcon.startAnimation(toBottomAnimation);
                    binding.pickFileActionButton.startAnimation(rotateCloseAnimation);

                    isPickFileActionButtonClicked = false;
                } else {
                    binding.scanQRCodeActionButton.setVisibility(View.VISIBLE);
                    binding.scanQRCodeActionButtonIcon.setVisibility(View.VISIBLE);
                    binding.generateKeypairActionButton.setVisibility(View.VISIBLE);
                    binding.generateKeypairActionButtonIcon.setVisibility(View.VISIBLE);
                    binding.encryptActionButton.setVisibility(View.VISIBLE);
                    binding.encryptActionButtonIcon.setVisibility(View.VISIBLE);
                    binding.decryptActionButton.setVisibility(View.VISIBLE);
                    binding.decryptActionButtonIcon.setVisibility(View.VISIBLE);

                    binding.scanQRCodeActionButton.startAnimation(fromBottomAnimation);
                    binding.scanQRCodeActionButtonIcon.startAnimation(fromBottomAnimation);
                    binding.generateKeypairActionButton.startAnimation(fromBottomAnimation);
                    binding.generateKeypairActionButtonIcon.startAnimation(fromBottomAnimation);
                    binding.encryptActionButton.startAnimation(fromBottomAnimation);
                    binding.encryptActionButtonIcon.startAnimation(fromBottomAnimation);
                    binding.decryptActionButton.startAnimation(fromBottomAnimation);
                    binding.decryptActionButtonIcon.startAnimation(fromBottomAnimation);
                    binding.pickFileActionButton.startAnimation(rotateOpenAnimation);

                    isPickFileActionButtonClicked = true;
                }
            }
        };
    }
}