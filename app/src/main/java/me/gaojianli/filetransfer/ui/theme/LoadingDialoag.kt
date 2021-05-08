package me.gaojianli.filetransfer.ui.theme

import android.app.Dialog
import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import me.gaojianli.filetransfer.R
import android.widget.TextView
import androidx.annotation.StringRes

class LoadingDialog(context: Context?) : Dialog(context!!, R.style.LoadingDialogTheme) {
    private val iv_loading: ImageView
    private val tv_hint: TextView
    private val animation: Animation
    fun show(hintText: String?, cancelable: Boolean, canceledOnTouchOutside: Boolean) {
        setCancelable(cancelable)
        setCanceledOnTouchOutside(canceledOnTouchOutside)
        tv_hint.text = hintText
        iv_loading.startAnimation(animation)
        show()
    }

    fun setText(hintText: String?){
        tv_hint.text = hintText
    }

    fun show(@StringRes hintTextRes: Int, cancelable: Boolean, canceledOnTouchOutside: Boolean) {
        setCancelable(cancelable)
        setCanceledOnTouchOutside(canceledOnTouchOutside)
        tv_hint.setText(hintTextRes)
        iv_loading.startAnimation(animation)
        show()
    }

    override fun cancel() {
        super.cancel()
        animation.cancel()
        iv_loading.clearAnimation()
    }

    override fun dismiss() {
        super.dismiss()
        animation.cancel()
        iv_loading.clearAnimation()
    }

    init {
        setContentView(R.layout.dialog_loading)
        iv_loading = findViewById(R.id.iv_loading)
        tv_hint = findViewById(R.id.tv_hint)
        animation = AnimationUtils.loadAnimation(context, R.anim.loading_dialog)
    }
}