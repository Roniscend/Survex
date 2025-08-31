package com.example.survey.ui

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.survey.data.Session
import com.example.survey.viewmodel.CameraViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onSessionClick: (String) -> Unit
) {
    val context = LocalContext.current
    // FIXED: Direct access to mutableStateListOf - no collectAsState needed
    val allSessions = viewModel.allSessions
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val filteredSessions = remember(searchQuery, allSessions) {
        if (searchQuery.isEmpty()) {
            allSessions
        } else {
            allSessions.filter { sessionItem ->
                sessionItem.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // FIXED: Use Pictures/SurveyApp directory to match CameraController
    val sessionsWithCounts = remember(filteredSessions) {
        filteredSessions.map { session ->
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val sessionDir = File(picturesDir, "SurveyApp${File.separator}${session.name}")

            val files = if (sessionDir.exists()) {
                sessionDir.listFiles { file ->
                    val extension = file.extension.lowercase()
                    extension in listOf("jpg", "jpeg", "png", "mp4", "avi", "mov", "mkv")
                } ?: arrayOf()
            } else {
                arrayOf()
            }

            val photoCount = files.count { file ->
                val extension = file.extension.lowercase()
                extension in listOf("jpg", "jpeg", "png")
            }

            val videoCount = files.count { file ->
                val extension = file.extension.lowercase()
                extension in listOf("mp4", "avi", "mov", "mkv")
            }

            SessionWithCounts(session, photoCount, videoCount, files.size)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar with Search
        TopAppBar(
            title = { Text("Search Sessions") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Fixed SearchBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    isSearchActive = false
                },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by session name...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                } else null
            ) {
                // SearchBar content when expanded
                if (sessionsWithCounts.isNotEmpty()) {
                    LazyColumn {
                        items(sessionsWithCounts) { sessionWithCount ->
                            ListItem(
                                headlineContent = { Text(sessionWithCount.session.name) },
                                supportingContent = {
                                    Text("${sessionWithCount.totalFiles} files")
                                },
                                leadingContent = {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                },
                                modifier = Modifier.clickable {
                                    onSessionClick(sessionWithCount.session.name)
                                    isSearchActive = false
                                }
                            )
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No sessions found for '$searchQuery'")
                    }
                }
            }
        }

        // Results display when SearchBar is not active
        if (!isSearchActive) {
            if (sessionsWithCounts.isEmpty() && searchQuery.isNotEmpty()) {
                // No Results State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No sessions found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Try a different search term",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                // Search Results - REMOVED: Duplicate SessionCard usage
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "${sessionsWithCounts.size} result${if (sessionsWithCounts.size != 1) "s" else ""} found",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sessionsWithCounts) { sessionWithCount ->
                            // FIXED: Use simplified card to avoid conflicts
                            SearchResultCard(
                                sessionWithCount = sessionWithCount,
                                onClick = { onSessionClick(sessionWithCount.session.name) }
                            )
                        }
                    }
                }
            } else {
                // Show all sessions when no search query
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Search your sessions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Start typing to find specific sessions",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// FIXED: Renamed to avoid conflicts with HomeScreen's SessionCard
@Composable
fun SearchResultCard(
    sessionWithCount: SessionWithCounts,
    onClick: () -> Unit
) {
    val session = sessionWithCount.session

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open Session",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show file counts from actual filesystem
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${sessionWithCount.photoCount} photos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${sessionWithCount.videoCount} videos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(session.createdAt)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
