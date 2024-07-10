package com.bengohub.vitalstracker.ui.Vitals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bengohub.vitalstracker.databinding.FragmentVitalsBinding;

public class VitalsFragment extends Fragment {

    private FragmentVitalsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        VitalsViewModel vitalsViewModel =
                new ViewModelProvider(this).get(VitalsViewModel.class);

        binding = FragmentVitalsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textVitals;
        vitalsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}