package id.diskola.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import androidx.core.content.ContextCompat
import id.diskola.app.R
import java.math.BigInteger
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class StringUtil @Inject constructor() {

    fun buildUserContentComment(
            context: Context,
            username: String,
            content: String,
            onClickUser: () -> Unit = {},
            onClickHashtag: (hashtag: String) -> Unit = {},
            onClickMention: (username: String) -> Unit = {}
    ) =
        SpannableStringBuilder(Html.fromHtml("<b>$username</b>")).apply {
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClickUser.invoke()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = ContextCompat.getColor(context, R.color.black)
                }
            }, 0, length, 0)
            append(" $content")

            val hashMatch = hashtagPattern.matcher(content)
            while (hashMatch.find()) {
                val find = hashMatch.group()
                val index = max(indexOf(find) - 1, 0)
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onClickHashtag.invoke(find)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.isFakeBoldText = true
                        ds.color = Color.BLACK
                    }
                }, index, min(index + find.length + 1, length), 0)
            }

            val mentionMatch = mentionPattern.matcher(content)
            while (mentionMatch.find()) {
                val find = mentionMatch.group()
                val index = max(indexOf(find) - 1, 0)
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onClickMention.invoke(find)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.isFakeBoldText = true
                        ds.color = Color.BLACK
                    }
                }, index, min(index + find.length + 1, length), 0)
            }
        }

    private val hashtagPattern by lazy { Pattern.compile("(?<![a-zA-Z0-9_])#(?=[0-9a-zA-Z])[a-zA-Z0-9_]+") }

    //    private val hashtagPattern by lazy { Pattern.compile("(?<![a-zA-Z0-9_])#(?=[0-9_]*[a-zA-Z])[a-zA-Z0-9_]+") }
    private val mentionPattern by lazy { Pattern.compile("@\\S*") }

    fun buildUserContentPost(
            context: Context,
            content: String,
            maxLength: Int = -1,
            onClickHashtag: (hashtag: String) -> Unit = {},
            onClickMention: (username: String) -> Unit = {},
            onClickLoadMore: () -> Unit = {}
    ): Spannable {
        val usedContent = if (maxLength > 0 && content.length > maxLength)
            content.substring(0, maxLength)
        else content

        return SpannableStringBuilder(usedContent).apply {
            val hashMatch = hashtagPattern.matcher(usedContent)
            while (hashMatch.find()) {
                val find = hashMatch.group()
                val index = max(usedContent.indexOf(find) - 1, 0)
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onClickHashtag.invoke(find)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.isFakeBoldText = true
                        ds.color = Color.BLACK
                    }
                }, index, min(index + find.length + 1, length), 0)
            }

            val mentionMatch = mentionPattern.matcher(usedContent)
            while (mentionMatch.find()) {
                val find = mentionMatch.group()
                val index = max(usedContent.indexOf(find) - 1, 0)
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onClickMention.invoke(find)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.isFakeBoldText = true
                        ds.color = Color.BLACK
                    }
                }, index, min(index + find.length + 1, length), 0)
            }

            val urlMatch = Patterns.WEB_URL.matcher(usedContent)
            while (urlMatch.find()) {
                val find = urlMatch.group()
                val index = max(usedContent.indexOf(find) - 1, 0)
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        (context as? Activity)?.startActivity(
                                Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(find)
                                )
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = true
                        ds.isFakeBoldText = false
                        ds.color = Color.BLACK
                    }
                }, index, min(index + find.length + 1, length), 0)
            }

            if (usedContent != content) {
                append(" selengkapnya")
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onClickLoadMore.invoke()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(context, R.color.white)
                        ds.isFakeBoldText = false
                        ds.isUnderlineText = false
                    }
                }, length - " selengkapnya".length, length, 0)
            }
        }
    }

    private val numberFormatDouble by lazy {
        NumberFormat.getCurrencyInstance(Locale("id")).apply {
            maximumFractionDigits = 2
            (this as? DecimalFormat)?.decimalFormatSymbols =
                (this as? DecimalFormat)?.decimalFormatSymbols?.apply { currencySymbol = "" }
        }
    }

    private val numberFormatInt by lazy {
        NumberFormat.getCurrencyInstance(Locale("id")).apply {
            maximumFractionDigits = 0
            (this as? DecimalFormat)?.decimalFormatSymbols =
                (this as? DecimalFormat)?.decimalFormatSymbols?.apply { currencySymbol = "" }
        }
    }

    fun formatCurrency(input: Double): String {
        return numberFormatDouble.format(input)
    }

    fun formatCurrency(input: Int): String {
        return numberFormatDouble.format(input)
    }

    fun formatCurrency2(input: Int): String {
        return numberFormatInt.format(input)
    }

    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    fun numOnly(input: String) = input.replace("\\D".toRegex(), "").toInt()

    fun conversationDateFormat(date: Date?): String {
        if (date == null) return ""

        val current = Calendar.getInstance()
        val input = Calendar.getInstance().apply { time = date }

        if (current.get(Calendar.YEAR) == input.get(Calendar.YEAR)) {
            return if (current.get(Calendar.DAY_OF_YEAR) == input.get(Calendar.DAY_OF_YEAR)) {
                // format time
                SimpleDateFormat("HH:mm", Locale("id")).format(date)
            } else {
                // format date month
                SimpleDateFormat("dd MMM", Locale("id")).format(date)
            }
        }

        return SimpleDateFormat("dd MMM yyyy", Locale("id")).format(date)
    }
    fun limitComma(number:String):String{
        val df = DecimalFormat("#.#")
        if(number!=""){
            return df.format(number.toDouble())
        }else{
            return ""
        }
    }

}