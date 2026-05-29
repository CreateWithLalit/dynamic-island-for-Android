package com.miui.dynamicisland.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.DynamicIslandApplication
import com.miui.dynamicisland.data.repository.CallHistoryItem
import com.miui.dynamicisland.data.repository.ContactItem
import com.miui.dynamicisland.data.repository.PhoneRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DialerScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val phoneRepo = remember { PhoneRepository(context) }
    val callRepo = remember { (context.applicationContext as DynamicIslandApplication).callRepository }
    
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Keypad
    var dialNumber by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    
    var hasCallLogPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) 
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallLogPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCallLogPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    val recents = if (hasCallLogPermission) phoneRepo.getCallHistory() else emptyList()
    val contacts = phoneRepo.getContacts(searchQuery)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Black,
            contentColor = Color.White,
            indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(it[selectedTab]), color = Color(0xFF30D158)) }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Recents", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Keypad", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Contacts", modifier = Modifier.padding(16.dp))
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    if (hasCallLogPermission) {
                        RecentsList(recents) { callRepo.placeCall(it) }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Call Log Permission Required", color = Color.Gray)
                        }
                    }
                }
                1 -> KeypadContent(
                    dialNumber = dialNumber,
                    onNumberChange = { dialNumber = it },
                    onCall = { 
                        if (dialNumber.isNotBlank()) {
                            callRepo.placeCall(dialNumber)
                            onDismiss()
                        }
                    }
                )
                2 -> ContactsList(
                    contacts = contacts,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onContactClick = { callRepo.placeCall(it) }
                )
            }
        }

        // Close Button at bottom
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Text("Close Dialer")
        }
    }
}

@Composable
private fun RecentsList(items: List<CallHistoryItem>, onCall: (String) -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items) { item ->
            ListItem(
                headlineContent = { 
                    Text(
                        text = item.name ?: item.number,
                        color = if (item.type == CallLog.Calls.MISSED_TYPE) Color.Red else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                supportingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (item.type) {
                            CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                            CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.AutoMirrored.Filled.CallMissed
                        }
                        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text(dateFormat.format(Date(item.timestamp)), color = Color.Gray, fontSize = 12.sp)
                    }
                },
                trailingContent = {
                    IconButton(onClick = { onCall(item.number) }) {
                        Icon(Icons.Default.Call, null, tint = Color(0xFF30D158))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun ContactsList(
    contacts: List<ContactItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onContactClick: (String) -> Unit
) {
    Column {
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search Contacts", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1C1C1E),
                unfocusedContainerColor = Color(0xFF1C1C1E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF30D158),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(contacts) { contact ->
                ListItem(
                    headlineContent = { Text(contact.name, color = Color.White, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(contact.number, color = Color.Gray) },
                    trailingContent = {
                        IconButton(onClick = { onContactClick(contact.number) }) {
                            Icon(Icons.Default.Call, null, tint = Color(0xFF30D158))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun KeypadContent(
    dialNumber: String,
    onNumberChange: (String) -> Unit,
    onCall: () -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dialNumber,
            fontSize = 40.sp,
            color = Color.White,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 20.dp),
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(keys) { key ->
                DialerButton(key) {
                    if (dialNumber.length < 15) onNumberChange(dialNumber + key)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(64.dp))
            FloatingActionButton(
                onClick = onCall,
                containerColor = Color(0xFF30D158),
                shape = CircleShape,
                modifier = Modifier.size(74.dp)
            ) {
                Icon(Icons.Default.Call, "Call", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(
                onClick = { if (dialNumber.isNotEmpty()) onNumberChange(dialNumber.dropLast(1)) },
                modifier = Modifier.size(64.dp)
            ) {
                if (dialNumber.isNotEmpty()) Icon(Icons.Default.Backspace, "Delete", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun DialerButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Normal)
    }
}
