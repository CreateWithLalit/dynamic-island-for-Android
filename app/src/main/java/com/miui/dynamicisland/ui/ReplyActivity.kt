package com.miui.dynamicisland.ui

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.data.repository.NotificationRepository
import com.miui.dynamicisland.ui.theme.DynamicIslandTheme
import kotlinx.coroutines.delay
import java.util.Locale

class ReplyActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_NOTIFICATION_KEY = "notification_key"
        const val EXTRA_CONTACT_NAME = "contact_name"
        
        fun launch(context: Context, contactName: String, notificationKey: String) {
            val intent = Intent(context, ReplyActivity::class.java).apply {
                putExtra(EXTRA_NOTIFICATION_KEY, notificationKey)
                putExtra(EXTRA_CONTACT_NAME, contactName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val notificationKey = intent.getStringExtra(EXTRA_NOTIFICATION_KEY) ?: ""
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Reply"
        
        setContent {
            DynamicIslandTheme {
                ReplyScreen(
                    contactName = contactName,
                    onSend = { message ->
                        sendReply(notificationKey, message)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
        
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }
    
    private fun sendReply(notificationKey: String, message: String) {
        val notificationData = NotificationRepository.notifications.value.items.find { it.notificationKey == notificationKey }
        val actions = notificationData?.actions ?: return
        
        val replyAction = actions.firstOrNull { action ->
            val title = action.title?.toString()?.lowercase(Locale.getDefault()) ?: ""
            action.remoteInputs?.isNotEmpty() == true && 
                (title.contains("reply") || title.contains("answer") || title.contains("write") || title.contains("message"))
        } ?: actions.firstOrNull { it.remoteInputs?.isNotEmpty() == true }

        if (replyAction != null) {
            executeDirectReply(this, replyAction, message)
            NotificationRepository.removeByKey(notificationKey)
        }
    }

    private fun executeDirectReply(context: Context, action: Notification.Action, messageText: String) {
        val remoteInputs = action.remoteInputs ?: return
        val resultsBundle = Bundle().apply {
            remoteInputs.forEach { input -> putCharSequence(input.resultKey, messageText) }
        }
        val fillInIntent = Intent().apply {
            RemoteInput.addResultsToIntent(remoteInputs, this, resultsBundle)
        }
        try {
            action.actionIntent.send(context, 0, fillInIntent)
        } catch (_: Exception) {}
    }
}

@Composable
fun ReplyScreen(
    contactName: String,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reply to $contactName",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF0A84FF), fontWeight = FontWeight.SemiBold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Type a reply...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { if (text.isNotBlank()) onSend(text) }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { if (text.isNotBlank()) onSend(text) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A84FF),
                        disabledContainerColor = Color(0xFF3A3A3C)
                    ),
                    enabled = text.isNotBlank()
                ) {
                    Text("Send", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
