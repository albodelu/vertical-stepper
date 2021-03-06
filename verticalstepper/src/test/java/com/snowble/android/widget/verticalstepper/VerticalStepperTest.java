package com.snowble.android.widget.verticalstepper;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatButton;
import android.text.TextPaint;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class VerticalStepperTest {

    private static class MockedStep {
        View innerView;
        VerticalStepper.LayoutParams innerLayoutParams;
        VerticalStepper.InternalTouchView touchView;
        AppCompatButton continueButton;
        VerticalStepper.LayoutParams continueLayoutParams;
        Step step;

        MockedStep() {
            innerView = mock(View.class);
            innerLayoutParams = mock(VerticalStepper.LayoutParams.class);
            when(innerView.getLayoutParams()).thenReturn(innerLayoutParams);

            continueButton = mock(AppCompatButton.class);
            continueLayoutParams = mock(VerticalStepper.LayoutParams.class);
            when(continueButton.getLayoutParams()).thenReturn(continueLayoutParams);

            touchView = mock(VerticalStepper.InternalTouchView.class);

            step = mock(Step.class);
            when(step.getInnerView()).thenReturn(innerView);
            when(step.getTouchView()).thenReturn(touchView);
            when(step.getContinueButton()).thenReturn(continueButton);
        }
    }

    public abstract static class GivenAStepper extends GivenAnActivity {
        VerticalStepper stepper;

        @Before
        public void givenAStepper() {
            stepper = new VerticalStepper(activity);
        }

        void mockActiveState(MockedStep step, boolean isActive) {
            when(step.step.isActive()).thenReturn(isActive);
            int visibility = isActive ? View.VISIBLE : View.GONE;
            when(step.innerView.getVisibility()).thenReturn(visibility);
            when(step.continueButton.getVisibility()).thenReturn(visibility);
        }
    }

    public static class GivenZeroSteps extends GivenAStepper {
        private int getColor(int colorRes) {
            return ResourcesCompat.getColor(activity.getResources(), colorRes, activity.getTheme());
        }

        @SuppressLint("PrivateResource") // https://code.google.com/p/android/issues/detail?id=230985
        @Test
        public void initPropertiesFromAttrs_NoAttrsSet_ShouldUseDefaults() {
            stepper.initPropertiesFromAttrs(null, 0, 0);

            assertThat(stepper.iconActiveColor).isEqualTo(getColor(R.color.bg_active_icon));
            assertThat(stepper.iconInactiveColor).isEqualTo(getColor(R.color.bg_inactive_icon));
            assertThat(stepper.iconCompleteColor).isEqualTo(stepper.iconActiveColor);
            assertThat(stepper.continueButtonStyle)
                    .isEqualTo(android.support.v7.appcompat.R.style.Widget_AppCompat_Button_Colored);
        }

        @SuppressLint("PrivateResource") // https://code.google.com/p/android/issues/detail?id=230985
        @Test
        public void initPropertiesFromAttrs_AttrsSet_ShouldUseAttrs() {
            Robolectric.AttributeSetBuilder builder = Robolectric.buildAttributeSet();
            builder.addAttribute(R.attr.iconColorActive, "@android:color/black");
            builder.addAttribute(R.attr.iconColorInactive, "@android:color/darker_gray");
            builder.addAttribute(R.attr.iconColorComplete, "@android:color/holo_orange_dark");
            builder.addAttribute(R.attr.continueButtonStyle, "@style/Widget.AppCompat.Button.Borderless");

            stepper.initPropertiesFromAttrs(builder.build(), 0, 0);

            assertThat(stepper.iconActiveColor).isEqualTo(getColor(android.R.color.black));
            assertThat(stepper.iconInactiveColor).isEqualTo(getColor(android.R.color.darker_gray));
            assertThat(stepper.iconCompleteColor).isEqualTo(getColor(android.R.color.holo_orange_dark));
            assertThat(stepper.continueButtonStyle)
                    .isEqualTo(android.support.v7.appcompat.R.style.Widget_AppCompat_Button_Borderless);
        }

        @Test
        public void initSteps_ShouldHaveEmptyInnerViews() {
            stepper.initSteps(null);

            assertThat(stepper.steps).isEmpty();
        }

        @Test
        public void calculateWidth_ShouldReturnHorizontalPadding() {
            int width = stepper.calculateWidth();

            assertThat(width)
                    .isEqualTo(stepper.calculateHorizontalPadding());
        }

        @Test
        public void doMeasurement_UnspecifiedSpecs_ShouldMeasurePadding() {
            int ms = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            stepper.doMeasurement(ms, ms);

            assertThat(stepper.getMeasuredHeight()).isEqualTo(stepper.calculateVerticalPadding());
            assertThat(stepper.getMeasuredWidth()).isEqualTo(stepper.calculateHorizontalPadding());
        }

        @Test
        public void doMeasurement_AtMostSpecsRequiresClipping_ShouldMeasureToAtMostValues() {
            int width = stepper.calculateHorizontalPadding() / 2;
            int height = stepper.calculateVerticalPadding() / 2;
            int wms = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST);
            int hms = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST);

            stepper.doMeasurement(wms, hms);

            assertThat(stepper.getMeasuredWidth()).isEqualTo(width);
            assertThat(stepper.getMeasuredHeight()).isEqualTo(height);
        }

        @Test
        public void doMeasurement_ExactlySpecsRequiresClipping_ShouldMeasureToExactValues() {
            int width = stepper.calculateHorizontalPadding() / 2;
            int height = stepper.calculateVerticalPadding() / 2;
            int wms = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int hms = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

            stepper.doMeasurement(wms, hms);

            assertThat(stepper.getMeasuredWidth()).isEqualTo(width);
            assertThat(stepper.getMeasuredHeight()).isEqualTo(height);
        }

        @Test
        public void doMeasurement_ExactlySpecsRequiresExpanding_ShouldMeasureToExactValues() {
            int width = stepper.calculateHorizontalPadding() * 2;
            int height = stepper.calculateVerticalPadding() * 2;
            int wms = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int hms = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

            stepper.doMeasurement(wms, hms);

            assertThat(stepper.getMeasuredWidth()).isEqualTo(width);
            assertThat(stepper.getMeasuredHeight()).isEqualTo(height);
        }

        @Test
        public void calculateHeight_ShouldReturnVerticalPadding() {
            int width = stepper.calculateHeight();

            assertThat(width)
                    .isEqualTo(stepper.calculateVerticalPadding());
        }

        @Test
        public void calculateHorizontalPadding_ShouldReturnAllPadding() {
            int horizontalPadding = stepper.calculateHorizontalPadding();

            assertThat(horizontalPadding)
                    .isEqualTo((stepper.outerHorizontalPadding * 2) +
                            stepper.getPaddingLeft() + stepper.getPaddingRight());
        }

        @Test
        public void calculateVerticalPadding_ShouldReturnAllPadding() {
            int verticalPadding = stepper.calculateVerticalPadding();

            assertThat(verticalPadding)
                    .isEqualTo((stepper.outerVerticalPadding * 2) +
                            stepper.getPaddingTop() + stepper.getPaddingBottom());
        }

        @Test
        public void layoutTouchView_WhenNotEnoughSpace_ShouldClip() {
            int leftPadding = 20;
            int topPadding = 4;
            int rightPadding = 10;
            int bottomPadding = 2;
            stepper.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);

            int left = 0;
            int top = 0;
            int right = 300;
            int bottom = 500;

            int adjustedLeft = left + stepper.outerHorizontalPadding + leftPadding;
            int adjustedTop = top + stepper.outerVerticalPadding + topPadding;
            int adjustedRight = right - stepper.outerHorizontalPadding - rightPadding;
            int adjustedBottom = bottom - stepper.outerVerticalPadding - bottomPadding;

            VerticalStepper.InternalTouchView touchView = mock(VerticalStepper.InternalTouchView.class);
            when(touchView.getMeasuredHeight()).thenReturn(bottom * 2);

            stepper.layoutTouchView(new Rect(adjustedLeft, adjustedTop, adjustedRight, adjustedBottom), touchView);

            verify(touchView).layout(eq(left + leftPadding), eq(top + topPadding),
                    eq(right - left - rightPadding), eq(bottom - top - bottomPadding));
        }

        @Test
        public void layoutTouchView_WhenEnoughSpace_ShouldUseFullWidthAndMeasuredHeight() {
            int left = 0;
            int top = 0;
            int right = 300;
            int bottom = 500;

            int adjustedLeft = left + stepper.outerHorizontalPadding;
            int adjustedTop = top + stepper.outerVerticalPadding;
            int adjustedRight = right - stepper.outerHorizontalPadding;
            int adjustedBottom = bottom - stepper.outerVerticalPadding;

            VerticalStepper.InternalTouchView touchView = mock(VerticalStepper.InternalTouchView.class);
            int touchMeasuredHeight = bottom / 2;
            when(touchView.getMeasuredHeight()).thenReturn(touchMeasuredHeight);

            stepper.layoutTouchView(new Rect(adjustedLeft, adjustedTop, adjustedRight, adjustedBottom), touchView);

            verify(touchView).layout(eq(left), eq(top), eq(right - left), eq(top + touchMeasuredHeight));
        }

        @Test
        public void layoutActiveView_WhenNotEnoughSpace_ShouldClip() {
            int left = 0;
            int top = 0;
            int right = 300;
            int bottom = 500;

            int leftMargin = 5;
            int topMargin = 20;
            int rightMargin = 10;
            int bottomMargin = 15;

            View activeView = mock(View.class);
            when(activeView.getMeasuredWidth()).thenReturn(right * 2);
            when(activeView.getMeasuredHeight()).thenReturn(bottom * 2);
            when(activeView.getLayoutParams()).thenReturn(
                    createTestLayoutParams(leftMargin, topMargin, rightMargin, bottomMargin));

            stepper.layoutActiveView(new Rect(left, top, right, bottom), activeView);

            verify(activeView).layout(eq(left + leftMargin), eq(top + topMargin),
                    eq(right - rightMargin), eq(bottom - bottomMargin));
        }

        @Test
        public void layoutActiveView_WhenEnoughSpace_ShouldUseFullWidthAndMeasuredHeight() {
            int left = 0;
            int top = 0;
            int right = 300;
            int bottom = 500;

            View activeView = mock(View.class);
            int measuredWidth = right / 2;
            when(activeView.getMeasuredWidth()).thenReturn(measuredWidth);
            int measuredHeight = bottom / 2;
            when(activeView.getMeasuredHeight()).thenReturn(measuredHeight);
            when(activeView.getLayoutParams()).thenReturn(mock(VerticalStepper.LayoutParams.class));

            stepper.layoutActiveView(new Rect(left, top, right, bottom), activeView);

            verify(activeView).layout(eq(left), eq(top), eq(left + measuredWidth), eq(top + measuredHeight));
        }
    }

    public abstract static class GivenOneStep extends GivenAStepper {
        MockedStep mockedStep1;

        @Before
        public void givenOneStep() {
            mockedStep1 = new MockedStep();

            stepper.steps.add(mockedStep1.step);

            clearInvocations(mockedStep1.innerView);
            clearInvocations(mockedStep1.innerLayoutParams);
            clearInvocations(mockedStep1.continueButton);
            clearInvocations(mockedStep1.continueLayoutParams);
            clearInvocations(mockedStep1.touchView);
            clearInvocations(mockedStep1.step);
        }

        void mockStep1Widths(int decoratorWidth, int innerUsedSpace, int innerWidth,
                             int continueUsedSpace, int continueWidth) {
            mockStepWidths(mockedStep1, decoratorWidth, innerUsedSpace, innerWidth, continueUsedSpace, continueWidth);
        }

        void mockStepWidths(MockedStep mockedStep, int decoratorWidth,
                            int innerUsedSpace, int innerWidth,
                            int continueUsedSpace, int continueWidth) {
            when(mockedStep.step.calculateStepDecoratorWidth()).thenReturn(decoratorWidth);
            when(mockedStep.step.calculateHorizontalUsedSpace(mockedStep.innerView)).thenReturn(innerUsedSpace);
            when(mockedStep.innerView.getMeasuredWidth()).thenReturn(innerWidth);
            when(mockedStep.step.calculateHorizontalUsedSpace(mockedStep.continueButton)).thenReturn(continueUsedSpace);
            when(mockedStep.continueButton.getMeasuredWidth()).thenReturn(continueWidth);
        }

        void mockStepHeights(int decoratorHeight, int childrenVisibleHeight, int bottomMarginHeight, Step step) {
            when(step.getDecoratorHeight()).thenReturn(decoratorHeight);
            when(step.getChildrenVisibleHeight()).thenReturn(childrenVisibleHeight);
            when(step.getBottomMarginHeight()).thenReturn(bottomMarginHeight);
        }
    }

    public static class GivenExactlyOneStep extends GivenOneStep {
        @Test
        public void initInnerView_ShouldInitializeStepViews() {
            assertThat(stepper.steps)
                    .hasSize(1)
                    .doesNotContainNull();

            Step step = stepper.steps.get(0);
            assertThat(step.getTouchView())
                    .isNotNull();
            assertThat(step.getContinueButton())
                    .isNotNull();
        }

        @Test
        public void initTouchView_ShouldSetClickListener() {
            stepper.initTouchView(mockedStep1.step);

            verify(mockedStep1.touchView).setOnClickListener((View.OnClickListener) notNull());
        }

        @Test
        public void initTouchView_ShouldAttachToStepper() {
            stepper.initTouchView(mockedStep1.step);

            assertThat(stepper.getChildCount()).isEqualTo(1);
        }

        @Test
        public void initNavButtons_ShouldSetTextToContinue() {
            stepper.initNavButtons(mockedStep1.step);

            verify(mockedStep1.continueButton).setText(R.string.continue_button);
        }

        @Test
        public void initNavButtons_ShouldSetLayoutParamsWithTopMarginAndHeight() {
            int height = 80;
            int topMargin = 20;
            when(mockedStep1.step.getNavButtonHeight()).thenReturn(height);
            when(mockedStep1.step.getNavButtonTopMargin()).thenReturn(topMargin);

            stepper.initNavButtons(mockedStep1.step);

            ArgumentCaptor<VerticalStepper.LayoutParams> lpCaptor =
                    ArgumentCaptor.forClass(VerticalStepper.LayoutParams.class);
            verify(mockedStep1.continueButton).setLayoutParams(lpCaptor.capture());

            VerticalStepper.LayoutParams lp = lpCaptor.getValue();
            assertThat(lp.topMargin).isEqualTo(topMargin);
            assertThat(lp.height).isEqualTo(height);
        }

        @Test
        public void initNavButtons_ShouldSetClickListener() {
            stepper.initNavButtons(mockedStep1.step);

            verify(mockedStep1.continueButton).setOnClickListener((View.OnClickListener) notNull());
        }

        @Test
        public void initNavButtons_ShouldAttachToStepper() {
            stepper.initNavButtons(mockedStep1.step);

            assertThat(stepper.getChildCount()).isEqualTo(1);
        }

        @Test
        public void measureBottomMarginHeights_ShouldNotMeasureBottomMarginToNextStep() {
            stepper.measureStepBottomMarginHeights();

            verify(mockedStep1.step, never()).measureBottomMarginToNextStep();
        }

        @Test
        public void calculateWidth_ShouldReturnHorizontalPaddingAndStepWidth() {
            int decoratorWidth = 20;
            int innerUsedSpace = 20;
            int innerWidth = decoratorWidth * 4;
            int continueUsedSpace = 30;
            int continueWidth = 0;
            mockStep1Widths(decoratorWidth, innerUsedSpace, innerWidth, continueUsedSpace, continueWidth);

            int width = stepper.calculateWidth();

            assertThat(width)
                    .isEqualTo(stepper.calculateHorizontalPadding()
                            + innerWidth + innerUsedSpace);
        }

        @Test
        public void calculateMaxStepWidth_DecoratorsHaveMaxWidth_ShouldReturnDecoratorsWidth() {
            int decoratorWidth = 20;
            int innerUsedSpace = 10;
            int innerWidth = 0;
            int continueUsedSpace = 15;
            int continueWidth = 0;
            mockStep1Widths(decoratorWidth, innerUsedSpace, innerWidth, continueUsedSpace, continueWidth);

            int maxWidth = stepper.calculateMaxStepWidth();

            assertThat(maxWidth)
                    .isEqualTo(decoratorWidth);
        }

        @Test
        public void calculateMaxStepWidth_InnerViewHasMaxWidth_ShouldReturnInnerViewWidth() {
            int decoratorWidth = 20;
            int innerUsedSpace = 20;
            int innerWidth = decoratorWidth * 4;
            int continueUsedSpace = 15;
            int continueWidth = 0;
            mockStep1Widths(decoratorWidth, innerUsedSpace, innerWidth, continueUsedSpace, continueWidth);

            int maxWidth = stepper.calculateMaxStepWidth();

            assertThat(maxWidth)
                    .isEqualTo(innerWidth + innerUsedSpace);
        }

        @Test
        public void calculateMaxStepWidth_NavButtonsHaveMaxWidth_ShouldReturnNavButtonsWidth() {
            int decoratorWidth = 20;
            int innerUsedSpace = 20;
            int innerWidth = 0;
            int continueUsedSpace = 10;
            int continueWidth = decoratorWidth * 4;
            mockStep1Widths(decoratorWidth, innerUsedSpace, innerWidth, continueUsedSpace, continueWidth);

            int maxWidth = stepper.calculateMaxStepWidth();

            assertThat(maxWidth)
                    .isEqualTo(continueWidth + continueUsedSpace);
        }

        @Test
        public void calculateHeight_ShouldReturnVerticalPaddingPlusTotalStepHeight() {
            int decoratorHeight = 100;
            int childrenVisibleHeight = 400;
            int bottomMarginHeight = 48;
            mockStepHeights(decoratorHeight, childrenVisibleHeight, bottomMarginHeight, mockedStep1.step);

            int width = stepper.calculateHeight();

            assertThat(width)
                    .isEqualTo(stepper.calculateVerticalPadding()
                            + decoratorHeight + childrenVisibleHeight + bottomMarginHeight);
        }

        @Test
        public void layoutActiveViews_ShouldNotModifyInputRect() {
            Rect rect = new Rect(1, 2, 3, 4);

            stepper.layoutActiveViews(rect, mockedStep1.step);

            assertThat(rect).isEqualTo(new Rect(1, 2, 3, 4));
        }
    }

    public static class GivenOneStepAndAStepValidator extends GivenOneStep {
        private StepValidator validator;

        @Before
        public void givenStepperSpyWithExactlyTwoStepsAndAStepValidator() {
            validator = mock(StepValidator.class);
            when(validator.validate(mockedStep1.innerView, false))
                    .thenReturn(ValidationResult.VALID_COMPLETE_RESULT);

            stepper.setStepValidator(validator);
        }

        @Test
        public void attemptStepCompletion_HasValidator_ShouldCallListenerWithInnerView() {
            stepper.attemptStepCompletion(mockedStep1.step);

            verify(validator).validate(mockedStep1.innerView, false);
        }

        @Test
        public void attemptStepCompletion_HasValidator_ShoulClearErrorButNotCompleteIfIncomplete() {
            when(validator.validate(mockedStep1.innerView, false))
                    .thenReturn(ValidationResult.VALID_INCOMPLETE_RESULT);

            stepper.attemptStepCompletion(mockedStep1.step);

            verify(mockedStep1.step).clearError();
            verify(mockedStep1.step, never()).markComplete();
        }

        @Test
        public void attemptStepCompletion_HasValidator_ShouldCompleteIfStepValidAndComplete() {
            stepper.attemptStepCompletion(mockedStep1.step);

            verify(mockedStep1.step).clearError();
            verify(mockedStep1.step).markComplete();
        }

        @Test
        public void attemptStepCompletion_removeStepValidator_ShouldComplete() {
            String error = "error";
            ValidationResult result = new ValidationResult(error);
            when(validator.validate(mockedStep1.innerView, false)).thenReturn(result);
            stepper.removeStepValidator();

            stepper.attemptStepCompletion(mockedStep1.step);

            verify(mockedStep1.step, never()).setError(error);
            verify(mockedStep1.step).clearError();
            verify(mockedStep1.step).markComplete();
        }
    }

    public static class GivenExactlyOneActiveStep extends GivenOneStep {

        @Before
        public void givenExactlyOneActiveStep() {
            mockActiveState(mockedStep1, true);
        }

        @Test
        public void syncVisibilityWithActiveState_ShouldMakeActiveViewsVisible() {
            stepper.syncVisibilityWithActiveState(mockedStep1.step);

            verify(mockedStep1.innerView).setVisibility(View.VISIBLE);
            verify(mockedStep1.continueButton).setVisibility(View.VISIBLE);
        }

        @Test
        public void measureActiveViews_ShouldHaveActiveViewsHeightsWithActualHeight() {
            final int innerViewHeight = 100;
            final int buttonHeight = 50;
            when(mockedStep1.innerView.getMeasuredHeight()).thenReturn(innerViewHeight);
            when(mockedStep1.continueButton.getMeasuredHeight()).thenReturn(buttonHeight);

            int ms = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            stepper.measureActiveViews(ms, ms);

            verify(mockedStep1.step).setActiveViewsHeight(innerViewHeight + buttonHeight);
        }
    }

    public static class GivenExactlyOneInactiveStep extends GivenOneStep {

        @Before
        public void givenExactlyOneInactiveStep() {
            mockActiveState(mockedStep1, false);
        }

        @Test
        public void syncVisibilityWithActiveState_ShouldMakeActiveViewsGone() {
            stepper.syncVisibilityWithActiveState(mockedStep1.step);

            verify(mockedStep1.innerView).setVisibility(View.GONE);
            verify(mockedStep1.continueButton).setVisibility(View.GONE);
        }
    }

    public static abstract class GivenTwoSteps extends GivenOneStep {
        MockedStep mockedStep2;

        @Before
        public void givenTwoSteps() {
            mockedStep2 = new MockedStep();

            stepper.steps.add(mockedStep2.step);

            clearInvocations(mockedStep2.innerView);
            clearInvocations(mockedStep2.innerLayoutParams);
            clearInvocations(mockedStep2.continueButton);
            clearInvocations(mockedStep2.continueLayoutParams);
            clearInvocations(mockedStep2.touchView);
            clearInvocations(mockedStep2.step);
        }

        void mockStep2Widths(int decoratorWidth, int innerUsedSpace, int innerWidth,
                             int continueUsedSpace, int continueWidth) {
            mockStepWidths(mockedStep2, decoratorWidth, innerUsedSpace, innerWidth, continueUsedSpace, continueWidth);
        }
    }

    public static class GivenExactlyTwoSteps extends GivenTwoSteps {
        @Test
        public void onSaveInstanceState_ShouldSaveStepStates() {
            Parcelable state = stepper.onSaveInstanceState();

            verify(mockedStep1.step).generateState();
            verify(mockedStep2.step).generateState();
            assertThat(state).isInstanceOf(VerticalStepper.SavedState.class);
            VerticalStepper.SavedState ss = (VerticalStepper.SavedState) state;
            assertThat(ss.stepStates).hasSize(2);
        }

        @Test
        public void measureStepDecoratorHeights_ShouldMeasureStepDecoratorHeightTwice() {
            stepper.measureStepDecoratorHeights();

            verify(mockedStep1.step).measureStepDecoratorHeight();
            verify(mockedStep2.step).measureStepDecoratorHeight();
        }

        @Test
        public void measureBottomMarginHeights_ShouldMeasureBottomMarginToNextStepOnce() {
            stepper.measureStepBottomMarginHeights();

            verify(mockedStep1.step).measureBottomMarginToNextStep();
        }

        @Test
        public void measureActiveViews_ShouldMeasureViews() {
            int ms = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            stepper.measureActiveViews(ms, ms);

            verify(mockedStep1.innerView).measure(anyInt(), anyInt());
            verify(mockedStep2.innerView).measure(anyInt(), anyInt());
            verify(mockedStep1.continueButton).measure(anyInt(), anyInt());
            verify(mockedStep1.continueButton).measure(anyInt(), anyInt());
        }

        @Test
        public void calculateMaxStepWidth_ShouldReturnLargerStepWidth() {
            int decoratorWidth = 20;
            int innerUsedSpace = 20;
            int continueWidth = 0;
            int continueUsedSpace = 10;

            int inner1Width = decoratorWidth * 2;
            mockStep1Widths(decoratorWidth, innerUsedSpace, inner1Width, continueUsedSpace, continueWidth);

            int inner2Width = decoratorWidth * 3;
            mockStep2Widths(decoratorWidth, innerUsedSpace, inner2Width, continueUsedSpace, continueWidth);

            int maxWidth = stepper.calculateMaxStepWidth();

            assertThat(maxWidth)
                    .isNotEqualTo(inner1Width + innerUsedSpace)
                    .isEqualTo(inner2Width + innerUsedSpace);
        }

        @Test
        public void calculateHeight_ShouldReturnVerticalPaddingPlusTotalStepHeight() {
            int decoratorHeight = 100;
            int childrenVisibleHeight = 400;
            int bottomMarginHeight = 48;
            mockStepHeights(decoratorHeight, childrenVisibleHeight, bottomMarginHeight, mockedStep1.step);
            mockStepHeights(decoratorHeight, childrenVisibleHeight, bottomMarginHeight, mockedStep2.step);

            int height = stepper.calculateHeight();

            assertThat(height)
                    .isEqualTo(stepper.calculateVerticalPadding()
                            + (2 * (decoratorHeight + childrenVisibleHeight + bottomMarginHeight)));
        }

        @Test
        public void measureTouchViews_ShouldMeasureAllWidthsAndHeightsExactly() {
            int width = 20;
            int height = 80;
            when(mockedStep1.step.getTouchViewHeight()).thenReturn(height);
            when(mockedStep2.step.getTouchViewHeight()).thenReturn(height);

            stepper.measureTouchViews(width);

            ArgumentCaptor<Integer> wmsCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Integer> hmsCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(mockedStep1.touchView).measure(wmsCaptor.capture(), hmsCaptor.capture());
            verify(mockedStep2.touchView).measure(wmsCaptor.capture(), hmsCaptor.capture());

            for (int actualWms : wmsCaptor.getAllValues()) {
                assertThat(View.MeasureSpec.getMode(actualWms)).isEqualTo(View.MeasureSpec.EXACTLY);
                assertThat(View.MeasureSpec.getSize(actualWms)).isEqualTo(width);
            }

            for (int actualHms : hmsCaptor.getAllValues()) {
                assertThat(View.MeasureSpec.getMode(actualHms)).isEqualTo(View.MeasureSpec.EXACTLY);
                assertThat(View.MeasureSpec.getSize(actualHms)).isEqualTo(height);
            }
        }
    }

    public static abstract class GivenStepperSpy extends GivenAStepper {
        VerticalStepper stepperSpy;

        @Before
        public void givenStepperSpy() {
            stepperSpy = spy(stepper);
        }
    }

    public static class GivenEmptyStepperSpy extends GivenStepperSpy {
        @Test
        public void onAttachedToWindow_ShouldInitSteps() {
            doNothing().when(stepperSpy).initSteps(null);

            stepperSpy.onAttachedToWindow();

            verify(stepperSpy).initSteps(null);
        }

        @SuppressLint("WrongCall") // Explicitly testing onMeasure
        @Test
        public void onMeasure_ShouldCallDoMeasurement() {
            doNothing().when(stepperSpy).doMeasurement(anyInt(), anyInt());

            stepperSpy.onMeasure(0, 0);

            verify(stepperSpy).doMeasurement(eq(0), eq(0));
        }

        @SuppressLint("WrongCall") // Explicitly testing onDraw
        @Test
        public void onDraw_ShouldCallDoDraw() {
            Canvas canvas = mock(Canvas.class);
            doNothing().when(stepperSpy).doDraw(same(canvas));

            stepperSpy.onDraw(canvas);

            verify(stepperSpy).doDraw(canvas);
        }
    }

    public static class GivenStepperSpyWithStubbedInitStepsMethods extends GivenStepperSpy {
        @Before
        public void givenStepperSpyWithStubbedInitStepsMethods() {
            View child1 = mock(View.class);
            View child2 = mock(View.class);
            VerticalStepper.LayoutParams lp = mock(VerticalStepper.LayoutParams.class);
            when(lp.getTitle()).thenReturn("title");
            when(child1.getLayoutParams()).thenReturn(lp);
            when(child2.getLayoutParams()).thenReturn(lp);

            // For some reason, calling addView() doesn't update the children properly with the stepperSpy.
            // So explicitly set child count and children
            doReturn(2).when(stepperSpy).getChildCount();
            doReturn(child1).when(stepperSpy).getChildAt(0);
            doReturn(child2).when(stepperSpy).getChildAt(1);

            doNothing().when(stepperSpy).initTouchView(any(Step.class));
            doNothing().when(stepperSpy).initNavButtons(any(Step.class));
            doNothing().when(stepperSpy).syncVisibilityWithActiveState(any(Step.class));
        }

        @Test
        public void initSteps_ShouldInitStepsAndChildViews() {
            stepperSpy.initSteps(null);

            verify(stepperSpy, times(2)).initTouchView(any(Step.class));
            verify(stepperSpy, times(2)).initNavButtons(any(Step.class));
            verify(stepperSpy, times(2)).syncVisibilityWithActiveState(any(Step.class));
            assertThat(stepperSpy.steps).hasSize(2).doesNotContainNull();
        }

        @Test
        public void initSteps_ShouldSetStatesForSteps() {
            String summary = "summary";
            String error = "error";
            List<Step.State> states = Arrays.asList(new Step.State(false, true, null, summary),
                    new Step.State(true, false, error, null));
            VerticalStepper.SavedState state = new VerticalStepper.SavedState(mock(Parcelable.class), states);

            stepperSpy.initSteps(state);

            Step step1 = stepperSpy.steps.get(0);
            assertThat(step1.isActive()).isFalse();
            assertThat(step1.isComplete()).isTrue();
            assertThat(step1.hasError()).isFalse();
            assertThat(step1.getSubtitle()).isEqualTo(summary);

            Step step2 = stepperSpy.steps.get(1);
            assertThat(step2.isActive()).isTrue();
            assertThat(step2.isComplete()).isFalse();
            assertThat(step2.hasError()).isTrue();
            assertThat(step2.getSubtitle()).isEqualTo(error);
        }
    }

    public static abstract class GivenStepperSpyWithTwoSteps extends GivenStepperSpy {
        MockedStep mockedStep1;
        MockedStep mockedStep2;

        @Before
        public void givenStepperSpyWithTwoSteps() {
            mockedStep1 = new MockedStep();
            mockedStep2 = new MockedStep();
            stepperSpy.steps.add(mockedStep1.step);
            stepperSpy.steps.add(mockedStep2.step);
        }
    }

    public static class GivenStepperSpyWithExactlyTwoSteps extends GivenStepperSpyWithTwoSteps {
        @Test
        public void touchViewOnClickListener_ShouldCallCollapseOtherStepsAndToggle() {
            ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);
            stepperSpy.initTouchView(mockedStep1.step);
            verify(mockedStep1.touchView).setOnClickListener(captor.capture());
            View.OnClickListener clickListenerSpy = spy(captor.getValue());

            doNothing().when(stepperSpy).collapseOtherSteps(mockedStep1.step);
            doNothing().when(stepperSpy).toggleStepExpandedState(mockedStep1.step);

            clickListenerSpy.onClick(mock(View.class));

            verify(stepperSpy).collapseOtherSteps(mockedStep1.step);
            verify(stepperSpy).toggleStepExpandedState(mockedStep1.step);
        }

        @Test
        public void continueButtonOnClickListener_ShouldCallCompleteStep() {
            ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);
            stepperSpy.initNavButtons(mockedStep1.step);
            verify(mockedStep1.continueButton).setOnClickListener(captor.capture());
            View.OnClickListener clickListenerSpy = spy(captor.getValue());

            doNothing().when(stepperSpy).attemptStepCompletion(mockedStep1.step);

            clickListenerSpy.onClick(mock(View.class));

            verify(stepperSpy).attemptStepCompletion(mockedStep1.step);
        }

        @Test
        public void attemptStepCompletion_HasValidator_ShouldSetErrorAndRelayout() {
            StepValidator validator = mock(StepValidator.class);
            String error = "error";
            ValidationResult result = new ValidationResult(error);
            when(validator.validate(mockedStep1.innerView, false)).thenReturn(result);
            stepperSpy.setStepValidator(validator);

            stepperSpy.attemptStepCompletion(mockedStep1.step);

            verify(mockedStep1.step).setError(error);
            verify(stepperSpy).requestLayout();
        }

        @Test
        public void attemptStepCompletion_ShouldCollapseCompleteCurrentStep() {
            mockActiveState(mockedStep1, true);

            stepperSpy.attemptStepCompletion(mockedStep1.step);

            verify(mockedStep1.step).clearError();
            verify(mockedStep1.step).markComplete();
            verify(stepperSpy).toggleStepExpandedState(mockedStep1.step);
        }

        @Test
        public void attemptStepCompletion_ShouldExpandNextStep() {
            mockActiveState(mockedStep1, true);
            mockActiveState(mockedStep2, false);

            stepperSpy.attemptStepCompletion(mockedStep1.step);

            verify(stepperSpy).toggleStepExpandedState(mockedStep2.step);
        }

        @Test
        public void attemptStepCompletion_LastStep_ShouldOnlyCollapseCurrentStep() {
            mockActiveState(mockedStep2, true);

            stepperSpy.attemptStepCompletion(mockedStep2.step);

            verify(stepperSpy).attemptStepCompletion(mockedStep2.step);
            verify(stepperSpy).toggleStepExpandedState(mockedStep2.step);
            verify(stepperSpy).toggleActiveState(mockedStep2.step);
            verify(stepperSpy).syncVisibilityWithActiveState(mockedStep2.step);
            verifyNoMoreInteractions(stepperSpy);
        }

        @Test
        public void collapseOtherSteps_ShouldCollapseAnyOtherActiveSteps() {
            mockActiveState(mockedStep1, true);
            mockActiveState(mockedStep2, true);

            stepperSpy.collapseOtherSteps(mockedStep1.step);

            verify(stepperSpy, never()).toggleStepExpandedState(mockedStep1.step);
            verify(stepperSpy).toggleStepExpandedState(mockedStep2.step);
        }

        @Test
        public void collapseOtherSteps_ShouldNotCollapseListenerStep() {
            mockActiveState(mockedStep1, true);

            stepper.collapseOtherSteps(mockedStep1.step);

            verify(stepperSpy, never()).toggleStepExpandedState(mockedStep1.step);
        }

        @Test
        public void toggleStepExpandedState_ShouldToggleActiveStateAndSyncVisibility() {
            stepperSpy.toggleStepExpandedState(mockedStep1.step);

            verify(stepperSpy).toggleActiveState(mockedStep1.step);
            verify(stepperSpy).syncVisibilityWithActiveState(mockedStep1.step);
        }

        @Test
        public void measureActiveView_ShouldMeasureActiveViewAccountingForUsedSpace() {
            int currentHeight = 30;
            int horizontalPadding = 40;
            doReturn(horizontalPadding).when(stepperSpy).calculateHorizontalPadding();

            VerticalStepper.LayoutParams innerLp = createTestLayoutParams(5, 10, 5, 10);
            innerLp.width = VerticalStepper.LayoutParams.WRAP_CONTENT;
            innerLp.height = VerticalStepper.LayoutParams.WRAP_CONTENT;
            when(mockedStep1.innerView.getLayoutParams()).thenReturn(innerLp);

            int innerHorizontalUsedSpace = 20;
            when(mockedStep1.step.calculateHorizontalUsedSpace(mockedStep1.innerView))
                    .thenReturn(innerHorizontalUsedSpace);

            int innerVerticalUsedSpace = 20;
            when(mockedStep1.step.calculateVerticalUsedSpace(mockedStep1.innerView))
                    .thenReturn(innerVerticalUsedSpace);

            int maxWidth = 1080;
            int maxHeight = 1920;
            int wms = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST);
            int hms = View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST);

            int expectedWidthSpec = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.AT_MOST);
            int expectedHeightSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.AT_MOST);
            doReturn(expectedWidthSpec).when(stepperSpy).nonStaticGetChildMeasureSpec(eq(wms), anyInt(), anyInt());
            doReturn(expectedHeightSpec).when(stepperSpy).nonStaticGetChildMeasureSpec(eq(hms), anyInt(), anyInt());

            stepperSpy.measureActiveView(mockedStep1.step, mockedStep1.innerView, wms, hms, currentHeight);

            verify(stepperSpy).nonStaticGetChildMeasureSpec(wms, horizontalPadding + innerHorizontalUsedSpace,
                    VerticalStepper.LayoutParams.WRAP_CONTENT);
            verify(stepperSpy).nonStaticGetChildMeasureSpec(hms, currentHeight + innerVerticalUsedSpace,
                    VerticalStepper.LayoutParams.WRAP_CONTENT);

            verify(mockedStep1.innerView).measure(expectedWidthSpec, expectedHeightSpec);
        }
    }

    public static class GivenStepperSpyWithTwoStepsAndInnerViewIds extends GivenStepperSpyWithTwoSteps {
        private int innerViewId1;
        private int innerViewId2;

        @Before
        public void givenStepperSpyWithTwoStepsAndInnerViewIds() {
            innerViewId1 = 21;
            innerViewId2 = 22;
            when(mockedStep1.innerView.getId()).thenReturn(innerViewId1);
            when(mockedStep2.innerView.getId()).thenReturn(innerViewId2);

            doNothing().when(stepperSpy).invalidate();
        }

        @Test
        public void setStepSummary_UnrecognizedViewId_ShouldDoNothing() {
            stepperSpy.setStepSummary(-1, "summary");

            verify(mockedStep1.step, never()).setSummary(anyString());
            verify(stepperSpy, never()).invalidate();
        }

        @Test
        public void setStepSummary_ShouldSetStepSummaryAndInvalidate() {
            String summary = "summary";
            stepperSpy.setStepSummary(innerViewId1, summary);

            verify(mockedStep1.step).setSummary(summary);
            verify(stepperSpy).invalidate();
        }
    }

    public static class GivenStepperSpyWithTwoStepsAndStandardActiveDimensions extends GivenStepperSpyWithTwoSteps {

        private static final int MAX_WIDTH = 1080;
        private static final int MAX_HEIGHT = 1920;
        private static final int WMS = View.MeasureSpec.makeMeasureSpec(MAX_WIDTH, View.MeasureSpec.AT_MOST);
        private static final int HMS = View.MeasureSpec.makeMeasureSpec(MAX_HEIGHT, View.MeasureSpec.AT_MOST);

        private static final int VERTICAL_PADDING = 30;
        private static final int DECORATOR_HEIGHT = 30;
        private static final int INNER_ACTIVE_HEIGHT = 200;
        private static final int CONTINUE_ACTIVE_HEIGHT = 50;
        private static final int BOTTOM_MARGIN = 30;

        @Before
        public void givenStepperSpyWithExactlyTwoStepsAndStandardActiveDimensions() {
            doNothing()
                    .when(stepperSpy).measureActiveView(any(Step.class), any(View.class), anyInt(), anyInt(), anyInt());
            doReturn(VERTICAL_PADDING).when(stepperSpy).calculateVerticalPadding();
            doReturn(DECORATOR_HEIGHT).when(mockedStep1.step).getDecoratorHeight();
            doReturn(DECORATOR_HEIGHT).when(mockedStep2.step).getDecoratorHeight();
            doReturn(INNER_ACTIVE_HEIGHT)
                    .when(stepperSpy).calculateActiveHeight(mockedStep1.step, mockedStep1.innerView);
            doReturn(INNER_ACTIVE_HEIGHT)
                    .when(stepperSpy).calculateActiveHeight(mockedStep2.step, mockedStep2.innerView);
            doReturn(CONTINUE_ACTIVE_HEIGHT)
                    .when(stepperSpy).calculateActiveHeight(mockedStep1.step, mockedStep1.continueButton);
            doReturn(CONTINUE_ACTIVE_HEIGHT)
                    .when(stepperSpy).calculateActiveHeight(mockedStep2.step, mockedStep2.continueButton);
            doReturn(BOTTOM_MARGIN).when(mockedStep1.step).getBottomMarginHeight();
            doReturn(0).when(mockedStep2.step).getBottomMarginHeight();
        }

        @Test
        public void measureActiveViews_ShouldMeasureActiveViewsAccountingForDecorator() {
            doReturn(0).when(stepperSpy).calculateActiveHeight(mockedStep1.step, mockedStep1.innerView);

            stepperSpy.measureActiveViews(WMS, HMS);

            verify(stepperSpy).measureActiveView(mockedStep1.step, mockedStep1.innerView, WMS, HMS,
                    VERTICAL_PADDING + DECORATOR_HEIGHT);
            verify(stepperSpy).measureActiveView(mockedStep1.step, mockedStep1.continueButton, WMS, HMS,
                    VERTICAL_PADDING + DECORATOR_HEIGHT);
        }

        @Test
        public void measureActiveViews_ShouldMeasureNavButtonsAccountingForInnerView() {
            stepperSpy.measureActiveViews(WMS, HMS);

            verify(stepperSpy).measureActiveView(mockedStep1.step, mockedStep1.continueButton, WMS, HMS,
                    VERTICAL_PADDING + DECORATOR_HEIGHT + INNER_ACTIVE_HEIGHT);
        }

        @Test
        public void measureActiveViews_ShouldMeasureActiveViewsAccountingForBottomMargin() {
            stepperSpy.measureActiveViews(WMS, HMS);

            verify(stepperSpy).measureActiveView(mockedStep1.step, mockedStep1.innerView, WMS, HMS,
                    VERTICAL_PADDING + DECORATOR_HEIGHT);
            verify(stepperSpy).measureActiveView(mockedStep1.step, mockedStep1.continueButton, WMS, HMS,
                    VERTICAL_PADDING + DECORATOR_HEIGHT + INNER_ACTIVE_HEIGHT);
            verify(stepperSpy).measureActiveView(mockedStep2.step, mockedStep2.innerView, WMS, HMS,
                    VERTICAL_PADDING + 2 * DECORATOR_HEIGHT + INNER_ACTIVE_HEIGHT
                            + CONTINUE_ACTIVE_HEIGHT + BOTTOM_MARGIN);
            verify(stepperSpy).measureActiveView(mockedStep2.step, mockedStep2.continueButton, WMS, HMS,
                    VERTICAL_PADDING + 2 * (DECORATOR_HEIGHT + INNER_ACTIVE_HEIGHT)
                            + CONTINUE_ACTIVE_HEIGHT + BOTTOM_MARGIN);
        }
    }

    public static class GivenStepperSpyWithTwoStepsAndStubbedDrawMethods
            extends GivenStepperSpyWithTwoStepsAndMockCanvas {
        @Before
        public void givenStepperSpyWithTwoStepsAndStubbedDrawMethods() {
            doNothing().when(stepperSpy).drawIcon(same(canvas), any(Step.class), anyInt());
            doNothing().when(stepperSpy).drawText(same(canvas), any(Step.class));
            doNothing().when(stepperSpy).drawConnector(same(canvas), any(Step.class), anyInt());
        }

        @Test
        public void doDraw_ShouldCallDrawIconTwice() {
            InOrder order = inOrder(stepperSpy);

            stepperSpy.doDraw(canvas);

            order.verify(stepperSpy).drawIcon(canvas, mockedStep1.step, 1);
            order.verify(stepperSpy).drawIcon(canvas, mockedStep2.step, 2);
        }

        @Test
        public void doDraw_ShouldCallDrawTextTwice() {
            InOrder order = inOrder(stepperSpy);

            stepperSpy.doDraw(canvas);

            order.verify(stepperSpy).drawText(canvas, mockedStep1.step);
            order.verify(stepperSpy).drawText(canvas, mockedStep2.step);
        }

        @Test
        public void doDraw_ShouldCallDrawConnectorOnce() {
            int distanceToNextStep = 300;
            when(mockedStep1.step.calculateYDistanceToNextStep()).thenReturn(distanceToNextStep);

            stepperSpy.doDraw(canvas);

            verify(stepperSpy).drawConnector(canvas, mockedStep1.step, distanceToNextStep);
        }

        @Test
        public void doDraw_ShouldTranslateByDistanceToNextStep() {
            InOrder order = inOrder(canvas);

            int leftPadding = 10;
            int topPadding = 20;
            int rightPadding = 5;
            int bottomPadding = 15;
            stepperSpy.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);

            int distanceToNextStep = 300;
            when(mockedStep1.step.calculateYDistanceToNextStep()).thenReturn(distanceToNextStep);

            stepperSpy.doDraw(canvas);

            // first translate for the left and top padding
            order.verify(canvas).translate(stepperSpy.outerHorizontalPadding + leftPadding,
                    stepperSpy.outerVerticalPadding + topPadding);

            // translate for the first step
            order.verify(canvas).translate(0, 0);

            // translate for the second step
            order.verify(canvas).translate(0, distanceToNextStep);

            // finally translate for the right and bottom padding
            order.verify(canvas).translate(stepperSpy.outerHorizontalPadding + rightPadding,
                    stepperSpy.outerVerticalPadding + bottomPadding);
        }

        @Test
        public void doDraw_ShouldSaveAndRestoreForEachChild() {
            InOrder order = inOrder(canvas);

            stepperSpy.doDraw(canvas);

            // for all of doDraw
            order.verify(canvas).save();

            // first step
            order.verify(canvas).save();
            order.verify(canvas).restore();

            // second step
            order.verify(canvas).save();
            order.verify(canvas).restore();

            // for all of doDraw
            order.verify(canvas).restore();
        }
    }

    public static class GivenStepperSpyWithTwoStepsAndStubbedDrawIconMethods
            extends GivenStepperSpyWithTwoStepsAndMockCanvas {

        @Before
        public void givenStepperSpyWithTwoStepsAndStubbedDrawIconMethods() {
            doNothing().when(stepperSpy).drawIconBackground(same(canvas), any(Step.class));
            doNothing().when(stepperSpy).drawIconError(same(canvas), any(Step.class));
            doNothing().when(stepperSpy).drawIconText(same(canvas), any(Step.class), anyInt());
        }

        @Test
        public void doDraw_HasError_ShouldCallDrawIconError() {
            when(mockedStep1.step.hasError()).thenReturn(true);

            stepperSpy.drawIcon(canvas, mockedStep1.step, 1);

            verify(stepperSpy).drawIconError(canvas, mockedStep1.step);
        }

        @Test
        public void drawIcon_ShouldCallDrawIconBackgroundAndDrawIconText() {
            stepperSpy.drawIcon(canvas, mockedStep1.step, 1);

            verify(stepperSpy).drawIconBackground(canvas, mockedStep1.step);
            verify(stepperSpy).drawIconText(canvas, mockedStep1.step, 1);
        }

        @Test
        public void drawIcon_ShouldCallSaveAndRestore() {
            InOrder order = inOrder(canvas);

            stepperSpy.drawIcon(canvas, mockedStep1.step, 1);

            order.verify(canvas).save();
            order.verify(canvas).restore();
        }
    }

    public static class GivenStepperSpyWithTwoStepsAndStubbedDrawTextMethods
            extends GivenStepperSpyWithTwoStepsAndMockCanvas {

        @Before
        public void givenStepperSpyWithTwoStepsAndStubbedDrawTextMethods() {
            doNothing().when(stepperSpy).drawTitle(same(canvas), any(Step.class));
            doNothing().when(stepperSpy).drawSubtitle(same(canvas), any(Step.class));
        }

        @Test
        public void drawText_ShouldCallDrawTitleAndDrawSubtitle() {
            stepperSpy.drawText(canvas, mockedStep1.step);

            verify(stepperSpy).drawTitle(canvas, mockedStep1.step);
            verify(stepperSpy).drawSubtitle(canvas, mockedStep1.step);
        }

        @Test
        public void drawText_ShouldSaveTranslateByIconWidthAndRestoreCanvas() {
            InOrder order = inOrder(canvas);
            int iconWidth = 40;
            when(mockedStep1.step.calculateStepDecoratorIconWidth()).thenReturn(iconWidth);

            stepperSpy.drawText(canvas, mockedStep1.step);

            order.verify(canvas).save();
            order.verify(canvas).translate(iconWidth, 0);
            order.verify(canvas).restore();
        }
    }

    public static abstract class GivenStepperSpyWithTwoStepsAndMockCanvas extends GivenStepperSpyWithTwoSteps {
        Canvas canvas;

        @Before
        public void givenStepperSpyWithTwoStepsAndMockCanvas() {
            canvas = mock(Canvas.class);
        }
    }

    public static class GivenStepperSpyWithExactlyTwoStepsAndMockCanvas
            extends GivenStepperSpyWithTwoStepsAndMockCanvas {
        @Test
        public void drawIconError_ShouldDrawErrorBitmap() {
            when(mockedStep1.step.hasError()).thenReturn(true);
            Bitmap bitmap = mock(Bitmap.class);
            when(mockedStep1.step.getIconErrorBitmap()).thenReturn(bitmap);

            stepperSpy.drawIconError(canvas, mockedStep1.step);

            verify(canvas).drawBitmap(bitmap, 0, 0, null);
        }

        @Test
        public void drawIconBackground_ShouldDrawCircleWithIconColor() {
            Paint color = mock(Paint.class);
            RectF rect = mock(RectF.class);
            when(mockedStep1.step.getIconBackground()).thenReturn(color);
            when(mockedStep1.step.getTempRectForIconBackground()).thenReturn(rect);

            stepperSpy.drawIconBackground(canvas, mockedStep1.step);

            verify(canvas).drawArc(rect, 0f, 360f, true, color);
        }

        @Test
        public void drawIconText_ShouldDrawStepNumber() {
            TextPaint paint = mock(TextPaint.class);
            Rect rect = mock(Rect.class);
            PointF point = mock(PointF.class);
            when(mockedStep1.step.getIconTextPaint()).thenReturn(paint);
            when(mockedStep1.step.getTempRectForIconTextBounds()).thenReturn(rect);
            when(mockedStep1.step.getTempPointForIconTextCenter()).thenReturn(point);
            int stepNumber = 4;

            stepperSpy.drawIconText(canvas, mockedStep1.step, stepNumber);

            String stepNumberString = String.valueOf(stepNumber);
            verify(canvas).drawText(eq(stepNumberString), anyFloat(), anyFloat(), same(paint));
        }

        @Test
        public void drawTitle_ShouldDrawTextWithStepTitle() {
            TextPaint paint = mock(TextPaint.class);
            String title = "vertical stepper";
            float titleBaseline = 20f;
            when(mockedStep1.step.getTitle()).thenReturn(title);
            when(mockedStep1.step.getTitleTextPaint()).thenReturn(paint);
            when(mockedStep1.step.getTitleBaselineRelativeToStepTop()).thenReturn(titleBaseline);

            stepperSpy.drawTitle(canvas, mockedStep1.step);

            verify(canvas).drawText(title, 0, titleBaseline, paint);
        }

        @Test
        public void drawSubtitle_WhenEmpty_ShouldNotDraw() {
            TextPaint paint = mock(TextPaint.class);
            when(mockedStep1.step.getSubtitleTextPaint()).thenReturn(paint);
            when(mockedStep1.step.getSubtitle()).thenReturn("");

            stepperSpy.drawSubtitle(canvas, mockedStep1.step);

            verify(canvas, never()).drawText(anyString(), anyFloat(), anyFloat(), eq(paint));
        }

        @Test
        public void drawSubtitle_NotEmpty_ShouldTranslateAndDraw() {
            InOrder order = inOrder(canvas);

            String subtitle = "subtitle";
            when(mockedStep1.step.getSubtitle()).thenReturn(subtitle);
            float titleBottom = 15f;
            when(mockedStep1.step.getTitleBottomRelativeToStepTop()).thenReturn(titleBottom);
            float subtitleBaseline = 20f;
            when(mockedStep1.step.getSubtitleBaselineRelativeToTitleBottom()).thenReturn(subtitleBaseline);
            TextPaint paint = mock(TextPaint.class);
            when(mockedStep1.step.getSubtitleTextPaint()).thenReturn(paint);

            stepperSpy.drawSubtitle(canvas, mockedStep1.step);

            order.verify(canvas).translate(0, titleBottom);
            order.verify(canvas).drawText(subtitle, 0, subtitleBaseline, paint);
        }

        @Test
        public void drawConnector_ShouldSaveTranslateDrawAndRestore() {
            InOrder order = inOrder(canvas);
            Paint paint = mock(Paint.class);
            float strokeWidth = 3f;
            when(paint.getStrokeWidth()).thenReturn(strokeWidth);
            when(mockedStep1.step.getConnectorPaint()).thenReturn(paint);

            stepperSpy.drawConnector(canvas, mockedStep1.step, 0);

            order.verify(canvas).save();
            order.verify(canvas).translate(anyFloat(), anyFloat());
            order.verify(canvas).drawLine(anyFloat(), anyFloat(), anyFloat(), anyFloat(), same(paint));
            order.verify(canvas).restore();
        }
    }

    public static class GivenStepperSpyWithTwoStepsAndStubbedLayoutActiveViewMethod
            extends GivenStepperSpyWithTwoSteps {
        private Rect rect;

        @Before
        public void givenStepperSpyWithTwoStepsAndStubbedLayoutActiveViewMethod() {
            rect = mock(Rect.class);
            doNothing().when(stepperSpy).layoutActiveView(same(rect), any(View.class));
        }

        @Test
        public void layoutInnerView_ShouldCallLayoutActiveViewWithInnerView() {
            stepperSpy.layoutInnerView(rect, mockedStep1.step);

            verify(stepperSpy).layoutActiveView(rect, mockedStep1.innerView);
        }

        @Test
        public void layoutNavButtons_ShouldCallLayoutActiveViewWithContinueButton() {
            stepperSpy.layoutNavButtons(rect, mockedStep1.step);

            verify(stepperSpy).layoutActiveView(rect, mockedStep1.continueButton);
        }
    }

    public static abstract class GivenStepperSpyWithTwoStepsAndStubbedLayoutMethods
            extends GivenStepperSpyWithTwoSteps {
        static class CaptureRectAnswer implements Answer<Void> {
            private final Rect rectToCaptureArg;

            CaptureRectAnswer(Rect rectToCaptureArg) {
                this.rectToCaptureArg = rectToCaptureArg;
            }

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = invocation.getArgument(0);
                rectToCaptureArg.set(rect);
                return null;
            }
        }

        @Before
        public void givenStepperSpyWithTwoStepsAndStubbedLayoutMethods() {
            doNothing().when(stepperSpy).layoutTouchView(any(Rect.class), any(VerticalStepper.InternalTouchView.class));
            doNothing().when(stepperSpy).layoutInnerView(any(Rect.class), any(Step.class));
            doNothing().when(stepperSpy).layoutNavButtons(any(Rect.class), any(Step.class));

            when(mockedStep1.step.getTempRectForLayout()).thenReturn(new Rect());
        }
    }

    @SuppressLint("WrongCall") // Explicitly testing onLayout
    public static class GivenStepperSpyWithTwoInactiveStepsAndStubbedLayoutMethods
            extends GivenStepperSpyWithTwoStepsAndStubbedLayoutMethods {
        @Before
        public void givenStepperSpyWithTwoInactiveStepsAndStubbedLayoutMethods() {
            when(mockedStep1.step.isActive()).thenReturn(false);
            when(mockedStep2.step.isActive()).thenReturn(false);
        }

        @Test
        public void onLayout_ShouldNotCallLayoutInnerViewOrLayoutNavButtons() {
            stepperSpy.onLayout(true, 0, 0, 0, 0);

            verify(stepperSpy, times(2)).layoutTouchView(any(Rect.class), any(VerticalStepper.InternalTouchView.class));

            verify(stepperSpy, never()).layoutInnerView(any(Rect.class), any(Step.class));
            verify(stepperSpy, never()).layoutNavButtons(any(Rect.class), any(Step.class));
        }

        @Test
        public void onLayout_ShouldAdjustTouchForPadding() {
            int leftPadding = 8;
            int topPadding = 20;
            int rightPadding = 4;
            int bottomPadding = 10;
            stepperSpy.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);

            int left = 0;
            int top = 0;
            int right = 400;
            int bottom = 200;

            stepperSpy.onLayout(true, left, top, right, bottom);

            ArgumentCaptor<Rect> rectCaptor = ArgumentCaptor.forClass(Rect.class);
            verify(stepperSpy).layoutTouchView(rectCaptor.capture(), same(mockedStep1.touchView));
            Rect touchRect = rectCaptor.getValue();

            assertThat(touchRect.left).isEqualTo(stepperSpy.outerHorizontalPadding + leftPadding);
            assertThat(touchRect.top).isEqualTo(stepperSpy.outerVerticalPadding + topPadding);
            assertThat(touchRect.right).isEqualTo(right - stepperSpy.outerHorizontalPadding - rightPadding);
            assertThat(touchRect.bottom).isEqualTo(bottom - stepperSpy.outerVerticalPadding - bottomPadding);
        }

        @Test
        public void onLayout_ShouldAdjustNextTopForPreviousStepHeight() {
            InOrder order = inOrder(stepperSpy);
            int distanceToNextStep = 400;
            when(mockedStep1.step.calculateYDistanceToNextStep()).thenReturn(distanceToNextStep);

            final Rect firstRect = new Rect();
            final Rect secondRect = new Rect();
            doAnswer(new CaptureRectAnswer(firstRect))
                    .when(stepperSpy).layoutTouchView(any(Rect.class), same(mockedStep1.touchView));
            doAnswer(new CaptureRectAnswer(secondRect))
                    .when(stepperSpy).layoutTouchView(any(Rect.class), same(mockedStep2.touchView));

            stepperSpy.onLayout(true, 0, 0, 0, 0);

            order.verify(stepperSpy).layoutTouchView(any(Rect.class), same(mockedStep1.touchView));
            order.verify(stepperSpy).layoutTouchView(any(Rect.class), same(mockedStep2.touchView));

            int firstStepTop = firstRect.top;
            int secondStepTop = secondRect.top;
            assertThat(secondStepTop).isEqualTo(firstStepTop + distanceToNextStep);
        }

        @Test
        public void onLayout_NonZeroLeft_ShouldAdjustForLeftOffset() {
            int left = 50;
            int right = 300;

            final Rect touchRect = new Rect();
            doAnswer(new CaptureRectAnswer(touchRect))
                    .when(stepperSpy).layoutTouchView(any(Rect.class), same(mockedStep1.touchView));

            stepperSpy.onLayout(true, left, 0, right, 0);

            verify(stepperSpy).layoutTouchView(any(Rect.class), same(mockedStep1.touchView));

            assertThat(touchRect.left).isEqualTo(stepperSpy.outerHorizontalPadding);
            assertThat(touchRect.right).isEqualTo(right - left - stepperSpy.outerHorizontalPadding);
        }

        @Test
        public void onLayout_NonZeroTop_ShouldAdjustForTopOffset() {
            int top = 50;
            int bottom = 300;

            final Rect touchRect = new Rect();
            doAnswer(new CaptureRectAnswer(touchRect))
                    .when(stepperSpy).layoutTouchView(any(Rect.class), same(mockedStep1.touchView));

            stepperSpy.onLayout(true, 0, top, 0, bottom);

            verify(stepperSpy).layoutTouchView(any(Rect.class), same(mockedStep1.touchView));

            assertThat(touchRect.top).isEqualTo(stepperSpy.outerVerticalPadding);
            assertThat(touchRect.bottom).isEqualTo(bottom - top - stepperSpy.outerVerticalPadding);
        }
    }

    @SuppressLint("WrongCall") // Explicitly testing onLayout
    public static class GivenStepperSpyWithTwoStepsOneActiveAndStubbedLayoutMethods
            extends GivenStepperSpyWithTwoStepsAndStubbedLayoutMethods {
        @Before
        public void givenStepperSpyWithTwoStepsOneActiveAndStubbedLayoutMethods() {
            when(mockedStep1.step.isActive()).thenReturn(true);
            when(mockedStep2.step.isActive()).thenReturn(false);
        }

        @Test
        public void onLayout_ShouldCallLayoutInnerViewAndLayoutNavButtons() {
            stepperSpy.onLayout(true, 0, 0, 0, 0);

            verify(stepperSpy, times(2)).layoutTouchView(any(Rect.class), any(VerticalStepper.InternalTouchView.class));

            verify(stepperSpy).layoutInnerView(any(Rect.class), any(Step.class));
            verify(stepperSpy).layoutNavButtons(any(Rect.class), any(Step.class));
        }

        @Test
        public void onLayout_ShouldAdjustInnerViewForPaddingAndStepDecorators() {
            int distanceToTextBottom = 80;
            when(mockedStep1.step.calculateYDistanceToTextBottom()).thenReturn(distanceToTextBottom);
            int iconWidth = 40;
            when(mockedStep1.step.calculateStepDecoratorIconWidth()).thenReturn(iconWidth);

            int leftPadding = 8;
            int topPadding = 20;
            int rightPadding = 4;
            int bottomPadding = 10;
            stepperSpy.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);

            final Rect innerRect = new Rect();
            doAnswer(new CaptureRectAnswer(innerRect))
                    .when(stepperSpy).layoutInnerView(any(Rect.class), same(mockedStep1.step));

            int left = 0;
            int top = 0;
            int right = 400;
            int bottom = 200;

            stepperSpy.onLayout(true, left, top, right, bottom);

            verify(stepperSpy).layoutInnerView(any(Rect.class), same(mockedStep1.step));

            assertThat(innerRect.left)
                    .isEqualTo(leftPadding + stepperSpy.outerHorizontalPadding + iconWidth);
            assertThat(innerRect.top)
                    .isEqualTo(stepperSpy.outerVerticalPadding + topPadding + distanceToTextBottom);
            assertThat(innerRect.right)
                    .isEqualTo(right - left - stepperSpy.outerHorizontalPadding - rightPadding);
            assertThat(innerRect.bottom)
                    .isEqualTo(bottom - top - stepperSpy.outerVerticalPadding - bottomPadding);
        }

        @Test
        public void onLayout_ShouldAdjustButtonsTopForInnerViewHeight() {
            InOrder order = inOrder(stepperSpy);
            when(mockedStep1.step.isActive()).thenReturn(true);
            int innerHeight = 400;
            when(mockedStep1.innerView.getHeight()).thenReturn(innerHeight);

            final Rect innerRect = new Rect();
            final Rect navRect = new Rect();
            doAnswer(new CaptureRectAnswer(innerRect))
                    .when(stepperSpy).layoutInnerView(any(Rect.class), same(mockedStep1.step));
            doAnswer(new CaptureRectAnswer(navRect))
                    .when(stepperSpy).layoutNavButtons(any(Rect.class), same(mockedStep1.step));

            stepperSpy.onLayout(true, 0, 0, 0, 0);

            order.verify(stepperSpy).layoutInnerView(any(Rect.class), same(mockedStep1.step));
            order.verify(stepperSpy).layoutNavButtons(any(Rect.class), same(mockedStep1.step));

            int innerTop = innerRect.top;
            int buttonsTop = navRect.top;

            assertThat(buttonsTop).isEqualTo(innerTop + innerHeight);
        }
    }
}
