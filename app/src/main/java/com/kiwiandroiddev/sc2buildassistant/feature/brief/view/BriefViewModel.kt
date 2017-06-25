package com.kiwiandroiddev.sc2buildassistant.feature.brief.view

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by Matt Clarke on 20/06/17.
 */
class BriefViewModel(app: Application) : AndroidViewModel(app), BriefView, BriefPresenter {

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

    private var attachedView: BriefView? = null

    init {
        (app as MyApplication).inject(this)
    }

    override fun attachView(view: BriefView) {
        attachedView = view
    }

    override fun getBuildId(): Long {
        return attachedView?.getBuildId() ?: throw IllegalStateException()
    }

    override fun detachView() {
        attachedView = null
    }

    override fun getViewEvents(): Observable<BriefView.BriefViewEvent> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun render(viewState: BriefView.BriefViewState) {

    }

    override fun onCleared() {
        presenter.detachView()
    }

}