package com.danielhlewis.wheelcontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by dlewis on 3/12/14.
 */
public class WheelControl extends View {

    public WheelControl(Context context) {
        super(context);
    }

    public WheelControl(Context context, int numberOfSlices) {
        this(context, new String[numberOfSlices]);
    }

    public WheelControl(Context context, String labels[]) {
        super(context);
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] != null) {
                addSlice(labels[i]);
            } else {
                addSlice("");
            }
        }
        //Porter-Duff seems glitchy in hardware (at least on the HTC One)
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        this.setOnTouchListener(wheelTouch);
    }

    ArrayList<WheelSlice> slices = new ArrayList<WheelSlice>();
    double initialRotation = -90;

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    int defaultUnselectedSliceColor = Color.parseColor("#009999");
    int defaultSelectedSliceColor = Color.parseColor("#FF7400");
    int defaultPositiveColor = Color.parseColor("#70E500");
    int defaultNegativeColor = Color.parseColor("#E1004C");
    int defaultCenterTextColor = Color.parseColor("#000000");

    String centerText = "";

    //Size Params
    int padding = 20;
    int labelPadding;
    int origin;
    int diameter, radius;
    float innerDiameterRelativeSize = (float) .5;
    int innerDiameter, innerRadius;
    RectF wheelBox, innerWheelBox;
    int labelPaddingRatio = 8;
    int textSizeRatio = 12;
    int textHeight;
    int centerTextWidth;
    Rect textBounds = new Rect();

    //Touch interaction
    boolean centerClickInitiated = false;
    int sliceTouching = -1;
    long touchDownTime;
    long clickThreshold = (long) (1000000000 * .25); //Using a quarter-second click time

    public enum SliceState {
        UNSELECTED,
        SELECTED,
        POSITIVE,
        NEGATIVE
    };

    public class WheelSlice {
        int overrideUnselectedColor = -1, overrideSelectedColor = -1,
                overridePositiveColor = -1, overrideNegativeColor = -1;
        String label = "";
        int labelWidth = 0;
        double arcLength, angleStart, angleCenter, angleEnd;
        int labelX, labelY;
        SliceState state = SliceState.UNSELECTED;


        public void clearColorOverrides() {
            overrideUnselectedColor = -1;
            overrideSelectedColor = -1;
            overridePositiveColor = -1;
            overrideNegativeColor = -1;
        }
    }

    //Set up listeners
    ArrayList<WheelSliceTouchListener> sliceTouchListeners = new ArrayList<WheelSliceTouchListener> ();
    public void setOnSliceTouchListener(WheelSliceTouchListener listener) {
        this.sliceTouchListeners.add(listener);
    }

    ArrayList<WheelSliceClickListener> sliceClickListeners = new ArrayList<WheelSliceClickListener>();
    public void setOnSliceClickListener(WheelSliceClickListener listener) {
        this.sliceClickListeners.add(listener);
    }

    ArrayList<WheelCenterClickListener> centerClickListeners = new ArrayList<WheelCenterClickListener> ();
    public void setOnCenterClickListener(WheelCenterClickListener listener) {
        this.centerClickListeners.add(listener);
    }

    public void setSliceState(int sliceNumber, SliceState s) {
        if (sliceNumber >= 0 && sliceNumber < slices.size()) {
            slices.get(sliceNumber).state = s;
        }
        invalidate();
    }

    public void setCenterText(String centerText) {
        this.centerText = centerText;
        recalculateCenterTextWidth();
        invalidate();
    }

    private View.OnTouchListener wheelTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                touchDownTime = System.nanoTime();
            int touchX = (int)motionEvent.getX();
            int touchY = (int)motionEvent.getY();
            int sliceTouched = -1;
            //Make sure it's inside the outer circle...
            boolean inOuterCircle = Math.pow((origin - touchX),2) + Math.pow((origin - touchY),2) <= Math.pow(radius,2);
            boolean inInnerCircle = Math.pow((origin - touchX),2) + Math.pow((origin - touchY),2) <= Math.pow(innerRadius,2);
            if (inOuterCircle && !inInnerCircle) {
                //We are touching a slice
                double a = Math.toDegrees(Math.atan2(origin - touchX, origin - touchY));
                int finalAngle = (int) ((-a + 360 + (slices.get(0).arcLength / 2)) % 360);
                sliceTouched = (int) (finalAngle / slices.get(0).arcLength);
                if (sliceTouching != sliceTouched) {
                    sliceTouching = -1;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    //We are initating a slice touch
                    sliceTouching = sliceTouched;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (sliceTouching == sliceTouched) {
                        if (System.nanoTime() - touchDownTime <= clickThreshold) {
                            //Slice click completed, notify listeners
                            for (WheelSliceClickListener listener : sliceClickListeners) {
                                listener.onSliceClick(sliceTouching);
                            }
                        }
                    }
                }
            } else {
                //We are not touching a slice
            }
            //Fire the slice touched callback
            for (WheelSliceTouchListener listener : sliceTouchListeners)
                listener.onSliceTouch(sliceTouched);

            if (!inInnerCircle) {
                //If we've left the center, cancel any pending center clicks
                centerClickInitiated = false;
            } else {
                // Center click logic - A center click will be reported if a down and up are
                //   recorded with no leaving the center inbetween
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    //Center click initiated
                    centerClickInitiated = true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP && centerClickInitiated) {
                    if (System.nanoTime() - touchDownTime <= clickThreshold) {
                        //Fire center clicked callback
                        for (WheelCenterClickListener listener : centerClickListeners)
                            listener.onCenterClick();
                    }
                }
            }
            return true;
        }
    };

    public void addSlice() {
        addSlice("");
    }

    public void addSlice(String label) {
        addSlice(label, -1, -1);
    }

    public void addSlice(String label, int overrideUnselectedColor, int overrideSelectedColor) {
        addSlice(slices.size(), label, overrideUnselectedColor, overrideSelectedColor);
    }

    public void addSlice(int location, String label, int overrideUnselectedColor, int overrideSelectedColor) {
        WheelSlice s = new WheelSlice();
        s.label = label;
        s.overrideSelectedColor = overrideSelectedColor;
        s.overrideUnselectedColor = overrideUnselectedColor;
        slices.add(s);
        recalculateSliceArcLengths();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        //Recalculate all of our components' sizes
        diameter = (w < h) ? (w) : (h);
        diameter -= padding * 2;
        radius = diameter / 2;
        origin = padding + radius;
        innerDiameter = (int) (diameter * innerDiameterRelativeSize);
        innerRadius = innerDiameter / 2;
        wheelBox = new RectF(padding, padding, diameter + padding, diameter + padding);
        innerWheelBox = new RectF(padding + (radius - innerRadius),
                padding + (radius - innerRadius),
                padding + (radius + innerRadius),
                padding + (radius + innerRadius));
        labelPadding = diameter / labelPaddingRatio;
        //Set our text size relative to the size of our circle
        // (Maybe not the best way to do it, but it works)
        paint.setTextSize(diameter / textSizeRatio);
        recalculateAllLabelWidths();
        paint.getTextBounds("a", 0, 1, textBounds);
        int textHeight = textBounds.height();
        //Set Text Labels
        for (WheelSlice slice : slices) {
            slice.labelX = (int) (wheelBox.centerX() + (Math.sin(Math.toRadians(-(slice.angleCenter + initialRotation))) * (radius - labelPadding)) - (slice.labelWidth / 2));
            slice.labelY = (int) (wheelBox.centerY() + (Math.cos(Math.toRadians(slice.angleCenter + initialRotation)) * (radius - labelPadding)) + (textHeight / 2));
        }
        recalculateCenterTextWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Bail if we don't have any slices to draw
        if (slices.size() == 0) {
            return;
        }

        drawSlices(canvas);
        drawLabels(canvas);
        drawCenterText(canvas);
    }

    private void drawSlices(Canvas canvas) {
        double currentAngle = initialRotation - (slices.get(0).arcLength / 2);
        int sliceCount = slices.size();
        for (int i = 0; i < sliceCount; i++) {
            WheelSlice slice = slices.get(i);
            int sliceColor;
            switch (slice.state) {
                default:
                case UNSELECTED:
                    sliceColor = (slice.overrideUnselectedColor != -1) ? (slice.overrideUnselectedColor) : (defaultUnselectedSliceColor);
                    break;
                case SELECTED:
                    sliceColor = (slice.overrideSelectedColor != -1) ? (slice.overrideSelectedColor) : (defaultSelectedSliceColor);
                    break;
                case POSITIVE:
                    sliceColor = (slice.overridePositiveColor != -1) ? (slice.overridePositiveColor) : (defaultPositiveColor);
                    break;
                case NEGATIVE:
                    sliceColor = (slice.overrideNegativeColor != -1) ? (slice.overrideNegativeColor) : (defaultNegativeColor);
                    break;
            }
            paint.setColor(sliceColor);
            canvas.drawArc(wheelBox, (float) currentAngle + 1, (float) slice.arcLength - 1, true, paint);
            currentAngle += slice.arcLength;
        }
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawOval(innerWheelBox, paint);
        paint.setXfermode(null);
    }

    private void drawLabels(Canvas canvas) {
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        int sliceCount = slices.size();
        for (int i = 0; i < sliceCount; i++) {
            WheelSlice slice = slices.get(i);
            canvas.drawText(slice.label, slice.labelX, slice.labelY, paint);
        }
        paint.setXfermode(null);
    }

    private void drawCenterText(Canvas canvas) {
        paint.setXfermode(null);
        paint.setColor(defaultCenterTextColor);
        canvas.drawText(centerText, origin - (centerTextWidth / 2), origin + (textHeight / 2), paint);
    }

    public void setSliceLabel(int sliceNumber, String label) {
        slices.get(sliceNumber).label = label;
        recalculateLabelWidth(sliceNumber);
        invalidate();
    }

    public void setSliceLabels(String labels[]) {
        int numSlices = slices.size();
        int numLabels = labels.length;
        for (int i = 0; i < numSlices && i < numLabels; i++) {
            setSliceLabel(i, labels[i]);
        }
    }

    private void recalculateLabelWidth(int sliceNumber) {
        paint.getTextBounds(slices.get(sliceNumber).label, 0, slices.get(sliceNumber).label.length(), textBounds);
        slices.get(sliceNumber).labelWidth = textBounds.width();
    }

    private void recalculateAllLabelWidths() {
        int sliceCount  = slices.size();
        for (int i = 0; i < sliceCount; i++) {
            recalculateLabelWidth(i);
        }
    }

    private void recalculateSliceArcLengths() {
        int sliceCount  = slices.size();
        double arcLength = 360.0 / sliceCount;
        for (int i = 0; i < sliceCount; i++) {
            WheelSlice slice = slices.get(i);
            slice.arcLength = arcLength;
            slice.angleStart = initialRotation - (arcLength / 2) + (i * arcLength);
            slice.angleEnd = slice.angleStart + arcLength;
            slice.angleCenter = slice.angleStart + ((slice.angleEnd - slice.angleStart) / 2);
        }
    }

    private void recalculateCenterTextWidth() {
        paint.getTextBounds(centerText, 0, centerText.length(), textBounds);
        centerTextWidth = textBounds.width();
    }
}
