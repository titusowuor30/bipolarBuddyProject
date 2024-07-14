package com.bengohub.vitalstracker.ui.About;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AboutViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AboutViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Welcome to the BipolarBuddy App.\n" +
                "\n" +
                "At BipolarBuddy, we understand the unique challenges faced by individuals living with bipolar disorder. Our commitment to providing comprehensive and compassionate care has driven us to develop this innovative mobile application. Designed with you in mind, our app is a tool to help you track your mood, manage your medication, and access valuable resources and support.");
    }

    public LiveData<String> getText() {
        return mText;
    }
}