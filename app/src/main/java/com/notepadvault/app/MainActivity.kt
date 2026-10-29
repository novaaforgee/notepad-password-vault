package com.notepadvault.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivity : FragmentActivity() {

    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityManager = SecurityManager(this)

        setContent {
            // Private / HyperOS Inspired Palette
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFF6900), // Private Vibrant Orange Accent
                    secondary = Color(0xFF00C4B4),
                    background = Color(0xFF0D0D11), // Deep Charcoal
                    surface = Color(0xFF181820),     // Elevated Glassy Surface
                    surfaceVariant = Color(0xFF22222E),
                    onBackground = Color(0xFFF0F0F5),
                    onSurface = Color(0xFFF0F0F5)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MiuiNotesMainScreen(
                        activity = this,
                        securityManager = securityManager
                    )
                }
            }
        }
    }
}

enum class VaultScreenState {
    NOTES_HOME,
    SECRET_VAULT,
    NOTE_EDITOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiuiNotesMainScreen(
    activity: FragmentActivity,
    securityManager: SecurityManager
) {
    var screenState by remember { mutableStateOf(VaultScreenState.NOTES_HOME) }
    var notes by remember { mutableStateOf(securityManager.loadNotes()) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var editingInHiddenVault by remember { mutableStateOf(false) }
    var showPasswordModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Private Swipe Down threshold tracking
    var pullOffsetY by remember { mutableStateOf(0f) }
    val animatedPullY by animateFloatAsState(
        targetValue = pullOffsetY,
        animationSpec = spring(stiffness = 400f)
    )

    fun triggerVaultAuthentication() {
        if (!securityManager.isPasswordSet()) {
            showPasswordModal = true
            return
        }
        if (BiometricHelper.canAuthenticate(activity)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Private Private Vault",
                subtitle = "Touch fingerprint sensor or enter password",
                onSuccess = {
                    screenState = VaultScreenState.SECRET_VAULT
                },
                onError = {
                    showPasswordModal = true
                }
            )
        } else {
            showPasswordModal = true
        }
    }

    when (screenState) {
        VaultScreenState.NOTES_HOME -> {
            val normalNotes = notes.filter { !it.isHidden }
            val filteredList = if (searchQuery.isBlank()) normalNotes else normalNotes.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0D11))
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            val newDelta = pullOffsetY + delta
                            if (newDelta in 0f..380f) {
                                pullOffsetY = newDelta
                            }
                        },
                        onDragStopped = {
                            if (pullOffsetY > 160f) {
                                triggerVaultAuthentication()
                            }
                            pullOffsetY = 0f
                        }
                    )
            ) {
                // Private Hidden Pad Lock Header Revealed on Pull Down
                val pullProgress = (animatedPullY / 200f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 24.dp)
                        .alpha(pullProgress),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF262636),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (pullProgress >= 0.8f) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (pullProgress >= 0.8f) Color(0xFFFF6900) else Color(0xFFAAAAAA),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (pullProgress >= 0.8f) "Release to open Private Vault" else "Pull down further to unlock private notes",
                            color = Color(0xFFA0A0B0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Main Content View (Displaces down on swipe like Private Pad)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(0, animatedPullY.roundToInt()) }
                        .background(Color(0xFF0D0D11))
                        .padding(horizontal = 18.dp)
                ) {
                    Spacer(modifier = Modifier.height(44.dp))

                    // Private Pad Header Style
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Notes",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${normalNotes.size} notes stored",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Direct Fingerprint / Vault Lock Icon button
                            IconButton(
                                onClick = { triggerVaultAuthentication() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1E2A))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Vault Fingerprint",
                                    tint = Color(0xFFFF6900)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Private Rounded Search Pill
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search notes...", color = Color(0xFF757588)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF8E8E9F))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF181822),
                            unfocusedContainerColor = Color(0xFF181822),
                            focusedBorderColor = Color(0xFFFF6900).copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )

                    // Small hint banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF555566),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Swipe down to open Private Vault",
                            fontSize = 12.sp,
                            color = Color(0xFF6E6E82)
                        )
                    }

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = Color(0xFF333344)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) "No notes yet. Tap + to write!" else "No notes match your search.",
                                    color = Color.Gray,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        // Private Grid Layout (Adaptive for phones & tablets)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredList, key = { it.id }) { note ->
                                MiuiNoteCard(
                                    note = note,
                                    onClick = {
                                        editingNote = note
                                        editingInHiddenVault = false
                                        screenState = VaultScreenState.NOTE_EDITOR
                                    },
                                    onHideToVault = {
                                        notes = notes.map {
                                            if (it.id == note.id) it.copy(isHidden = true) else it
                                        }
                                        securityManager.saveNotes(notes)
                                    },
                                    onDelete = {
                                        notes = notes.filter { it.id != note.id }
                                        securityManager.saveNotes(notes)
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Action Button
                FloatingActionButton(
                    onClick = {
                        editingNote = null
                        editingInHiddenVault = false
                        screenState = VaultScreenState.NOTE_EDITOR
                    },
                    containerColor = Color(0xFFFF6900),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .size(60.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Note", modifier = Modifier.size(30.dp))
                }
            }
        }

        VaultScreenState.SECRET_VAULT -> {
            val secretNotes = notes.filter { it.isHidden }
            MiuiPrivateVaultScreen(
                notes = secretNotes,
                onBack = { screenState = VaultScreenState.NOTES_HOME },
                onAddNote = {
                    editingNote = null
                    editingInHiddenVault = true
                    screenState = VaultScreenState.NOTE_EDITOR
                },
                onSelectNote = { note ->
                    editingNote = note
                    editingInHiddenVault = true
                    screenState = VaultScreenState.NOTE_EDITOR
                },
                onUnhideNote = { note ->
                    notes = notes.map {
                        if (it.id == note.id) it.copy(isHidden = false) else it
                    }
                    securityManager.saveNotes(notes)
                },
                onDelete = { note ->
                    notes = notes.filter { it.id != note.id }
                    securityManager.saveNotes(notes)
                }
            )
        }

        VaultScreenState.NOTE_EDITOR -> {
            MiuiNoteEditor(
                existingNote = editingNote,
                isVaultNote = editingInHiddenVault,
                onSave = { saved ->
                    notes = if (editingNote != null) {
                        notes.map { if (it.id == saved.id) saved else it }
                    } else {
                        listOf(saved) + notes
                    }
                    securityManager.saveNotes(notes)
                    screenState = if (saved.isHidden) VaultScreenState.SECRET_VAULT else VaultScreenState.NOTES_HOME
                },
                onBack = {
                    screenState = if (editingInHiddenVault) VaultScreenState.SECRET_VAULT else VaultScreenState.NOTES_HOME
                }
            )
        }
    }

    // Password Fallback Dialog
    if (showPasswordModal) {
        MiuiPasswordDialog(
            securityManager = securityManager,
            onSuccess = {
                showPasswordModal = false
                screenState = VaultScreenState.SECRET_VAULT
            },
            onDismiss = { showPasswordModal = false }
        )
    }
}

