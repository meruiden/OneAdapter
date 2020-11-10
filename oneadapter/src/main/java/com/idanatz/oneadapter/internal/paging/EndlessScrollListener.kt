package com.idanatz.oneadapter.internal.paging

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.idanatz.oneadapter.internal.utils.Logger
import com.idanatz.oneadapter.internal.utils.extensions.findLastVisibleItemPosition

internal class EndlessScrollListener (
        private val layoutManager: RecyclerView.LayoutManager,
        private var visibleThreshold: Int = 0, // The minimum amount of items to have below your current scroll position before loading more.
        private val includeEmptyState: Boolean,
        private val loadMoreObserver: LoadMoreObserver
) : RecyclerView.OnScrollListener() {

    private var currentPage = 0 // The current offset index of data you have loaded
    private var previousTotalItemCount = 0 // The total number of items in the data set after the last load
    private var loading = false // True if we are still waiting for the last set of data to load.
    private val startingPageIndex = 0 // Sets the starting page index

    init {
        when (layoutManager) {
            is GridLayoutManager -> visibleThreshold *= layoutManager.spanCount
            is StaggeredGridLayoutManager -> visibleThreshold *= layoutManager.spanCount
        }
        resetState()
    }

    // This happens many times a second during a scroll, so be wary of the code you place here.
    override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
        if (!isUserScrolled(view)) {
            return
        }

        when (evaluateLoadingState()) {
            LoadingState.FinishLoading -> {
                loading = false
                loadMoreObserver.onLoadingStateChanged(loading)
            }
            LoadingState.LoadingStarted -> {
                loading = true
                loadMoreObserver.onLoadingStateChanged(loading)
                currentPage++
                loadMoreObserver.onLoadMore(currentPage)
            }
            else -> {}
        }

        // update the previous item count to the current item count
        this.previousTotalItemCount = layoutManager.itemCount
    }

    fun resetState() {
        currentPage = startingPageIndex
        loading = false
        previousTotalItemCount = if (includeEmptyState) 1 else 0
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putInt(STATE_PAGE, currentPage)
            putInt(STATE_COUNT, previousTotalItemCount)
        }
    }

    fun onRestoreInstanceState(savedState: Bundle) {
        currentPage = savedState.getInt(STATE_PAGE, currentPage)
        previousTotalItemCount = savedState.getInt(STATE_COUNT, previousTotalItemCount)
    }

    private fun isUserScrolled(view: RecyclerView) = view.scrollState != RecyclerView.SCROLL_STATE_IDLE

    private fun evaluateLoadingState(): LoadingState {
        // inner functions
        fun shouldStartLoading(lastVisibleItemPosition: Int, totalItemCount: Int) = !loading && lastVisibleItemPosition + visibleThreshold > totalItemCount
        fun isLoadingFinished(totalItemCount: Int) = loading && totalItemCount > (previousTotalItemCount + 1) // + 1 for the loading holder

        val totalItemCount = layoutManager.itemCount
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        return when {
            isLoadingFinished(totalItemCount) -> LoadingState.FinishLoading
            shouldStartLoading(lastVisibleItemPosition, totalItemCount) -> LoadingState.LoadingStarted
            loading -> LoadingState.MidLoading
            else -> LoadingState.Normal
        }.also {
            if (it != LoadingState.Normal) Logger.logd { "onScrolled -> loading state: $it" }
        }
    }

    enum class LoadingState {
        Normal, LoadingStarted, MidLoading, FinishLoading
    }

    companion object {
        const val STATE_PAGE = "ES_STATE_PAGE"
        const val STATE_COUNT = "ES_STATE_COUNT"
    }
}