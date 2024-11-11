package com.alexzava.krypto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.alexzava.krypto.databinding.FragmentCompleteActionBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.goterl.lazysodium.LazySodium;
import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.SodiumAndroid;

public class CompleteActionFragment extends Fragment {
    private final String LOG_TAG = this.getClass().getSimpleName();
    FragmentCompleteActionBinding binding;
    LazySodium sodium;

    // Args
    String uriStr;
    String action;
    String keyHex;
    String keySaltHex;
    String keyMode;
    String personalPublicKey;

    BottomSheetBehavior bottomSheetBehavior;

    public CompleteActionFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentCompleteActionBinding.inflate(getLayoutInflater());
        sodium = new LazySodiumAndroid(new SodiumAndroid());

        uriStr = CompleteActionFragmentArgs.fromBundle(getArguments()).getUriStr();
        action = CompleteActionFragmentArgs.fromBundle(getArguments()).getAction();
        keyHex = CompleteActionFragmentArgs.fromBundle(getArguments()).getKeyHex();
        keySaltHex = CompleteActionFragmentArgs.fromBundle(getArguments()).getKeySaltHex();
        keyMode = CompleteActionFragmentArgs.fromBundle(getArguments()).getKeyMode();
        personalPublicKey = CompleteActionFragmentArgs.fromBundle(getArguments()).getPublicKey();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_complete_action, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Progress broadcast
        LocalBroadcastManager.getInstance(requireContext().getApplicationContext())
                .registerReceiver(broadcastReceiver, new IntentFilter(Constants.PROGRESS_BROADCAST_NAME));

        if(action.equals(Constants.ACTION_ENCRYPT)) {
            binding.actionHintText.setText(getString(R.string.complete_fragment_encrypting));
        } else {
            binding.actionHintText.setText(getString(R.string.complete_fragment_decrypting));
        }

        // Set listeners
        binding.nextActionButton.setOnClickListener(navigateToMainActivityAction());
        binding.shareActionButton.setOnClickListener(shareButtonAction());

        // App toolbar
        binding.viewAppToolBar.topAppBar.setNavigationIcon(null);
        binding.viewAppToolBar.topAppBar.setOnMenuItemClickListener(menuItemClickListener());

        // Bottom Sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.viewShareBottomSheet.bottomSheet);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.bottomSheetBackground.setAlpha(0);
        binding.viewShareBottomSheet.copyLinkButton.setOnClickListener(copyShareLinkAction());
        binding.viewShareBottomSheet.linkSwitch.setOnCheckedChangeListener(switchOnCheckedChanged());
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback());

        // Start background service
        Intent backgroundService = new Intent(requireContext(), BackgroundService.class);
        backgroundService.putExtra(Constants.ARG_SERVICE_URI, uriStr);
        backgroundService.putExtra(Constants.ARG_SERVICE_ACTION, action);
        backgroundService.putExtra(Constants.ARG_SERVICE_KEY_HEX, keyHex);
        backgroundService.putExtra(Constants.ARG_SERVICE_SALT_HEX, keySaltHex);
        backgroundService.putExtra(Constants.ARG_SERVICE_KEY_MODE, keyMode);
        requireActivity().startService(backgroundService);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle != null && bundle.containsKey(Constants.BROADCAST_RECEIVER_PROGRESS)) {
                int progress = bundle.getInt(Constants.BROADCAST_RECEIVER_PROGRESS);

                if(bundle.containsKey(Constants.BROADCAST_RECEIVER_IS_DONE) &&
                        bundle.getBoolean(Constants.BROADCAST_RECEIVER_IS_DONE)) {
                    binding.nextActionButton.setVisibility(View.VISIBLE);
                    binding.progressText.setVisibility(View.INVISIBLE);
                    binding.progressIndicator.setVisibility(View.INVISIBLE);

                    if(bundle.containsKey(Constants.BROADCAST_RECEIVER_DECRYPTION_ERROR) && bundle.getBoolean(Constants.BROADCAST_RECEIVER_DECRYPTION_ERROR)) {
                        binding.actionHintText.setText(R.string.complete_fragment_wrong_password);
                    } else {
                        binding.actionHintText.setText(R.string.complete_fragment_process_done);
                        if(keyMode.equals(Constants.MODE_PUBLIC_KEY) && action.equals(Constants.ACTION_ENCRYPT)) {
                            binding.shareActionButton.setVisibility(View.VISIBLE);

                            generateShareQRCodeAction();
                        }
                    }
                }
                binding.progressIndicator.setProgress(progress);
                binding.progressText.setText(progress + "%");
            }
        }
    };

    private View.OnClickListener navigateToMainActivityAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivity = new Intent(requireContext(), MainActivity.class);
                startActivity(mainActivity);
            }
        };
    }

    private View.OnClickListener shareButtonAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
    private void generateShareQRCodeAction() {
        String shareLink = Utils.generateShareLink(binding.viewShareBottomSheet.linkSwitch.isChecked(), personalPublicKey);
        Bitmap qrCode = Utils.generateQRCodeBitmap(requireContext(), shareLink);
        if(qrCode != null) {
            binding.viewShareBottomSheet.shareQrCodeImage.setImageBitmap(qrCode);
        }
    }

    // Copy link
    private View.OnClickListener copyShareLinkAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shareLink = Utils.generateShareLink(binding.viewShareBottomSheet.linkSwitch.isChecked(), personalPublicKey);
                Utils.copyToClipboard(requireContext(), "Share link", shareLink);
                Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private CompoundButton.OnCheckedChangeListener switchOnCheckedChanged() {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                generateShareQRCodeAction();
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