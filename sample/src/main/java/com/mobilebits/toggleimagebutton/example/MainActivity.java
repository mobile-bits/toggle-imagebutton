package com.mobilebits.toggleimagebutton.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.mobilebits.toggleimagebutton.ToggleImageButton;

public class MainActivity extends AppCompatActivity {

    private ToggleImageButton mHorizontalToggle;
    private LinearLayout mHorizontalLayout;

    private ToggleImageButton mVerticalToggle;
    private LinearLayout mVerticalLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHorizontalToggle = (ToggleImageButton)findViewById(R.id.horizontal_toggle);
        mHorizontalLayout = (LinearLayout)findViewById(R.id.horizontal_layout);

        mVerticalToggle = (ToggleImageButton)findViewById(R.id.vertical_toggle);
        mVerticalLayout = (LinearLayout)findViewById(R.id.vertical_layout);

        mVerticalToggle.setAnimDirection(ToggleImageButton.ANIM_DIRECTION_HORIZONTAL);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mHorizontalToggle.setParentSize(mHorizontalLayout.getHeight());
        mVerticalToggle.setParentSize(mVerticalLayout.getWidth());
    }
}
