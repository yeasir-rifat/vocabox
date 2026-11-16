package com.vocabulary.app.data.repository

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.GroupOrder
import com.vocabulary.app.data.model.ListOptions
import com.vocabulary.app.data.model.WordOrder
import com.vocabulary.app.domain.WordGroup
import com.vocabulary.app.domain.WordGrouper
import com.vocabulary.app.domain.WordListFilter
import com.vocabulary.app.domain.initial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** The study groups of the current list settings, plus what the settings panel needs to show. */
data class GroupsState(
    val groups: List<WordGroup>,
    /** Words across all groups: the number on the "Show N words" button. */
    val wordCount: Int,
    /** Letters that have a word under every other setting, for the letter picker. */
    val availableLetters: Set<Char>
)

private data class GroupsSnapshot(
    val options: ListOptions,
    val readIds: Set<Long>,
    val favouriteIds: Set<Long>
)

/**
 * Groupwise: filter first, then cut the filtered words into groups of 20 (Group 1, 2, 3...).
 *
 * One shared copy serves both the group list and the group pages, so Group 3 is the same group
 * on both. Read and favourite state is taken when the settings change, not on every read, so
 * under Unread a group does not shrink and the groups do not shift while you study.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class WordGroupsSource @Inject constructor(
    repository: WordRepository,
    optionsStore: ListOptionsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val snapshots = optionsStore.options.mapLatest { options ->
        GroupsSnapshot(
            options = options,
            readIds = repository.observeReadIds().first().toSet(),
            favouriteIds = repository.observeBookmarkedIds().first().toSet()
        )
    }

    /** null while loading. */
    val state: StateFlow<GroupsState?> = combine(repository.observeWords(), snapshots) { words, snap ->
        build(words, snap)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(30_000), null)

    private fun build(words: List<WordEntity>, snap: GroupsSnapshot): GroupsState {
        val kept = WordListFilter.apply(words, snap.readIds, snap.favouriteIds, snap.options)
        val order = if (snap.options.order == WordOrder.RANDOM) GroupOrder.SHUFFLED else GroupOrder.ALPHABETICAL
        val letters = WordListFilter.apply(
            words,
            snap.readIds,
            snap.favouriteIds,
            snap.options.copy(letters = emptySet(), count = null)
        ).map { it.initial() }.toSet()
        return GroupsState(
            groups = WordGrouper.group(kept, order, snap.options.shuffleSeed),
            wordCount = kept.size,
            availableLetters = letters
        )
    }
}
