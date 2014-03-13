package com.danielhlewis.wheelcontrol.samples;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.danielhlewis.wheelcontrol.WheelCenterClickListener;
import com.danielhlewis.wheelcontrol.WheelControl;
import com.danielhlewis.wheelcontrol.WheelSliceClickListener;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        int selectedSlice = -1;
        WheelControl wheel;
        int centerClicks = 0;
        int spHandles[] = new int[12];
        SoundPool sp;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.relativeLayout);
            //values = calculateData(values);
            //MyGraphView graphView = new MyGraphView(this.getActivity(), values);
            //lv1.addView(graphView);
            String labels[] = {"C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B"};
            //String labels[] = {"12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"};
            wheel = new WheelControl(this.getActivity(), labels);
            //wheel = new WheelControl(this.getActivity(), 4);
            wheel.setOnSliceClickListener(wheelSliceClickListener);
            wheel.setOnCenterClickListener(wheelCenterClickListener);
            layout.addView(wheel);

            sp = new SoundPool(12, AudioManager.STREAM_MUSIC, 0);
            spHandles[0] = sp.load(this.getActivity(), R.raw._48, 1);
            spHandles[1] = sp.load(this.getActivity(), R.raw._49, 1);
            spHandles[2] = sp.load(this.getActivity(), R.raw._50, 1);
            spHandles[3] = sp.load(this.getActivity(), R.raw._51, 1);
            spHandles[4] = sp.load(this.getActivity(), R.raw._52, 1);
            spHandles[5] = sp.load(this.getActivity(), R.raw._53, 1);
            spHandles[6] = sp.load(this.getActivity(), R.raw._54, 1);
            spHandles[7] = sp.load(this.getActivity(), R.raw._55, 1);
            spHandles[8] = sp.load(this.getActivity(), R.raw._56, 1);
            spHandles[9] = sp.load(this.getActivity(), R.raw._57, 1);
            spHandles[10] = sp.load(this.getActivity(), R.raw._58, 1);
            spHandles[11] = sp.load(this.getActivity(), R.raw._59, 1);

            return rootView;
        }

        private WheelSliceClickListener wheelSliceClickListener = new WheelSliceClickListener() {
            @Override
            public void onSliceClick(int sliceNumber) {
                if (sliceNumber != selectedSlice) {
                    if (sliceNumber != -1) {
                        wheel.setSliceState(sliceNumber, WheelControl.SliceState.SELECTED);
                        sp.play(spHandles[sliceNumber], 1, 1, 1, 0, 1);
                    }
                    if (selectedSlice != -1) {
                        wheel.setSliceState(selectedSlice, WheelControl.SliceState.UNSELECTED);
                    }
                    selectedSlice = sliceNumber;
                }
            }
        };

        private WheelCenterClickListener wheelCenterClickListener = new WheelCenterClickListener() {
            @Override
            public void onCenterClick() {
                centerClicks++;
                wheel.setCenterText(String.valueOf(centerClicks));
            }
        };
    }

}
