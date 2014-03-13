package com.danielhlewis.wheelcontrol.samples;

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

        setTitle("Wheel Control Samples");

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
            //String labels[] = {"C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B"};
            //String labels[] = {"12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"};
            //wheel = new WheelControl(this.getActivity(), labels);
            wheel = new WheelControl(this.getActivity(), 4);
            wheel.setOnSliceClickListener(wheelSliceClickListener);
            wheel.setOnCenterClickListener(wheelCenterClickListener);
            layout.addView(wheel);
            return rootView;
        }

        private WheelSliceClickListener wheelSliceClickListener = new WheelSliceClickListener() {
            @Override
            public void onSliceClick(int sliceNumber) {
                if (sliceNumber != selectedSlice) {
                    if (sliceNumber != -1) {
                        wheel.setSliceState(sliceNumber, WheelControl.SliceState.SELECTED);
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
