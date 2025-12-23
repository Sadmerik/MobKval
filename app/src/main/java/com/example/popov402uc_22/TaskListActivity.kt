package com.example.popov402uc_22

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.example.popov402uc_22.ui.theme.Popov402UC22Theme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class TaskListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Popov402UC22Theme {
                TaskListScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("task_list_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }

    val savedNotes = remember { sharedPreferences.getString("notes", null) }
    val notes = remember {
        try {
            val type = object : TypeToken<List<Note>>() {}.type
            mutableStateListOf<Note>().apply {
                savedNotes?.let { addAll(gson.fromJson(it, type)) }
            }
        } catch (e: Exception) {
            mutableStateListOf<Note>()
        }
    }

    val completedTasks = remember { mutableStateOf(sharedPreferences.getInt("completed_tasks", 0)) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var backgroundUri by remember { mutableStateOf(sharedPreferences.getString("background_uri", null)?.toUri()) }
    var isVideo by remember { mutableStateOf(sharedPreferences.getBoolean("is_video", false)) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(notes.size, completedTasks.value, backgroundUri, isVideo) {
        with(sharedPreferences.edit()) {
            putString("notes", gson.toJson(notes))
            putInt("completed_tasks", completedTasks.value)
            putString("background_uri", backgroundUri?.toString())
            putBoolean("is_video", isVideo)
            apply()
        }
    }

    val addNoteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val title = data?.getStringExtra("note_title") ?: ""
                val description = data?.getStringExtra("note_description") ?: ""
                val imageUriString = data?.getStringExtra("note_image_uri")
                val imageUri = imageUriString?.let { Uri.parse(it) }
                notes.add(
                    Note(
                        id = (notes.maxOfOrNull { it.id } ?: 0) + 1,
                        title = title,
                        description = description,
                        imageUri = imageUri
                    )
                )
            }
        }
    )

    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            }
            backgroundUri = uri
            isVideo = uri?.toString()?.contains("video") == true
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        backgroundUri?.let {
            if (isVideo) {
                VideoPlayer(uri = it)
            } else {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = { Text("Completed: ${completedTasks.value}", color = Color.White) },
                    actions = {
                        Clock()
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Background") },
                                onClick = { 
                                    backgroundLauncher.launch("*/*")
                                    showMenu = false 
                                }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    addNoteLauncher.launch(Intent(context, AddNoteActivity::class.java))
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
            ) {
                items(notes) { note ->
                    NoteItem(
                        note = note,
                        onNoteCompleted = {
                            notes.remove(note)
                            completedTasks.value++
                        },
                        onNoteLongPress = {
                            noteToDelete = note
                        }
                    )
                }
            }
        }

        noteToDelete?.let { note ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = { Text("Delete Note") },
                text = { Text("Are you sure you want to delete this note?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            notes.remove(note)
                            noteToDelete = null
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { noteToDelete = null }
                    ) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { 
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun Clock() {
    var time by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    Text(text = time, style = MaterialTheme.typography.titleMedium, color = Color.White)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteItem(note: Note, onNoteCompleted: () -> Unit, onNoteLongPress: () -> Unit) {
    val view = LocalView.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    onNoteLongPress()
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = note.isCompleted, onCheckedChange = { onNoteCompleted() })
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                note.imageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(text = note.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = note.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskListScreenPreview() {
    Popov402UC22Theme {
        TaskListScreen()
    }
}
