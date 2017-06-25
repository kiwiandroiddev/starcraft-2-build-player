package com.kiwiandroiddev.sc2buildassistant.feature.brief.view

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefCachingProxy
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView
import javax.inject.Inject

/**
 * Created by Matt Clarke on 20/06/17.
 */
class BriefViewModel(app: Application) : AndroidViewModel(app), BriefPresenter {

    @Inject lateinit var _backingPresenter: BriefPresenter

    private var briefCachingProxy: BriefCachingProxy? = null

    init {
        (app as MyApplication).inject(this)
    }

    fun setBuildId(buildId: Long) {
        if (briefCachingProxy == null) {
            briefCachingProxy = BriefCachingProxy(buildId).apply {
                _backingPresenter.attachView(this)
            }
        } else if (buildId != briefCachingProxy!!.getBuildId()) {
            throw IllegalStateException("BriefCachingProxy already initialised with different build ID")
        }
    }

    override fun attachView(view: BriefView) {
        briefCachingProxy?.attachView(view)
    }

    override fun detachView() {
        briefCachingProxy?.detachView()
    }

    override fun onCleared() {
        _backingPresenter.detachView()
    }

}