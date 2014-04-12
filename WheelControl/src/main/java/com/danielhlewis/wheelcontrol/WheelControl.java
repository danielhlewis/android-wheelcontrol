package com.danielhlewis.wheelcontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

import static android.graphics.Bitmap.createBitmap;

/**
 * Created by dlewis on 3/12/14.
 */
public class WheelControl extends View {

    public WheelControl(Context context) {
        this(context, 0);
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

        bitmap = createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);

        this.setOnTouchListener(wheelTouch);
    }

    ArrayList<WheelSlice> slices = new ArrayList<WheelSlice>();
    double initialRotation = -90;

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    int defaultUnselectedSliceColor = Color.parseColor("#009999");
    int defaultSelectedSliceColor = Color.parseColor("#FF7400");
    int defaultPositiveColor = Color.parseColor("#70E500");
    int defaultNegativeColor = Color.parseColor("#E1004C");

    String centerText = "";
    int centerTextColor = Color.parseColor("#000000");

    boolean drawCenterCircle = false;
    int centerColor = Color.parseColor("#E1004C");

    Canvas bitmapCanvas;
    Bitmap bitmap;

    //Size Params
    int padding = 20;
    int centerButtonPadding = 5;
    int labelPadding;
    int origin;
    int diameter, radius;
    float innerDiameterRelativeSize = (float) .5;
    int innerDiameter, innerRadius;
    RectF wheelBox, innerWheelBox, innermostWheelBox;
    int labelPaddingRatio = 8;
    int textSizeRatio = 12;
    int textHeight;
    int centerTextWidth;
    Rect textBounds = new Rect();
    boolean showLabels = true, showCenterText = true;

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
    }

    public void clearSliceColorOverrides(int sliceNum) {
        setSliceColorOverrides(sliceNum, -1, -1, -1, -1);
    }

    public void setSliceColorOverrides(int sliceNum, int overrideUnselectedColor, int overrideSelectedColor,
                                       int overridePositiveColor, int overrideNegativeColor) {
        if (sliceNum >= 0 && sliceNum < slices.size()) {
            WheelSlice slice = slices.get(sliceNum);
            slice.overrideUnselectedColor = overrideUnselectedColor;
            slice.overrideSelectedColor = overrideSelectedColor;
            slice.overridePositiveColor = overridePositiveColor;
            slice.overrideNegativeColor = overrideNegativeColor;
        }
        invalidate();
    }

    public int[] getColorOverrides(int sliceNum) {
        int overrides[] = null;
        if (sliceNum >= 0 && sliceNum < slices.size()) {
            overrides = new int[4];
            WheelSlice slice = slices.get(sliceNum);
            overrides[0] = slice.overrideUnselectedColor;
            overrides[1] = slice.overrideSelectedColor;
            overrides[2] = slice.overridePositiveColor;
            overrides[3] = slice.overrideNegativeColor;
        }
        return overrides;
    }

    public void setNumberOfSlices(int n) {
        int sliceCount = slices.size();
        if (n > sliceCount) {
            //Add slices
            for (int i = 0; i < n - sliceCount; i++) {
                addSlice();
            }
        } else if (n < sliceCount) {
            for (int i = 0; i < sliceCount - n; i++) {
                removeSlice(slices.size() - 1);
            }
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

    private View.OnTouchListener wheelTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN)
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
                if (action == MotionEvent.ACTION_DOWN) {
                    //We are initating a slice touch
                    sliceTouching = sliceTouched;
                } else if (action == MotionEvent.ACTION_UP) {
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
                listener.onSliceTouch(sliceTouched, action);

            if (!inInnerCircle) {
                //If we've left the center, cancel any pending center clicks
                centerClickInitiated = false;
            } else {
                // Center click logic - A center click will be reported if a down and up are
                //   recorded with no leaving the center inbetween
                if (action == MotionEvent.ACTION_DOWN) {
                    //Center click initiated
                    centerClickInitiated = true;
                } else if (action == MotionEvent.ACTION_UP && centerClickInitiated) {
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

    public void setCenterTextColor(int centerTextColor) {
        this.centerTextColor = centerTextColor;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
        invalidate();
    }

    public void setShowCenterText(boolean showCenterText) {
        this.showCenterText = showCenterText;
    }

    public void setSliceState(int sliceNumber, SliceState s) {
        if (sliceNumber >= 0 && sliceNumber < slices.size()) {
            slices.get(sliceNumber).state = s;
        }
        invalidate();
    }

    public int getCenterTextColor() {
        return centerTextColor;
    }

    public boolean isDrawCenterCircle() {
        return drawCenterCircle;
    }

    public void setDrawCenterCircle(boolean drawCenterCircle) {
        this.drawCenterCircle = drawCenterCircle;
    }

    public int getCenterColor() {
        return centerColor;
    }

    public void setCenterColor(int centerColor) {
        this.centerColor = centerColor;
    }

    public SliceState getSliceState(int sliceNumber) {
        if (sliceNumber >= 0 && sliceNumber < slices.size()) {
            return slices.get(sliceNumber).state;
        } else {
            return null;
        }
    }

    public void setCenterText(String centerText) {
        this.centerText = centerText;
        recalculateCenterTextWidth();
        invalidate();
    }

    public int getNumberOfSlices() {
        return slices.size();
    }

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

    public void removeSlice(int location) {
        slices.remove(slices.size() - 1);
        recalculateSliceArcLengths();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        //Recalculate all of our components' sizes
        diameter = (w < h) ? (w) : (h);
        bitmap = createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
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
        innermostWheelBox = new RectF(innerWheelBox.left + centerButtonPadding,
                innerWheelBox.top + centerButtonPadding,
                innerWheelBox.right - centerButtonPadding,
                innerWheelBox.bottom - centerButtonPadding);
        labelPadding = diameter / labelPaddingRatio;
        //Set our text size relative to the size of our circle
        // (Maybe not the best way to do it, but it works)
        paint.setTextSize(diameter / textSizeRatio);
        recalculateAllLabelWidths();
        paint.getTextBounds("A", 0, 1, textBounds);
        textHeight = textBounds.height();
        //Set Text Labels
        recalculateCenterTextWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Bail if we don't have any slices to draw
        if (slices.size() == 0) {
            return;
        }


        drawSlices(bitmapCanvas);
        if (showLabels) drawLabels(bitmapCanvas);
        if (showCenterText) drawCenterText(bitmapCanvas);
        canvas.drawBitmap(bitmap, 0, 0, paint);
    }

    private void drawSlices(Canvas canvas) {
        double currentAngle = initialRotation - (slices.get(0).arcLength / 2);
        int sliceCount = slices.size();
        int paddingDegrees = (sliceCount == 1) ? (0) : (1);
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

            canvas.drawArc(wheelBox, (float) currentAngle + paddingDegrees, (float) slice.arcLength - paddingDegrees, true, paint);

            currentAngle += slice.arcLength;
        }
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawOval(innerWheelBox, paint);
        if (drawCenterCircle) {
            paint.setColor(centerColor);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            canvas.drawOval(innermostWheelBox, paint);
        }
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
        paint.setColor(centerTextColor);
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
        WheelSlice slice = slices.get(sliceNumber);
        paint.getTextBounds(slice.label, 0, slice.label.length(), textBounds);
        slice.labelWidth = textBounds.width();
        if (wheelBox != null) {
            slice.labelX = (int) (wheelBox.centerX() + (Math.sin(Math.toRadians(-(slice.angleCenter + initialRotation))) * (radius - labelPadding)) - (slice.labelWidth / 2));
            slice.labelY = (int) (wheelBox.centerY() + (Math.cos(Math.toRadians(slice.angleCenter + initialRotation)) * (radius - labelPadding)) + (textHeight / 2));
        }
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

    public int selectRandomSlice() {
        Random random = new Random();
        int sliceCount = slices.size();
        for (int i = 0; i < sliceCount; i++) {
            setSliceState(i, SliceState.UNSELECTED);
        }
        int selectedSlice = random.nextInt(getNumberOfSlices());
        setSliceState(selectedSlice, SliceState.SELECTED);
        return selectedSlice;
    }
}