@Composable
fun MiuiNoteCard(
    note: Note,
    onClick: () -> Unit,
    onHideToVault: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181822))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = if (note.title.isBlank()) "Untitled" else note.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.content,
                color = Color(0xFFABABBF),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = Color(0xFF6E6E82)
                )
                Row {
                    // Hide to vault action
                    IconButton(
                        onClick = onHideToVault,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = "Hide Note",
                            tint = Color(0xFFFF6900),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF4D4D),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiuiPrivateVaultScreen(
    notes: List<Note>,
    onBack: () -> Unit,
    onAddNote: () -> Unit,
    onSelectNote: (Note) -> Unit,
    onUnhideNote: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF0D0D11),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 12.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Private Notes",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF6900),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            "Protected by AES-256 & Fingerprint",
                            fontSize = 11.sp,
                            color = Color(0xFF00C4B4)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = Color(0xFFFF6900),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Secret Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1E2A),
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6900),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Private Vault is Empty", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + or use the lock icon on any note to hide it here.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 70.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectNote(note) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B27))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (note.title.isBlank()) "Untitled Secret" else note.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6900),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.content,
                                    color = Color(0xFFBEBECE),
                                    fontSize = 13.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { onUnhideNote(note) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LockOpen,
                                            contentDescription = "Unhide",
                                            tint = Color(0xFF00C4B4),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDelete(note) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFFF4D4D),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiuiNoteEditor(
    existingNote: Note?,
    isVaultNote: Boolean,
    onSave: (Note) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var content by remember { mutableStateOf(existingNote?.content ?: "") }
    var isHidden by remember { mutableStateOf(existingNote?.isHidden ?: isVaultNote) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D11))
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Lock toggle button
                IconButton(onClick = { isHidden = !isHidden }) {
                    Icon(
                        imageVector = if (isHidden) Icons.Default.Lock else Icons.Outlined.LockOpen,
                        contentDescription = "Toggle Private Note",
                        tint = if (isHidden) Color(0xFFFF6900) else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank() || content.isNotBlank()) {
                            onSave(
                                Note(
                                    id = existingNote?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    content = content.trim(),
                                    updatedAt = System.currentTimeMillis(),
                                    isHidden = isHidden
                                )
                            )
                        } else {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6900)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Title", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6E6E82)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        HorizontalDivider(color = Color(0xFF22222E), thickness = 1.dp)

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("Start typing your thoughts...", color = Color(0xFF555566)) },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun MiuiPasswordDialog(
    securityManager: SecurityManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val isSet = remember { securityManager.isPasswordSet() }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2C),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFF6900),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isSet) "Unlock Private Vault" else "Set Vault Password",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column {
                Text(
                    text = if (isSet) "Enter your secret password or PIN" else "Set a master password to protect private notes",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMsg = ""
                    },
                    label = { Text("Password / PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isSet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMsg = ""
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMsg, color = Color(0xFFFF4D4D), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSet) {
                        if (securityManager.verifyPassword(password)) {
                            onSuccess()
                        } else {
                            errorMsg = "Incorrect password! Please try again."
                        }
                    } else {
                        if (password.length < 4) {
                            errorMsg = "Password must be at least 4 chars long."
                        } else if (password != confirmPassword) {
                            errorMsg = "Passwords do not match."
                        } else {
                            securityManager.setPassword(password)
                            onSuccess()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6900)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
