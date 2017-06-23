package com.kiwiandroiddev.sc2buildassistant.feature.brief.view

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by Matt Clarke on 20/06/17.
 */
class BriefViewModel(app: Application) : AndroidViewModel(app), BriefView {

    companion object {

        val DEFAULT_VIEW_STATE = BriefView.BriefViewState(
                showAds = false,
                showLoadError = false,
                briefText = null,
                buildSource = null,
                buildAuthor = null
        )
    }

    @Inject lateinit var presenter: BriefPresenter

    private var buildId: Long? = null
    private var viewState =
            MutableLiveData<BriefView.BriefViewState>().apply { setValue(DEFAULT_VIEW_STATE) }

    init {
        (app as MyApplication).inject(this)
    }

    fun setBuildId(buildId: Long) {
        if (this.buildId == null) {
            presenter.attachView(this, buildId)
            this.buildId = buildId
        } else if (this.buildId != buildId) {
            throw IllegalArgumentException("Programming error: setBuildId called again for same ViewModel with different buildID")
        }
    }

    fun getViewState(): LiveData<BriefView.BriefViewState> = viewState

    override fun getViewEvents(): Observable<BriefView.BriefViewEvent> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun render(viewState: BriefView.BriefViewState) {
        this.viewState.value = viewState
    }

    override fun onCleared() {
        presenter.detachView()
    }
}