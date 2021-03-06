package com.snowble.android.widget.verticalstepper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.AbsSavedState;
import android.support.v7.widget.AppCompatButton;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VerticalStepper extends ViewGroup {
    private Context context;
    private Resources resources;
    private Step.Common commonStepValues;
    private AlwaysValidValidator alwaysValidValidator = new AlwaysValidValidator();
    @NonNull
    private StepValidator validator = alwaysValidValidator;

    @VisibleForTesting
    List<Step> steps;

    @VisibleForTesting
    int outerHorizontalPadding;
    @VisibleForTesting
    int outerVerticalPadding;
    @VisibleForTesting
    int iconInactiveColor;
    @VisibleForTesting
    int iconActiveColor;
    @VisibleForTesting
    int iconCompleteColor;
    @VisibleForTesting
    int continueButtonStyle;

    private SavedState savedState;

    public VerticalStepper(Context context) {
        super(context);
        init();
    }

    public VerticalStepper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public VerticalStepper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VerticalStepper(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init() {
        init(null);
    }

    private void init(@Nullable AttributeSet attrs) {
        init(attrs, 0);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr) {
        init(attrs, defStyleAttr, 0);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setWillNotDraw(false);

        context = getContext();
        resources = getResources();

        initPropertiesFromAttrs(attrs, defStyleAttr, defStyleRes);
        initPadding();

        commonStepValues = new Step.Common(context, iconActiveColor, iconInactiveColor, iconCompleteColor);
        steps = new ArrayList<>();
    }

    @VisibleForTesting
    void initPropertiesFromAttrs(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VerticalStepper,
                    defStyleAttr, defStyleRes);
        try {
            initIconPropertiesFromAttrs(a);
            initNavButtonPropertiesFromAttrs(a);
        } finally {
            a.recycle();
        }
    }

    private void initIconPropertiesFromAttrs(TypedArray a) {
        int defaultActiveColor =
                ThemeUtils.getResolvedAttributeData(context.getTheme(), R.attr.colorPrimary, R.color.bg_active_icon);
        iconActiveColor = a.getColor(R.styleable.VerticalStepper_iconColorActive,
                ResourcesCompat.getColor(resources, defaultActiveColor, context.getTheme()));
        iconInactiveColor = a.getColor(R.styleable.VerticalStepper_iconColorInactive,
                ResourcesCompat.getColor(resources, R.color.bg_inactive_icon, context.getTheme()));
        iconCompleteColor = a.getColor(R.styleable.VerticalStepper_iconColorComplete,
                iconActiveColor);
    }

    @SuppressLint("PrivateResource") // https://code.google.com/p/android/issues/detail?id=230985
    private void initNavButtonPropertiesFromAttrs(TypedArray a) {
        continueButtonStyle = a.getResourceId(
                R.styleable.VerticalStepper_continueButtonStyle, R.style.Widget_AppCompat_Button_Colored);
    }

    private void initPadding() {
        outerHorizontalPadding = resources.getDimensionPixelSize(R.dimen.outer_padding_horizontal);
        outerVerticalPadding = resources.getDimensionPixelSize(R.dimen.outer_padding_vertical);
    }

    /**
     * Set a validator that can indicate a step has an invalid state when the user attempts to move to the next step.
     *
     * @param validator the validator
     */
    public void setStepValidator(@NonNull StepValidator validator) {
        this.validator = validator;
    }

    /**
     * Removes the validator
     */
    public void removeStepValidator() {
        this.validator = alwaysValidValidator;
    }

    /**
     * Set the summary for a given step.
     *
     * @param stepViewId the id of the step's view whose summary should be set.
     * @param summary the summary to set for the step
     */
    public void setStepSummary(int stepViewId, @NonNull String summary) {
        for (Step s : steps) {
            if (s.getInnerView().getId() == stepViewId) {
                s.setSummary(summary);
                invalidate();
                break;
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        List<Step.State> stepStates = new ArrayList<>(steps.size());
        for (Step step : steps) {
            stepStates.add(step.generateState());
        }
        return new SavedState(superState, stepStates);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initSteps(savedState);
        savedState = null;
    }

    @VisibleForTesting
    void initSteps(@Nullable SavedState savedState) {
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(context, continueButtonStyle);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            Step.State initialState = savedState != null ? savedState.stepStates.get(i) : null;
            Step step = new Step(getChildAt(i), new InternalTouchView(context),
                    new AppCompatButton(contextWrapper, null, 0), commonStepValues, initialState);
            steps.add(step);
        }

        for (Step s : steps) {
            initTouchView(s);
            initNavButtons(s);
            syncVisibilityWithActiveState(s);
        }
    }

    @VisibleForTesting
    void initTouchView(final Step step) {
        InternalTouchView touchView = step.getTouchView();
        touchView.setBackgroundResource(step.getTouchViewBackgroundResource());
        touchView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                collapseOtherSteps(step);
                toggleStepExpandedState(step);
            }
        });
        addView(touchView);
    }

    @VisibleForTesting
    void initNavButtons(final Step step) {
        AppCompatButton continueButton = step.getContinueButton();
        continueButton.setText(R.string.continue_button);
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, step.getNavButtonHeight());
        lp.topMargin = step.getNavButtonTopMargin();
        continueButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptStepCompletion(step);
            }
        });
        addView(continueButton, lp);
    }

    @VisibleForTesting
    void attemptStepCompletion(Step step) {
        ValidationResult validation = validator.validate(step.getInnerView(), step.isOptional());
        @ValidationResult.Result int result = validation.getResult();
        if (result == ValidationResult.INVALID) {
            step.setError(validation.getError());
            requestLayout();
        } else {
            step.clearError();
            if (result == ValidationResult.VALID_COMPLETE) {
                step.markComplete();
            }
            toggleStepExpandedState(step);

            int nextIndex = steps.indexOf(step) + 1;
            if (nextIndex < steps.size()) {
                toggleStepExpandedState(steps.get(nextIndex));
            } else {
                // TODO this is the last step. Complete the form
                // TODO Add listener for entire stepper validation
            }
        }
    }

    @VisibleForTesting
    void collapseOtherSteps(Step stepToExcludeFromCollapse) {
        for (Step s : steps) {
            if (s != stepToExcludeFromCollapse && s.isActive()) {
                toggleStepExpandedState(s);
            }
        }
    }

    @VisibleForTesting
    void toggleStepExpandedState(Step step) {
        toggleActiveState(step);
        syncVisibilityWithActiveState(step);
    }

    @VisibleForTesting
    void toggleActiveState(Step step) {
        step.setActive(!step.isActive());
    }

    @VisibleForTesting
    void syncVisibilityWithActiveState(Step step) {
        int visibility = step.isActive() ? View.VISIBLE : View.GONE;
        step.getInnerView().setVisibility(visibility);
        step.getContinueButton().setVisibility(visibility);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        doMeasurement(widthMeasureSpec, heightMeasureSpec);
    }

    @VisibleForTesting
    void doMeasurement(int widthMeasureSpec, int heightMeasureSpec) {
        measureStepDecoratorHeights();
        measureStepBottomMarginHeights();
        measureActiveViews(widthMeasureSpec, heightMeasureSpec);
        int width = calculateWidth();
        int height = calculateHeight();

        width = Math.max(width, getSuggestedMinimumWidth());
        height = Math.max(height, getSuggestedMinimumHeight());

        width = resolveSize(width, widthMeasureSpec);
        height = resolveSize(height, heightMeasureSpec);

        measureTouchViews(width);

        setMeasuredDimension(width, height);
    }

    @VisibleForTesting
    void measureStepDecoratorHeights() {
        for (int i = 0, innerViewsSize = steps.size(); i < innerViewsSize; i++) {
            steps.get(i).measureStepDecoratorHeight();
        }
    }

    @VisibleForTesting
    void measureStepBottomMarginHeights() {
        for (int i = 0, innerViewsSize = steps.size(); i < innerViewsSize - 1; i++) {
            steps.get(i).measureBottomMarginToNextStep();
        }
    }

    @VisibleForTesting
    void measureActiveViews(int widthMeasureSpec, int heightMeasureSpec) {
        int currentHeight = calculateVerticalPadding();
        for (int i = 0, innerViewsSize = steps.size(); i < innerViewsSize; i++) {
            Step step = steps.get(i);
            int activeViewsHeight = 0;

            currentHeight += step.getDecoratorHeight();

            View innerView = step.getInnerView();
            measureActiveView(step, innerView, widthMeasureSpec, heightMeasureSpec, currentHeight);
            int innerHeight = calculateActiveHeight(step, innerView);
            activeViewsHeight += innerHeight;
            currentHeight += innerHeight;

            View continueButton = step.getContinueButton();
            measureActiveView(step, continueButton, widthMeasureSpec, heightMeasureSpec, currentHeight);
            int continueHeight = calculateActiveHeight(step, continueButton);
            activeViewsHeight += continueHeight;
            currentHeight += continueHeight;

            step.setActiveViewsHeight(activeViewsHeight);

            currentHeight += step.getBottomMarginHeight();
        }
    }

    @VisibleForTesting
    void measureActiveView(Step step, View activeView, int parentWms, int parentHms, int currentHeight) {
        LayoutParams lp = (LayoutParams) activeView.getLayoutParams();
        int activeViewUsedWidth = calculateHorizontalPadding() + step.calculateHorizontalUsedSpace(activeView);
        int activeViewWms = nonStaticGetChildMeasureSpec(parentWms, activeViewUsedWidth, lp.width);

        int activeViewUsedHeight = step.calculateVerticalUsedSpace(activeView) + currentHeight;
        int activeViewHms = nonStaticGetChildMeasureSpec(parentHms, activeViewUsedHeight, lp.height);

        activeView.measure(activeViewWms, activeViewHms);
    }

    /***
     * This is simply a non-static version of {@link ViewGroup#getChildMeasureSpec(int, int, int)} for testing.
     */
    @VisibleForTesting
    int nonStaticGetChildMeasureSpec(int spec, int padding, int childDimension) {
        return ViewGroup.getChildMeasureSpec(spec, padding, childDimension);
    }

    @VisibleForTesting
    int calculateActiveHeight(Step step, View activeView) {
        if (step.isActive()) {
            return activeView.getMeasuredHeight() + step.calculateVerticalUsedSpace(activeView);
        }
        return 0;
    }

    @VisibleForTesting
    int calculateWidth() {
        return calculateHorizontalPadding() + calculateMaxStepWidth();
    }

    @VisibleForTesting
    int calculateMaxStepWidth() {
        int width = 0;
        for (int i = 0, innerViewsSize = steps.size(); i < innerViewsSize; i++) {
            Step step = steps.get(i);

            width = Math.max(width, step.calculateStepDecoratorWidth());

            View innerView = step.getInnerView();
            int innerViewHorizontalPadding = step.calculateHorizontalUsedSpace(innerView);
            width = Math.max(width, innerView.getMeasuredWidth() + innerViewHorizontalPadding);

            AppCompatButton continueButton = step.getContinueButton();
            int continueHorizontalPadding = step.calculateHorizontalUsedSpace(continueButton);
            width = Math.max(width, continueButton.getMeasuredWidth() + continueHorizontalPadding);
        }
        return width;
    }

    @VisibleForTesting
    int calculateHeight() {
        int height = calculateVerticalPadding();
        for (Step step : steps) {
            height += step.getDecoratorHeight();
            height += step.getChildrenVisibleHeight();
            height += step.getBottomMarginHeight();
        }
        return height;
    }

    @VisibleForTesting
    void measureTouchViews(int width) {
        for (Step s : steps) {
            measureTouchView(width, s.getTouchViewHeight(), s.getTouchView());
        }
    }

    private void measureTouchView(int width, int height, InternalTouchView view) {
        int wms = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int hms = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        view.measure(wms, hms);
    }

    @VisibleForTesting
    int calculateHorizontalPadding() {
        return outerHorizontalPadding + outerHorizontalPadding + getPaddingLeft() + getPaddingRight();
    }

    @VisibleForTesting
    int calculateVerticalPadding() {
        return outerVerticalPadding + outerVerticalPadding + getPaddingTop() + getPaddingBottom();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (steps.isEmpty()) {
            return;
        }
        Rect rect = steps.get(0).getTempRectForLayout();
        rect.set(getPaddingLeft() + outerHorizontalPadding,
                getPaddingTop() + outerVerticalPadding,
                right - left - getPaddingRight() - outerHorizontalPadding,
                bottom - top - getPaddingBottom() - outerVerticalPadding);
        for (int i = 0, innerViewsSize = steps.size(); i < innerViewsSize; i++) {
            Step step = steps.get(i);

            layoutTouchView(rect, step.getTouchView());

            if (step.isActive()) {
                layoutActiveViews(rect, step);
            }
            rect.top += step.calculateYDistanceToNextStep();
        }
    }

    @VisibleForTesting
    void layoutTouchView(Rect rect, InternalTouchView touchView) {
        // The touch view isn't clipped to the outer padding for so offset it.
        int touchLeft = rect.left - outerHorizontalPadding;

        int touchTop = rect.top - outerVerticalPadding;

        int touchRight = rect.right + outerHorizontalPadding;

        int touchBottomMax = rect.bottom + outerVerticalPadding;
        int touchBottom = Math.min(touchTop + touchView.getMeasuredHeight(), touchBottomMax);

        touchView.layout(touchLeft, touchTop, touchRight, touchBottom);
    }

    @VisibleForTesting
    void layoutActiveViews(Rect rect, Step step) {
        int originalLeft = rect.left;
        int originalTop = rect.top;

        rect.left += step.calculateStepDecoratorIconWidth();
        rect.top += step.calculateYDistanceToTextBottom();

        layoutInnerView(rect, step);

        rect.top += step.getInnerView().getHeight();
        layoutNavButtons(rect, step);

        rect.left = originalLeft;
        rect.top = originalTop;
    }

    @VisibleForTesting
    void layoutInnerView(Rect rect, Step step) {
        layoutActiveView(rect, step.getInnerView());
    }

    @VisibleForTesting
    void layoutNavButtons(Rect rect, Step step) {
        layoutActiveView(rect, step.getContinueButton());
    }

    @VisibleForTesting
    void layoutActiveView(Rect rect, View activeView) {
        LayoutParams lp = (LayoutParams) activeView.getLayoutParams();

        int activeLeft = rect.left + lp.leftMargin;

        int activeTop = rect.top + lp.topMargin;

        int activeRightMax = rect.right - lp.rightMargin;
        int activeRight = Math.min(activeLeft + activeView.getMeasuredWidth(), activeRightMax);

        int activeBottomMax = rect.bottom - lp.bottomMargin;
        int activeBottom = Math.min(activeTop + activeView.getMeasuredHeight(), activeBottomMax);

        activeView.layout(activeLeft, activeTop, activeRight, activeBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        doDraw(canvas);
    }

    @VisibleForTesting
    void doDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(outerHorizontalPadding + getPaddingLeft(), outerVerticalPadding + getPaddingTop());
        int dyToNextStep = 0;
        for (int i = 0, innerViewsSize = steps.size(); i < innerViewsSize; i++) {
            canvas.translate(0, dyToNextStep);

            canvas.save();

            int stepNumber = i + 1;
            Step step = steps.get(i);

            drawIcon(canvas, step, stepNumber);

            drawText(canvas, step);

            boolean hasMoreSteps = stepNumber < innerViewsSize;
            if (hasMoreSteps) {
                dyToNextStep = step.calculateYDistanceToNextStep();

                drawConnector(canvas, step, dyToNextStep);
            }

            canvas.restore();
        }
        canvas.translate(outerHorizontalPadding + getPaddingRight(), outerVerticalPadding + getPaddingBottom());
        canvas.restore();
    }

    @VisibleForTesting
    void drawIcon(Canvas canvas, Step step, int stepNumber) {
        canvas.save();

        if (step.hasError()) {
            drawIconError(canvas, step);
        } else {
            drawIconBackground(canvas, step);
            drawIconText(canvas, step, stepNumber);
        }

        canvas.restore();
    }

    @VisibleForTesting
    void drawIconError(Canvas canvas, Step step) {
        canvas.drawBitmap(step.getIconErrorBitmap(), 0, 0, null);
    }

    @VisibleForTesting
    void drawIconBackground(Canvas canvas, Step step) {
        canvas.drawArc(step.getTempRectForIconBackground(), 0f, 360f, true, step.getIconBackground());
    }

    @VisibleForTesting
    void drawIconText(Canvas canvas, Step step, int stepNumber) {
        String stepNumberString = String.format(Locale.getDefault(), "%d", stepNumber);
        TextPaint iconTextPaint = step.getIconTextPaint();
        int iconDimension = step.getIconDimension();

        PointF center = step.getTempPointForIconTextCenter();
        ViewUtils.findTextCenterStartPoint(stepNumberString, iconDimension, iconDimension, iconTextPaint,
                step.getTempRectForIconTextBounds(), center);

        canvas.drawText(stepNumberString, center.x, center.y, iconTextPaint);
    }

    @VisibleForTesting
    void drawText(Canvas canvas, Step step) {
        canvas.save();

        canvas.translate(step.calculateStepDecoratorIconWidth(), 0);

        drawTitle(canvas, step);
        drawSubtitle(canvas, step);

        canvas.restore();
    }

    @VisibleForTesting
    void drawTitle(Canvas canvas, Step step) {
        TextPaint paint = step.getTitleTextPaint();
        canvas.drawText(step.getTitle(), 0, step.getTitleBaselineRelativeToStepTop(), paint);
    }

    @VisibleForTesting
    void drawSubtitle(Canvas canvas, Step step) {
        String subtitle = step.getSubtitle();
        if (!TextUtils.isEmpty(subtitle)) {
            canvas.translate(0, step.getTitleBottomRelativeToStepTop());
            canvas.drawText(subtitle, 0, step.getSubtitleBaselineRelativeToTitleBottom(), step.getSubtitleTextPaint());
        }
    }

    @VisibleForTesting
    void drawConnector(Canvas canvas, Step step, int yDistanceToNextStep) {
        canvas.save();

        Paint connectorPaint = step.getConnectorPaint();
        float connectorWidth = connectorPaint.getStrokeWidth();
        canvas.translate(ViewUtils.findCenterStartX(connectorWidth, step.getIconDimension()), 0);
        float startY = step.calculateConnectorStartY();
        float stopY = step.calculateConnectorStopY(yDistanceToNextStep);
        canvas.drawLine(0, startY, 0, stopY, connectorPaint);

        canvas.restore();
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(context, attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {

        private static final String EMPTY_TITLE = " ";
        private String title;
        private String summary;
        private boolean isOptional;

        LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.VerticalStepper_Layout);
            try {
                //noinspection ConstantConditions
                title = a.getString(R.styleable.VerticalStepper_Layout_step_title);
                summary = a.getString(R.styleable.VerticalStepper_Layout_step_summary);
                isOptional = a.getBoolean(R.styleable.VerticalStepper_Layout_step_optional, false);
            } finally {
                a.recycle();
            }
            if (TextUtils.isEmpty(title)) {
                throw new IllegalArgumentException("step_title cannot be empty.");
            }
        }

        LayoutParams(int width, int height) {
            super(width, height);
            title = EMPTY_TITLE;
        }

        LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            title = EMPTY_TITLE;
        }

        String getTitle() {
            return title;
        }

        String getSummary() {
            return summary;
        }

        boolean isOptional() {
            return isOptional;
        }
    }

    @VisibleForTesting
    static class InternalTouchView extends View {
        public InternalTouchView(Context context) {
            super(context);
        }
    }

    private static class AlwaysValidValidator implements StepValidator {
        @Override
        public ValidationResult validate(View v, boolean isOptional) {
            return ValidationResult.VALID_COMPLETE_RESULT;
        }
    }

    static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        @VisibleForTesting
        List<Step.State> stepStates;

        SavedState(Parcelable superState, List<Step.State> stepStates) {
            super(superState);
            this.stepStates = stepStates;
        }

        SavedState(Parcel source) {
            this(source, null);
        }

        SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            stepStates = new ArrayList<>();
            source.readTypedList(stepStates, Step.State.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeTypedList(stepStates);
        }
    }
}
