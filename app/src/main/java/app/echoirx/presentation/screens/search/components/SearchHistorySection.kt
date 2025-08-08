package app.echoirx.presentation.screens.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.echoirx.R
import app.echoirx.domain.model.SearchHistoryItem
import app.echoirx.presentation.components.EmptyStateMessage
import app.echoirx.presentation.components.preferences.PreferencePosition

@Composable
fun SearchHistorySection(
    searchHistory: List<SearchHistoryItem>,
    onHistoryItemClick: (SearchHistoryItem) -> Unit,
    onDeleteHistoryItem: (SearchHistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (searchHistory.isEmpty()) {
            EmptyStateMessage(
                title = stringResource(R.string.msg_no_search_history),
                description = stringResource(R.string.msg_no_search_history_desc),
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_search)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 16.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.title_recent_searches),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(
                    onClick = onClearHistory
                ) {
                    Text(
                        text = stringResource(R.string.action_clear_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(
                    items = searchHistory,
                    key = { index, item -> "search_history_${item.id}_$index" }
                ) { index, item ->
                    val position = when {
                        searchHistory.size == 1 -> PreferencePosition.Single
                        index == 0 -> PreferencePosition.Top
                        index == searchHistory.size - 1 -> PreferencePosition.Bottom
                        else -> PreferencePosition.Middle
                    }

                    SearchHistoryItem(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        item = item,
                        position = position,
                        onClick = { onHistoryItemClick(item) },
                        onDelete = { onDeleteHistoryItem(item) }
                    )
                }
            }
        }
    }
}