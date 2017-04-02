package com.kiwiandroiddev.sc2buildassistant.activity;


import android.support.test.espresso.NoMatchingViewException;
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
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void mainActivityTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        trySleep(2000);

        dismissChangelogIfNeeded();

        openExpansionSpinner();

        // take screenshot here...

        selectLotvExpansionFromSpinner();

        trySleep(1000);

        selectProtossTab();

        trySleep(1000);

        // should be 1 Gate FE
        clickOnBuildListItem(0);

        trySleep(1000);

        // take screenshot here...

        selectPlayFAB();

        trySleep(1000);

        selectPlayPauseControl();

        trySleep(6000);

        // take screenshot here of pylon overlay
    }

    private void selectPlayPauseControl() {
        onView(withId(R.id.playPauseButton)).perform(click());
    }

    private void selectPlayFAB() {
        onView(withId(R.id.activity_brief_play_action_button)).perform(click());
    }

    private void selectProtossTab() {
        onView(withText(R.string.race_protoss)).perform(click());
    }

    private void selectLotvExpansionFromSpinner() {
        onView(withText(R.string.expansion_lotv)).perform(click());
    }

    private void openExpansionSpinner() {
        ViewInteraction appCompatSpinner = onView(
                allOf(withId(R.id.toolbar_expansion_spinner),
                        withParent(withId(R.id.toolbar)),
                        isDisplayed()));
        appCompatSpinner.perform(click());
    }

    private void dismissChangelogIfNeeded() {
        try {
            ViewInteraction mDButton = onView(
                    allOf(withId(R.id.md_buttonDefaultPositive),
                            withParent(allOf(withId(R.id.md_root),
                                    withParent(withId(android.R.id.content)))),
                            isDisplayed()));
            mDButton.perform(click());
            trySleep(1000);
        } catch (NoMatchingViewException e) {
            // ignore
        }
    }

    private void clickOnBuildListItem(int position) {
        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.build_list),
                        withParent(allOf(withId(R.id.pager),
                                withParent(withId(R.id.activity_main_root_view)))),
                        isDisplayed()));
        recyclerView.perform(actionOnItemAtPosition(position, click()));
    }

    private void trySleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
