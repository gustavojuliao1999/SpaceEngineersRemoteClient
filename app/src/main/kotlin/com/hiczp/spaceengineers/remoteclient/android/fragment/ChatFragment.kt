package com.hiczp.spaceengineers.remoteclient.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.lifecycle.viewModelScope
import com.hiczp.spaceengineers.remoteapi.SpaceEngineersRemoteClient
import com.hiczp.spaceengineers.remoteclient.android.activity.VRageViewModel
import com.hiczp.spaceengineers.remoteclient.android.binding.FormViewModel
import com.hiczp.spaceengineers.remoteclient.android.binding.bind
import com.hiczp.spaceengineers.remoteclient.android.extension.emptyCoroutineExceptionHandler
import com.hiczp.spaceengineers.remoteclient.android.extension.toLocalDateTime
import com.hiczp.spaceengineers.remoteclient.android.extension.vRageViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.UI
import org.jetbrains.anko.support.v4.longToast

class ChatFragment : Fragment() {
    private lateinit var vRageViewModel: VRageViewModel
    private lateinit var model: ChatViewModel
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        vRageViewModel = vRageViewModel()
        model = ViewModelProvider(this)[ChatViewModel::class.java].apply {
            init(vRageViewModel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        lateinit var content: TextView
        lateinit var sendButton: Button
        val view = UI {
            verticalLayout {
                scrollView = scrollView {
                    horizontalPadding = dip(5)

                    content = textView {
                        hint = "Loading..."
                    }
                }.lparams(matchParent) {
                    weight = 1f
                }

                linearLayout {
                    editText {
                        singleLine = true
                    }.lparams(weight = 1f).bind(model = model, fieldName = "input")
                    sendButton = button("Send")
                }
            }
        }.view

        model.error.observe(this) {
            longToast(it)
        }

//        //input method
//        var inputMethodOpen = false
//        var previousBottomDifference = 0
//        scrollView.onScrollChange { _, _, scrollY, _, _ ->
//            if (!inputMethodOpen) {
//                previousBottomDifference = content.bottom - (scrollView.height + scrollY)
//            }
//        }
//        scrollView.onLayoutChange { _, _, _, _, bottom, _, _, _, oldBottom ->
//            if (oldBottom != 0 && oldBottom != bottom) {
//                scrollView.scrollTo(
//                    scrollView.scrollX,
//                    content.bottom - scrollView.height - previousBottomDifference
//                )
//                inputMethodOpen = oldBottom > bottom
//            }
//        }

        //new messages incoming
        var firstTimeReceiveMessage = true
        var previousLine = 0
        vRageViewModel.chatMessages.observe(this@ChatFragment) { messages ->
            if (firstTimeReceiveMessage) content.hint = ""
            if (messages.size > previousLine) {
                val nowInEnd = content.bottom == scrollView.height + scrollView.scrollY
                val currentLine = messages.size
                content.append(
                    messages.subList(previousLine, currentLine).joinToString(
                        separator = "\n",
                        postfix = "\n"
                    ) {
                        "${it.timestamp.toLocalDateTime()} [${it.displayName}]: ${it.content}"
                    }
                )
                previousLine = currentLine

                val savedScrollY = model.scrollY
                if (firstTimeReceiveMessage && savedScrollY != null) {  //rebuild UI
                    scrollView.post {
                        scrollView.scrollY = savedScrollY
                    }
                } else if (nowInEnd) {  //auto scroll
                    scrollView.post {
                        scrollView.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
            firstTimeReceiveMessage = false
        }

        //sendButton
        model.observe("input", this) {
            sendButton.isEnabled = it.isNotEmpty()
        }
        sendButton.onClick {
            val inputText = model["input"]!!
            model["input"] = ""
            model.sendMessage(inputText)
            //scroll to end after message send
            scrollView.fullScroll(View.FOCUS_DOWN)
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        val windowToken = requireActivity().currentFocus?.windowToken
        if (windowToken != null) {
            requireContext().getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(windowToken, 0)
        }
        //status
        model.scrollY = scrollView.scrollY
    }
}

//no multiple inheritance
class ChatViewModel : FormViewModel() {
    private lateinit var vRageViewModel: VRageViewModel
    private lateinit var client: SpaceEngineersRemoteClient
    val error = MutableLiveData<String>()
    var scrollY: Int? = null

    fun init(vRageViewModel: VRageViewModel) {
        this.vRageViewModel = vRageViewModel
        this.client = vRageViewModel.client
    }

    fun sendMessage(message: String) = viewModelScope.launch(IO + emptyCoroutineExceptionHandler) {
        try {
            client.session.sendMessage(message)
        } catch (e: Exception) {
            error.postValue(e.message ?: e.toString())
            throw e
        }
        vRageViewModel.chatMessagePulse.send(Unit)
    }
}
