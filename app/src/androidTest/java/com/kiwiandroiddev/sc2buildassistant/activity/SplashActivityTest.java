package com.kiwiandroiddev.sc2buildassistant.activity;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.kiwiandroiddev.sc2buildassistant.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void splashActivityTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction mDButton = onView(
                allOf(withId(R.id.md_buttonDefaultPositive), withText("OK"),
                        withParent(allOf(withId(R.id.md_root),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        mDButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatSpinner = onView(
                allOf(withId(R.id.toolbar_expansion_spinner),
                        withParent(withId(R.id.toolbar)),
                        isDisplayed()));
        appCompatSpinner.perform(click());

        ViewInteraction linearLayout = onView(
                allOf(withClassName(is("android.widget.LinearLayout")), isDisplayed()));
        linearLayout.perform(click());

        ViewInteraction appCompatTextView = onView(
                allOf(withText("Протосс"), isDisplayed()));
        appCompatTextView.perform(click());

        ViewInteraction viewPager = onView(
                allOf(withId(R.id.pager),
                        withParent(allOf(withId(R.id.activity_main_root_view),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        viewPager.perform(swipeLeft());

        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.build_list),
                        withParent(allOf(withId(R.id.pager),
                                withParent(withId(R.id.activity_main_root_view)))),
                        isDisplayed()));
        recyclerView.perform(actionOnItemAtPosition(0, click()));

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(3549901);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.activity_brief_play_action_button), isDisplayed()));
        floatingActionButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(3592050);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.playPauseButton),
                        withParent(withId(R.id.buttonWrapper)),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withId(R.id.stopButton),
                        withParent(withId(R.id.buttonWrapper)),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

    }

}
