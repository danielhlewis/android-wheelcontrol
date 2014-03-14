package com.danielhlewis.wheelcontrol.samples;

import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.danielhlewis.wheelcontrol.WheelCenterClickListener;
import com.danielhlewis.wheelcontrol.WheelControl;
import com.danielhlewis.wheelcontrol.WheelSliceClickListener;
import com.danielhlewis.wheelcontrol.WheelSliceTouchListener;

import java.util.Random;

public class MainActivity extends ActionBarActivity {

    static WheelControl wheel;
    static String labels[] = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
    static int lastSliceTouched = -1;
    static String sliceStates[];
    static int colorOverrides[];
    static int sliceCount = 6;
    static int stateCount = 2;
    static boolean requireClicks = true;
    static boolean defaultColors = true;
    static boolean showLabels = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //String labels[] = {"C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B"};
        wheel = new WheelControl(this, labels);
        wheel.setOnSliceClickListener(wheelSliceClickListener);
        wheel.setOnCenterClickListener(wheelCenterClickListener);
        wheel.setOnSliceTouchListener(wheelSliceTouchListener);

        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            showLabels = savedInstanceState.getBoolean("showLabels");
            sliceCount = savedInstanceState.getInt("sliceCount");
            stateCount = savedInstanceState.getInt("stateCount");
            sliceStates = savedInstanceState.getStringArray("sliceStates");
            colorOverrides = savedInstanceState.getIntArray("colorOverrides");
            requireClicks = savedInstanceState.getBoolean("requireClicks");
            defaultColors = savedInstanceState.getBoolean("defaultColors");
        } else {
            colorOverrides = new int[sliceCount * 4];
            for (int i = 0; i < sliceCount * 4; i++) {
                colorOverrides[i] = -1;
            }

            sliceStates = new String[sliceCount];
            for (int i = 0; i < sliceCount; i++) {
                sliceStates[i] = "UNSELECTED";
            }
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.container, new WheelFragment())
//                    .commit();
        }

        wheel.setShowLabels(showLabels);
        wheel.setCenterText((showLabels) ? ("Hide Labels") : ("Show Labels"));
        wheel.setNumberOfSlices(sliceCount);
        for (int i = 0; i < sliceCount; i++) {
            wheel.setSliceColorOverrides(i, colorOverrides[i * 4], colorOverrides[i * 4 + 1],
                    colorOverrides[i * 4 + 2], colorOverrides[i * 4 + 3]);
            wheel.setSliceState(i, WheelControl.SliceState.valueOf(sliceStates[i]));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("showLabels", showLabels);
        bundle.putInt("sliceCount", sliceCount);
        bundle.putInt("stateCount", stateCount);
        bundle.putBoolean("requireClicks", requireClicks);
        bundle.putBoolean("defaultColors", defaultColors);
        //Slice count may have changed, so we need to remake these arrays
        colorOverrides = new int[sliceCount * 4];
        sliceStates = new String[sliceCount];
        for (int i = 0; i < sliceCount; i++) {
            int sliceOverrides[] = wheel.getColorOverrides(i);
            for (int j = 0; j < 4; j++) {
                colorOverrides[i * 4 + j] = sliceOverrides[j];
            }
            sliceStates[i] = wheel.getSliceState(i).name();
        }
        bundle.putIntArray("colorOverrides", colorOverrides);
        bundle.putStringArray("sliceStates", sliceStates);
    }

    WheelControl.SliceState getNextState(WheelControl.SliceState s) {
        switch (s) {
            case UNSELECTED:
                if (stateCount > 1) {
                    return WheelControl.SliceState.SELECTED;
                } else {
                    return WheelControl.SliceState.UNSELECTED;
                }
            case SELECTED:
                if (stateCount > 2) {
                    return WheelControl.SliceState.POSITIVE;
                } else {
                    return WheelControl.SliceState.UNSELECTED;
                }
            case POSITIVE:
                if (stateCount > 3) {
                    return WheelControl.SliceState.NEGATIVE;
                } else {
                    return WheelControl.SliceState.UNSELECTED;
                }
            case NEGATIVE:
                return WheelControl.SliceState.UNSELECTED;
        }
        return WheelControl.SliceState.UNSELECTED;
    }

    private WheelSliceClickListener wheelSliceClickListener = new WheelSliceClickListener() {
        @Override
        public void onSliceClick(int sliceNumber) {
            if (requireClicks) {
                if (sliceNumber != -1) {
                    wheel.setSliceState(sliceNumber, getNextState(wheel.getSliceState(sliceNumber)));
                }
            }
        }
    };

    private WheelCenterClickListener wheelCenterClickListener = new WheelCenterClickListener() {
        @Override
        public void onCenterClick() {
            showLabels = !showLabels;
            wheel.setShowLabels(showLabels);
            wheel.setCenterText((showLabels) ? ("Hide Labels") : ("Show Labels"));
        }
    };

    private WheelSliceTouchListener wheelSliceTouchListener = new WheelSliceTouchListener() {
        @Override
        public void onSliceTouch(int sliceNumber, int actionType) {
            if (!requireClicks) {
                if (sliceNumber != lastSliceTouched) {
                    if (sliceNumber != -1) {
                        wheel.setSliceState(sliceNumber, getNextState(wheel.getSliceState(sliceNumber)));
                    }
                    lastSliceTouched = sliceNumber;
                }
                if (actionType == MotionEvent.ACTION_UP) {
                    lastSliceTouched = -1;
                }
            }
        }
    };

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class WheelFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_wheel, container, false);
            RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.wheelLayout);
            layout.addView(wheel);
            return rootView;
        }


    }

    public static class OptionsFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_options, container, false);

            //Set up slice count spinner
            Spinner spinnerSlice = (Spinner) rootView.findViewById(R.id.spinnerSliceCount);
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> adapterSlice = ArrayAdapter.createFromResource(this.getActivity(),
                    R.array.slice_count_options, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapterSlice.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinnerSlice.setAdapter(adapterSlice);
            spinnerSlice.setSelection(sliceCount - 1);
            spinnerSlice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int pos, long id) {
                    if (sliceCount != pos + 1) {
                        int oldSliceCount = sliceCount;
                        sliceCount = pos + 1;
                        wheel.setNumberOfSlices(sliceCount);
                        wheel.setSliceLabels(labels);
                        for (int s = 0; s < sliceCount; s++) {
                            wheel.setSliceState(s, WheelControl.SliceState.UNSELECTED);
                        }
                        if (!defaultColors) {
                            for (int s = oldSliceCount; s < sliceCount; s++) {
                                Random rnd = new Random();
                                int a = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                                int b = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                                int c = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                                int d = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                                wheel.setSliceColorOverrides(s, a, b, c, d);
                            }
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    //pass
                }
            });


            //Set up slice state spinner
            Spinner spinnerState = (Spinner) rootView.findViewById(R.id.spinnerStateCount);
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> adapterState = ArrayAdapter.createFromResource(this.getActivity(),
                    R.array.slice_state_options, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapterState.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinnerState.setAdapter(adapterState);
            spinnerState.setSelection(stateCount - 2);
            spinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (stateCount != i+2) {
                        stateCount = i + 2;
                        for (int s = 0; s < sliceCount; s++) {
                            wheel.setSliceState(s, WheelControl.SliceState.UNSELECTED);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            final Button btnColorScheme = (Button) rootView.findViewById(R.id.btnColorScheme);
            btnColorScheme.setText(defaultColors ? "Default" : "Random");
            btnColorScheme.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    defaultColors = !defaultColors;
                    btnColorScheme.setText(defaultColors ? "Default" : "Random");
                    if (defaultColors) {
                        for (int i = 0; i < sliceCount; i++) {
                            wheel.clearSliceColorOverrides(i);
                            wheel.setSliceState(i, WheelControl.SliceState.UNSELECTED);
                        }
                    } else {
                        for (int i = 0; i < sliceCount; i++) {
                            Random rnd = new Random();
                            int a = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                            int b = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                            int c = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                            int d = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                            wheel.setSliceColorOverrides(i, a, b, c, d);
                            wheel.setSliceState(i, WheelControl.SliceState.UNSELECTED);
                        }
                    }
                }
            });

            final Button btnInteraction = (Button) rootView.findViewById(R.id.btnInteraction);
            btnInteraction.setText(requireClicks ? "Click" : "Touch");
            btnInteraction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requireClicks = !requireClicks;
                    btnInteraction.setText(requireClicks ? "Click" : "Touch");
                }
            });

            return rootView;
        }
    }
}
