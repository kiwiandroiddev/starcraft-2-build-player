package com.kiwiandroiddev.sc2buildassistant.feature.brief.view

import android.app.Activity
import android.app.ActivityOptions
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.DrawableRes
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextSwitcher
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.analytics.tracking.android.EasyTracker
import com.google.analytics.tracking.android.MapBuilder
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.jakewharton.rxrelay2.PublishRelay
import com.kiwiandroiddev.sc2buildassistant.R
import com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.*
import com.kiwiandroiddev.sc2buildassistant.activity.OnScrollDirectionChangedListener
import com.kiwiandroiddev.sc2buildassistant.ads.AdLoader
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Expansion
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewEvent
import com.kiwiandroiddev.sc2buildassistant.feature.settings.data.sharedpreferences.SettingKeys.KEY_SHOW_STATUS_BAR
import com.kiwiandroiddev.sc2buildassistant.util.*
import com.kiwiandroiddev.sc2buildassistant.view.WindowInsetsCapturingView
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_brief.*
import timber.log.Timber
import java.util.*

/**
 * Screen for showing an explanation of the build order, including references etc.
 * From here users can play the build order by pressing the Play action item.
 */
class BriefActivity : AppCompatActivity(), BriefView, LifecycleRegistryOwner {

    companion object {

        val DEFAULT_INITIAL_VIEW_STATE = BriefView.BriefViewState(
                showAds = false,
                showLoadError = false,
                showTranslateOption = false,
                showTranslationError = false,
                translationLoading = false,
                showRevertTranslationOption = false,
                translationStatusMessage = null,
                briefText = null,
                buildSource = null,
                buildAuthor = null
        )

        private val KEY_VIEW_STATE = "com.kiwiandroiddev.sc2buildassistant.feature.brief.view.VIEW_STATE"

        private val sRaceBgMap = HashMap<Faction, Int>()
        private val sColumns = ArrayList<String>()

        init {
            sRaceBgMap.put(Faction.TERRAN, R.drawable.terran_icon_blur_drawable)
            sRaceBgMap.put(Faction.PROTOSS, R.drawable.protoss_icon_blur_drawable)
            sRaceBgMap.put(Faction.ZERG, R.drawable.zerg_icon_blur_drawable)

            // Columns from the build order table containing info we want to display
            sColumns.add(DbAdapter.KEY_SOURCE)
            sColumns.add(DbAdapter.KEY_DESCRIPTION)
            sColumns.add(DbAdapter.KEY_AUTHOR)
        }

        @DrawableRes
        fun getBackgroundDrawable(race: Faction): Int {
            return sRaceBgMap[race]!!
        }

        /**
         * Starts a new BriefActivity for the given build details.
         *
         *
         * Only the buildId is strictly needed, having other build fields passed in is a speed
         * optimization - saves the new BriefActivity from having to refetch these itself using the
         * build ID.
         */
        fun open(callingActivity: Activity,
                 buildId: Long,
                 faction: Faction,
                 expansion: Expansion,
                 buildName: String,
                 sharedBuildNameTextView: TextView) {
            val i = Intent(callingActivity, BriefActivity::class.java)
            i.putExtra(KEY_BUILD_ID, buildId)    // pass build order record ID

            // speed optimization - pass these so brief activity doesn't need to
            // requery them from the database and can display them instantly
            i.putExtra(KEY_FACTION_ENUM, faction)
            i.putExtra(KEY_EXPANSION_ENUM, expansion)
            i.putExtra(KEY_BUILD_NAME, buildName)

            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // create the transition animation - the views in the layouts
                // of both activities are defined with android:transitionName="buildName"
                val options = ActivityOptions
                        .makeSceneTransitionAnimation(callingActivity,
                                sharedBuildNameTextView, "buildName")

                callingActivity.startActivity(i, options.toBundle())
            } else {
                callingActivity.startActivity(i)
            }
        }
    }

    private var mBuildId: Long = 0
    private lateinit var mFaction: Faction
    private lateinit var mExpansion: Expansion
    private lateinit var mBuildName: String

    private var currentViewState = DEFAULT_INITIAL_VIEW_STATE

    @BindView(R.id.toolbar) lateinit var mToolbar: Toolbar
    @BindView(R.id.brief_source) lateinit var mSourceText: TextView
    @BindView(R.id.brief_source_layout) lateinit var mSourceLayout: View
    @BindView(R.id.brief_root) lateinit var mRootView: View
    @BindView(R.id.activity_brief_chrome_layer) lateinit var mChromeLayer: View
    @BindView(R.id.brief_build_notes_text_switcher) lateinit var mNotesTextSwitcher: TextSwitcher
    @BindView(R.id.brief_author_layout) lateinit var mAuthorLayout: View
    @BindView(R.id.brief_author) lateinit var mAuthorText: TextView
    @BindView(R.id.activity_brief_play_action_button) lateinit var mPlayButton: FloatingActionButton
    @BindView(R.id.ad_frame) lateinit var mAdFrame: ViewGroup
    @BindView(R.id.brief_window_insets_capturing_view) lateinit var mWindowInsetsCapturingView: WindowInsetsCapturingView
    @BindView(R.id.brief_content_layout) lateinit var mBriefContentLayout: ViewGroup
    @BindView(R.id.buildName) lateinit var mBuildNameText: TextView

    private lateinit var briefViewModel: BriefViewModel
    private val viewEventPublishRelay = PublishRelay.create<BriefViewEvent>()
    val lifecycleRegistry = LifecycleRegistry(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        initSystemUiVisibility()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_brief)
        ButterKnife.bind(this)

        initIntentParameterFields(savedInstanceState)
        restoreViewStateFieldIfExists(savedInstanceState)
        initToolbar()
        initNotesTextSwitcher()
        displayBasicInfo()
        trackBriefView()
        initScrollView()
        ensureBriefContentIsNotHiddenBySystemBars()

        briefViewModel = ViewModelProviders.of(this).get(BriefViewModel::class.java)
        briefViewModel.setBuildId(mBuildId)
        briefViewModel.attachView(this)
    }

    private fun initNotesTextSwitcher() {
        mNotesTextSwitcher.makeLinksClickableInAllChildTextViews()
    }

    private fun ViewGroup.makeLinksClickableInAllChildTextViews() {
        views.forEach { (it as TextView?)?.apply { movementMethod = LinkMovementMethod.getInstance() } }
    }

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry

    private fun restoreViewStateFieldIfExists(savedInstanceState: Bundle?) {
        savedInstanceState?.apply {
            getSerializable(KEY_VIEW_STATE)?.let { viewState ->
                currentViewState = viewState as BriefView.BriefViewState
                forceRenderViewState(currentViewState)
            }
        }
    }

    private fun initIntentParameterFields(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            mBuildId = intent.getLongExtra(KEY_BUILD_ID, 0)
            mFaction = intent.getSerializableExtra(KEY_FACTION_ENUM) as Faction
            mExpansion = intent.getSerializableExtra(KEY_EXPANSION_ENUM) as Expansion
            mBuildName = intent.getStringExtra(KEY_BUILD_NAME)
        } else {
            mBuildId = savedInstanceState.getLong(KEY_BUILD_ID, 0)
            mFaction = savedInstanceState.getSerializable(KEY_FACTION_ENUM) as Faction
            mExpansion = savedInstanceState.getSerializable(KEY_EXPANSION_ENUM) as Expansion
            mBuildName = savedInstanceState.getString(KEY_BUILD_NAME)
        }
    }

    private fun initSystemUiVisibility() {
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_SHOW_STATUS_BAR, false)) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        makeNavigationBarTranslucentIfPossible()
    }

    private fun initToolbar() {
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
    }

    private fun initScrollView() {
        val nestedScrollView = findViewById(R.id.brief_nested_scroll_view) as NestedScrollView
        nestedScrollView.setOnScrollChangeListener(object : OnScrollDirectionChangedListener() {
            override fun onStartScrollingDown() {
                onScrollDownBrief()
            }

            override fun onStartScrollingUp() {
                onScrollUpBrief()
            }
        })
    }

    private fun onScrollDownBrief() {
        mPlayButton.hide()
        hideToolbar()
        hideSystemNavigationBar()
    }

    private fun onScrollUpBrief() {
        mPlayButton.show()
        showToolbar()
        showSystemNavigationBar()
    }

    private fun hideToolbar() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.slide_and_fade_out_to_top)
        animation.setAnimationListener(object : NoOpAnimationListener() {
            override fun onAnimationEnd(animation: Animation) {
                mToolbar.visible = false
            }
        })
        mToolbar.startAnimation(animation)
    }

    private fun showToolbar() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.slide_and_fade_in_from_top)
        animation.setAnimationListener(object : NoOpAnimationListener() {
            override fun onAnimationStart(animation: Animation) {
                mToolbar.visible = true
            }
        })
        mToolbar.startAnimation(animation)
    }

    private fun hideSystemNavigationBar() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    private fun showSystemNavigationBar() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    private fun makeNavigationBarTranslucentIfPossible() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }

    private fun fadeInAdOnLoad(adView: AdView) {
        adView.alpha = 0.0f
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                val fadeDurationMs = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
                adView.animate()
                        .alpha(1.0f)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .duration = fadeDurationMs
            }
        }
    }

    private fun ensureBriefContentIsNotHiddenBySystemBars() {
        mWindowInsetsCapturingView.setOnCapturedWindowInsetsListener { insets ->
            mBriefContentLayout.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            mWindowInsetsCapturingView.clearOnCapturedWindowInsetsListener()
        }
    }

    public override fun onStart() {
        super.onStart()
        EasyTracker.getInstance(this).activityStart(this)
    }

    @OnClick(R.id.activity_brief_play_action_button)
    fun onPlayBuildSelected() {
        viewEventPublishRelay.accept(BriefViewEvent.PlaySelected())
    }

    override fun getBuildId(): Long = mBuildId

    override fun getViewEvents(): Observable<BriefViewEvent> = viewEventPublishRelay

    override fun render(viewState: BriefView.BriefViewState) {
        Timber.d("viewState = ${viewState.copy(briefText = viewState.briefText?.substring(0, 10))}")

        calculateAndApplyViewStateDiff(currentViewState, viewState)
        currentViewState = viewState
    }

    fun forceRenderViewState(viewState: BriefView.BriefViewState) {
        calculateAndApplyViewStateDiff(oldViewState = DEFAULT_INITIAL_VIEW_STATE, newViewState = viewState)
    }

    private fun calculateAndApplyViewStateDiff(oldViewState: BriefView.BriefViewState,
                                               newViewState: BriefView.BriefViewState) {
        if (oldViewState.showAds != newViewState.showAds) {
            when (newViewState.showAds) {
                true -> showAdBanner()
                false -> hideAdBanner()
            }
        }

        if (oldViewState.briefText != newViewState.briefText) {
            newViewState.briefText?.let { briefText -> setNotes(briefText) }
        }

        setAuthor(newViewState.buildAuthor)
        setSource(newViewState.buildSource)

        if (oldViewState.showTranslateOption != newViewState.showTranslateOption) {
            if (newViewState.showTranslateOption) {
                showTranslationAvailableOption(newViewState.translationStatusMessage)
            }
        }

        if (oldViewState.translationLoading != newViewState.translationLoading) {
            when (newViewState.translationLoading) {
                true -> showTranslationLoading()
                false -> hideTranslationLoading()
            }
        }

        if (oldViewState.showRevertTranslationOption != newViewState.showRevertTranslationOption) {
            if (newViewState.showRevertTranslationOption) {
                showRevertTranslationOption(newViewState.translationStatusMessage)
            }
        }

        with(newViewState) {
            if (noneAreTrue(showTranslateOption, showRevertTranslationOption, translationLoading)) {
                hideTranslationBar()
            }
        }

        if (!oldViewState.showTranslationError && newViewState.showTranslationError) {
            Toast.makeText(this, getString(R.string.brief_translation_error_message), Toast.LENGTH_LONG).show()

            if (newViewState.showTranslateOption) {
                showTranslationAvailableOption(newViewState.translationStatusMessage)
            }
        }
    }

    private fun hideTranslationBar() {
        brief_translation_bar.visible = false
    }

    private fun showRevertTranslationOption(translationStatusMessage: String?) {
        brief_translation_bar.visible = true
        brief_translation_bar_text.text = translationStatusMessage ?: ""

        brief_translation_bar_button.visible = true
        brief_translation_bar_button.text = getString(R.string.brief_revert_translation_button_text)
        brief_translation_bar_button.setOnClickListener {
            viewEventPublishRelay.accept(BriefViewEvent.RevertTranslationSelected())
        }
    }

    private fun showTranslationAvailableOption(promptMessage: String?) {
        brief_translation_bar.visible = true
        brief_translation_bar_text.text = promptMessage ?: ""

        brief_translation_bar_button.visible = true
        brief_translation_bar_button.text = getString(R.string.translate_button)
        brief_translation_bar_button.setOnClickListener {
            viewEventPublishRelay.accept(BriefViewEvent.TranslateSelected())
        }
    }

    private fun showTranslationLoading() {
        brief_translation_bar.visible = true
        brief_translation_bar_text.text = getString(R.string.brief_translation_loading)

        fadeOutTranslateButton()
        fadeInTranslationSpinner()
    }

    private fun hideTranslationLoading() {
        fadeInTranslateButton()
        fadeOutTranslationSpinner()
    }

    private fun fadeInTranslateButton() {
        brief_translation_bar_button.startFadeInAnimation()
    }

    private fun fadeOutTranslationSpinner() {
        brief_translation_bar_loading_spinner.startFadeOutAnimation()
    }

    private fun fadeOutTranslateButton() {
        brief_translation_bar_button.startFadeOutAnimation()
    }

    private fun fadeInTranslationSpinner() {
        brief_translation_bar_loading_spinner.startFadeInAnimation()
    }

    private fun setAuthor(author: String?) {
        if (!author.isNullOrBlank()) {
            mAuthorLayout.visible = true
            mAuthorText.text = author
        } else {
            mAuthorLayout.visible = false
        }
    }

    private fun setSource(source: String?) {
        if (!source.isNullOrBlank()) {
            mSourceLayout.visible = true
            mSourceText.text = Html.fromHtml(source)
            mSourceText.movementMethod = LinkMovementMethod.getInstance()
        } else {
            mSourceLayout.visible = false
        }
    }

    private fun setNotes(notes: String?) {
        mNotesTextSwitcher.setText(Html.fromHtml(notes))
    }

    private fun showAdBanner() {
        mAdFrame.visible = true

        if (mAdFrame.childCount == 0) {
            val adView = AdView(this)
            mAdFrame.addView(adView)
            AdLoader.loadAdForRealUsers(adView)
            fadeInAdOnLoad(adView)
        }
    }

    private fun hideAdBanner() {
        mAdFrame.visible = false
    }

    public override fun onStop() {
        super.onStop()
        EasyTracker.getInstance(this).activityStop(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.brief_menu, menu)        // add the "play build" action bar item
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    finishCompat()
                    true
                }
                R.id.menu_edit_build -> {
                    viewEventPublishRelay.accept(BriefViewEvent.EditSelected())
                    true
                }
                R.id.menu_settings -> {
                    viewEventPublishRelay.accept(BriefViewEvent.SettingsSelected())
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onBackPressed() {
        finishCompat()
    }

    private fun finishCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition()
        } else {
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_BUILD_ID, mBuildId)
        outState.putString(KEY_BUILD_NAME, mBuildName)
        outState.putSerializable(KEY_FACTION_ENUM, mFaction)
        outState.putSerializable(KEY_EXPANSION_ENUM, mExpansion)
        outState.putSerializable(KEY_VIEW_STATE, currentViewState)
    }

    /**
     * Immediately displays title, faction and expansion info. These data are sent from the
     * calling activity and don't need to be queried from the database
     */
    private fun displayBasicInfo() {
        mBuildNameText.text = mBuildName
        mRootView.setBackgroundDrawable(resources.getDrawable(getBackgroundDrawable(mFaction)))
    }

    /**
     * Send info about this brief view to Google Analytics as it's of interest
     * which builds are being viewed and which aren't
     */
    private fun trackBriefView() {
        EasyTracker.getInstance(this).send(
                MapBuilder.createEvent(
                        "brief_view", mExpansion.toString() + "_" + mFaction.toString(), mBuildName, null)
                        .build())
    }

    override fun onDestroy() {
        briefViewModel.detachView()
        super.onDestroy()
    }
}
