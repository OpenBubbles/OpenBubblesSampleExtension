package com.example.openbubblesextension

import android.R.id
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.SyncStateContract.Constants
import android.util.Base64
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.Text
import com.bluebubbles.messaging.IKeyboardHandle
import com.bluebubbles.messaging.IMadridExtension
import com.bluebubbles.messaging.IMessageViewHandle
import com.bluebubbles.messaging.ITaskCompleteCallback
import com.bluebubbles.messaging.IViewUpdateCallback
import com.bluebubbles.messaging.MadridMessage
import com.example.openbubblesextension.MadridExtension.Companion.currentKeyboardHandle
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.roundToInt


class KeyboardClickCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val bm = BitmapFactory.decodeResource(context.resources, R.drawable.my_image)
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val b = baos.toByteArray()
        val imageEncoded: String = Base64.encodeToString(b, Base64.NO_WRAP)


        val message = MadridMessage().apply {
            messageGuid = UUID.randomUUID().toString()
            ldText = "Basketball"
            url = "data:asdjfladsjf"
            session = UUID.randomUUID().toString()

            imageBase64 = imageEncoded
            caption = "Play basketball"

            isLive = true
        }

        currentKeyboardHandle?.addMessage(message)
    }
}

class MadridExtension(private val context: Context) : IMadridExtension.Stub() {

    companion object {
        var currentKeyboardHandle: IKeyboardHandle? = null
    }

    private var callback: IViewUpdateCallback? = null;

    override fun keyboardClosed() {
        currentKeyboardHandle = null
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    val keyboardRemoteViews = GlanceRemoteViews()

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    override fun keyboardOpened(callback: IViewUpdateCallback?, handle: IKeyboardHandle?): RemoteViews {
        this.callback = callback

        currentKeyboardHandle = handle

        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density

        val result = runBlocking {
            keyboardRemoteViews.compose(context, DpSize(dpWidth.dp, 300.dp)) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button("Hello world!", actionRunCallback<KeyboardClickCallback>())
                }
            }
        }

        return result.remoteViews
    }

    override fun didTapTemplate(message: MadridMessage?, handle: IMessageViewHandle?) {
        val intent = Intent(context, MessageActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        handle?.lock()
        Log.i("here", message!!.caption)
        message.caption = "no way jose"
        handle!!.updateMessage(message, object : ITaskCompleteCallback.Stub() {
            override fun complete() {
                Log.i("sent!", "done")
                handle.unlock()
            }
        })
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    override fun getLiveView(
        callback: IViewUpdateCallback?,
        message: MadridMessage?,
        handle: IMessageViewHandle?
    ): RemoteViews {
        Log.i("live view", "init")
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val messageWidth = (dpWidth * 0.60).roundToInt() - 10

        val result = runBlocking {
            keyboardRemoteViews.compose(context, DpSize(messageWidth.dp, 250.dp)) {
                Column {
                    Image(ImageProvider(R.drawable.my_image), "Flower", modifier = GlanceModifier.wrapContentHeight())
                    Text("Caption here...")
                }
            }
        }

        return result.remoteViews
    }

    override fun messageUpdated(message: MadridMessage?) {
        Log.i("update", "message");
    }

}