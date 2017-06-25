package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewEvent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewState
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

/**
 * Created by Matt Clarke on 25/06/17.
 */
class BriefCachingProxy(backingPresenter: BriefPresenter) : BriefPresenter, BriefView {

    private val viewStateBehaviorRelay = BehaviorRelay.create<BriefViewState>()
    private val viewEventPublishRelay = PublishRelay.create<BriefViewEvent>()
    private var viewStateDisposable: Disposable? = null
    private var viewEventDisposable: Disposable? = null
    private var buildId: Long? = null

    init { backingPresenter.attachView(this) }

    override fun attachView(view: BriefView) {
        viewStateDisposable = viewStateBehaviorRelay.subscribe { viewState -> view.render(viewState) }
        viewEventDisposable = view.getViewEvents().subscribe(viewEventPublishRelay)

        if (buildId == null) {
            buildId = view.getBuildId()
        } else if (view.getBuildId() != buildId) {
            throw IllegalStateException("View previously attached with different build ID")
        }
    }

    override fun getBuildId(): Long =
            buildId ?: throw IllegalStateException("No view attached to proxy build ID")

    override fun getViewEvents(): Observable<BriefViewEvent> = viewEventPublishRelay

    override fun render(viewState: BriefViewState) {
        viewStateBehaviorRelay.accept(viewState)
    }

    override fun detachView() {
        viewStateDisposable?.apply { if (!isDisposed) dispose() }
        viewStateDisposable = null

        viewEventDisposable?.apply { if (!isDisposed) dispose() }
        viewEventDisposable = null
    }

}