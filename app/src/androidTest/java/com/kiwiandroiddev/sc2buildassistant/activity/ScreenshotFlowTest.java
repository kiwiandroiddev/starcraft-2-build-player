package com.kiwiandroiddev.sc2buildassistant.activity;


import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.kiwiandroiddev.sc2buildassistant.R;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

/**
 * TODO: apply Page Object pattern
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ScreenshotFlowTest {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @ClassRule public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Test
    public void mainActivityTest() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        trySleep(2000);

        dismissChangelogIfNeeded();

        Screengrab.screenshot("main");

        openExpansionSpinner();

        Screengrab.screenshot("mainExpansions");

        selectLotvExpansionFromSpinner();

        trySleep(500);

        clickProtossText();

        trySleep(500);

        // should be 1 Gate FE
        clickOnBuildListItem(0);

        trySleep(1000);

        Screengrab.screenshot("brief");

        selectPlayFAB();

        trySleep(1000);

        selectPlayPauseControl();

        trySleep(6000);

        Screengrab.screenshot("player");

        selectSettingsButton();

        trySleep(500);

        Screengrab.screenshot("settings");

        pressBack();
        pressBack();
        pressBack();

        selectNewBuildFromOverflow();

        trySleep(500);

        closeSoftKeyboard();

        openFactionSpinner();
        trySleep(500);

        Screengrab.screenshot("buildInfo");

        clickProtossText();

        selectEditItemsTab();

        selectNewBuildItemButton();

        swipeLeftInPager();
        swipeLeftInPager();
        trySleep(500);

        Screengrab.screenshot("unitSelection");

        pressBack();

        selectEditInfoTab();

        openFactionSpinner();

        clickTerranText();

        selectEditItemsTab();

        selectNewBuildItemButton();
        swipeLeftInPager();
        selectReactor();

        enterMinutes("3");
        enterSeconds("40");

        selectTargetUnitButton();
        swipeLeftInPager();
        selectBarracks();

        Screengrab.screenshot("itemEditor");
    }

    private void selectBarracks() {
        onView(withContentDescription(R.string.gameitem_barracks)).perform(click());
    }

    private void selectTargetUnitButton() {
        onView(withId(R.id.dlg_target_button)).perform(click());
    }

    private void enterSeconds(String seconds) {
        onView(withId(R.id.dlg_seconds)).perform(typeText(seconds));
    }

    private void enterMinutes(String minutes) {
        onView(withId(R.id.dlg_minutes)).perform(typeText(minutes));
    }

    private void selectReactor() {
        onView(withContentDescription(R.string.gameitem_reactor)).perform(click());
    }

    private void clickTerranText() {
        onView(withText(R.string.race_terran)).perform(click());
    }

    private void selectEditInfoTab() {
        onView(withText(R.string.edit_build_info_title)).perform(click());
    }

    private void swipeLeftInPager() {
        onView(withId(R.id.pager)).perform(swipeLeft());
    }

    private void selectNewBuildItemButton() {
        onView(withId(R.id.edit_build_activity_add_button)).perform(click());
    }

    private void selectEditItemsTab() {
        onView(withText(R.string.edit_build_items_title)).perform(click());
    }

    private void openFactionSpinner() {
        onView(withId(R.id.edit_faction_spinner)).perform(click());
    }

    private void selectNewBuildFromOverflow() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

        onView(withText(R.string.menu_new_build)).perform(click());
    }

    private void selectSettingsButton() {
        onView(withId(R.id.menu_settings)).perform(click());
    }

    private void selectPlayPauseControl() {
        onView(withId(R.id.playPauseButton)).perform(click());
    }

    private void selectPlayFAB() {
        onView(withId(R.id.activity_brief_play_action_button)).perform(click());
    }

    private void clickProtossText() {
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
