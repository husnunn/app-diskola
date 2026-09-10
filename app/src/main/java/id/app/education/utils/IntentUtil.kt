package id.app.education.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.LabeledIntent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import androidx.activity.result.ActivityResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import id.app.education.apiservice.AuthApiService
import id.app.education.BuildConfig
import id.app.education.database.LocalDatabase
import id.app.education.R
import java.util.ArrayList


class
IntentUtil @Inject constructor(
    @ApplicationContext val context: Context,
    val localDatabase: LocalDatabase,
    val pref: PreferenceClass,
    val fileUtils: FileUtils,
    val apiService: Provider<AuthApiService>
) {

    fun openWhatsApp(
        activity: Activity,
        phone: String,
        content: String = "",
        onFailed: () -> Unit = {}
    ) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.whatsapp")
                data = Uri.parse("https://wa.me/$phone?text=$content")
            })
        } catch (e: Exception) {
            Timber.e(e)
            onFailed.invoke()
        }
    }

    fun openCall(activity: Activity, phone: String) {
        activity.startActivity(Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel: $phone")
        })
    }

    fun openSms(activity: Activity, phone: String, content: String = "") {
        activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms: $phone")
            putExtra("sms_body", content)
        })
    }

    var onChoosenIntent: (Intent?) -> Unit = {}
    private val chooserReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                context?.unregisterReceiver(this)
                try {
                    onChoosenIntent.invoke(intent)
                    intent?.extras?.let {
                        for (key in it.keySet()) {
                            Timber.e("broadcast intent: %s", it[key])
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    fun openEmail(activity: Activity, onChoosenIntent: (Intent?) -> Unit = {}) {
        val emailIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
        val packageManager = activity.packageManager

        val activitiesHandlingEmails = packageManager.queryIntentActivities(emailIntent, 0)
        if (activitiesHandlingEmails.isNotEmpty()) {
            // use the first email package to create the chooserIntent
            val firstEmailPackageName = activitiesHandlingEmails.first().activityInfo.packageName
            val firstEmailInboxIntent =
                packageManager.getLaunchIntentForPackage(firstEmailPackageName)
            val emailAppChooserIntent =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    Intent.createChooser(
                        firstEmailInboxIntent,
                        "Buka email dengan",
                        PendingIntent.getBroadcast(
                            activity,
                            0,
                            Intent("${activity.packageName}.CHOOSER_INTENT"),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        ).intentSender
                    )
                } else {
                    Intent.createChooser(
                        firstEmailInboxIntent,
                        "Buka email dengan"
                    )
                }

            // created UI for other email packages and add them to the chooser
            val emailInboxIntents = mutableListOf<LabeledIntent>()
//            for (i in 1 until activitiesHandlingEmails.size) {
//                val activityHandlingEmail = activitiesHandlingEmails[i]
            for (activityHandlingEmail in activitiesHandlingEmails) {
                val packageName = activityHandlingEmail.activityInfo.packageName
                Timber.e("package: $packageName")
                if (emailInboxIntents.firstOrNull { it.sourcePackage == packageName } == null) {
                    emailInboxIntents.add(
                        LabeledIntent(
                            packageManager.getLaunchIntentForPackage(packageName),
                            packageName,
                            activityHandlingEmail.loadLabel(packageManager),
                            activityHandlingEmail.icon
                        )
                    )
                }
            }
            val extraEmailInboxIntents = emailInboxIntents.toTypedArray()
            Timber.e("extraEmailInboxIntents: $extraEmailInboxIntents")

            if (extraEmailInboxIntents.isNotEmpty()) {
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
//                    this.onChoosenIntent = onChoosenIntent
//                    LocalBroadcastManager.getInstance(activity).registerReceiver(
//                        chooserReceiver,
//                        IntentFilter("${activity.packageName}.CHOOSER_INTENT")
//                    )
//                } else
                onChoosenIntent.invoke(null)

                activity.startActivity(
                    emailAppChooserIntent.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        extraEmailInboxIntents
                    )
                )
            } else {
                onChoosenIntent.invoke(null)
                Timber.e("email intent is empty")
            }
        } else {
            onChoosenIntent.invoke(null)
            Timber.e("mailto intent is empty")
        }
    }

    var currentPhotoPath: String = ""

    fun requestCameraPermission(activity: Activity) {
        // ✅ Step 1: Check permission before showing dialog
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera(activity)
            return
        }

        // ✅ Step 2: Show explanation dialog
        showCameraPermissionDialog(activity) {
            val permissions = mutableListOf(Manifest.permission.CAMERA)

            // Handle different API levels
            if (Build.VERSION.SDK_INT <= 28) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE) // API < 29
            } else if (Build.VERSION.SDK_INT < 33) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE) // API 29 - 32
            } else {
                permissions.addAll(
                    listOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                ) // API 33+
            }

            // ✅ Step 3: Request Permissions Using Dexter
            Dexter.withContext(activity)
                .withPermissions(permissions)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report?.areAllPermissionsGranted() == true) {
                            launchCamera(activity)
                        } else {
                            Toast.makeText(activity, "\"Izin ditolak, fitur kamera tidak dapat digunakan\"", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                })
                .check()
        }
    }

    private fun launchCamera(activity: Activity) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        takePictureIntent.resolveActivity(activity.packageManager)?.also {
            val photoFile: File? = try {
                val timeStamp = System.currentTimeMillis().toString()
                val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: activity.filesDir
                File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                    currentPhotoPath = absolutePath
                }
            } catch (ex: IOException) {
                Toast.makeText(activity, "\"Gagal membuka kamera, izin tidak diberikan\"", Toast.LENGTH_SHORT).show()
                null
            }

            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.provider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                activity.startActivityForResult(takePictureIntent, RC_CAMERA)
            }
        }
    }

    fun requestGalleryPermission(activity: Activity) {
        // ✅ Check before showing dialog
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activity.startActivityForResult(intent, RC_GALLERY_PHOTO)
            return
        }

        showGalleryPermissionDialog(activity) {
            val permissions = mutableListOf<String>()

            when {
                Build.VERSION.SDK_INT <= 28 -> permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                Build.VERSION.SDK_INT < 33 -> permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                else -> permissions.addAll(
                    listOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                )
            }

            Dexter.withContext(activity)
                .withPermissions(permissions)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report?.areAllPermissionsGranted() == true) {
                            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            activity.startActivityForResult(intent, RC_GALLERY_PHOTO)
                        } else {
                            Toast.makeText(activity, "\"Izin ditolak, fitur galeri tidak dapat digunakan\"", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                })
                .check()
        }
    }

    fun showCameraPermissionDialog(activity: Activity, onProceed: () -> Unit) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Akses Kamera Diperlukan")
            .setMessage("Aplikasi ini membutuhkan akses ke kamera Anda untuk mengambil foto untuk profil, absensi, atau transaksi QR. Data ini hanya digunakan dalam aplikasi dan tidak akan dibagikan tanpa izin Anda.")
            .setPositiveButton("Setuju") { _, _ -> onProceed() }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(activity, R.color.black))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(activity, R.color.black))
    }

    fun showGalleryPermissionDialog(activity: Activity, onProceed: () -> Unit) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Akses Galeri Diperlukan")
            .setMessage("Aplikasi ini membutuhkan akses ke galeri Anda untuk memungkinkan pemilihan gambar yang akan digunakan dalam kebutuhan fitur materi dan tugas. Data ini hanya digunakan dalam aplikasi dan tidak akan dibagikan tanpa izin Anda.")
            .setPositiveButton("Setuju") { _, _ -> onProceed() }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(activity, R.color.black))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(activity, R.color.black))
    }

    fun compressBitmap(bitmap: Bitmap, maxSizeInBytes: Int): Bitmap? {
        var quality = 80
        var compressedSize: Int
        var outputStream: ByteArrayOutputStream
        var compressedBitmap: Bitmap
        do {
            outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedBitmap =
                BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size())
            compressedSize = outputStream.toByteArray().size
            quality -= 5
        } while (quality > 0 && compressedSize > maxSizeInBytes)
        return if (compressedSize <= maxSizeInBytes) compressedBitmap else null
    }

    fun downloadFile(activity: Activity, fileUri: Uri, fileName: String, fileType: String) {
        val permissions = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (Build.VERSION.SDK_INT <= 28)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        Dexter.withContext(activity)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        if (fileUri.toString().isEmpty())
                            Toast.makeText(
                                activity,
                                "$fileType tidak tersedia, mohon ulangi beberapa saat lagi",
                                Toast.LENGTH_SHORT
                            ).show()
                        else {
                            Toast.makeText(
                                activity,
                                "proses download $fileType akan dimulai sesaat lagi",
                                Toast.LENGTH_SHORT
                            ).show()
                            (activity.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager?)?.enqueue(
                                DownloadManager.Request(fileUri)
                                    .apply {
                                        setTitle(fileName)
                                        setDestinationInExternalPublicDir(
                                            Environment.DIRECTORY_DOWNLOADS, fileName
                                        )
                                        allowScanningByMediaScanner()
                                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    }
                            )
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }

//    fun openPdfPicker(activity: Activity, pageTitle: String) = openFilePicker(
//        activity,
//        pageTitle,
//        listOf(Triple("Pdf", arrayOf("pdf"), R.drawable.ic_pdf))
//
//    )

//    fun openFilePicker(
//        activity: Activity,
//        pageTitle: String,
//        fileType: List<Triple<String, Array<String>, Int>>
//    ) {
//        val permissions = mutableListOf(
//            Manifest.permission.CAMERA,
//            Manifest.permission.RECORD_AUDIO,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        )
//        if (Build.VERSION.SDK_INT <= 28)
//            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//
//        Dexter.withContext(activity)
//            .withPermissions(permissions)
//            .withListener(object : MultiplePermissionsListener {
//                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
//                    FilePickerBuilder.instance
//                        .setMaxCount(1)
//                        .setActivityTitle(pageTitle)
//                        .setActivityTheme(R.style.PickerTheme)
//                        .apply {
//                            fileType.forEach {
//                                addFileSupport(it.first, it.second, it.third)
//                            }
//                        }
//                        .enableDocSupport(false)
//                        .showFolderView(true)
//                        .enableImagePicker(true)
////                        .apply {
////                            fileType.firstOrNull { it.second.contains("jpg") }?.let {
////                                enableImagePicker(true)
////                            }
////                        }
//                        .pickFile(activity, RC_PDF_PICKER)
//                }
//
//                override fun onPermissionRationaleShouldBeShown(
//                    permissions: MutableList<PermissionRequest>?,
//                    token: PermissionToken?
//                ) {
//                    token?.continuePermissionRequest()
//                }
//            })
//            .check()
//    }

//    fun openFile(activity: Activity, file: File, title: String) {
//        showFilePermissionDialog(activity) {
//            try {
//                MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)?.let { mimeType ->
//                    Timber.e("File type: $mimeType")
//                    if (mimeType.contains("pdf", true)) {
//                        activity.startActivity(
//                            Intent(activity, PdfPage::class.java)
//                                .putExtra("file_path", file.path)
//                                .putExtra("title", title)
//                        )
//                    } else {
//                        val intent = Intent(Intent.ACTION_VIEW)
//
//                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                            FileProvider.getUriForFile(activity, id.ebede.education.BuildConfig.APPLICATION_ID, file)
//                        } else {
//                            Uri.fromFile(file)
//                        }
//
//                        intent.setDataAndType(uri, mimeType)
//                        activity.startActivity(intent)
//                    }
//                }
//            } catch (e: Exception) {
//                Timber.e(e)
//                (activity as? BasePage)?.toast("Gagal membuka file")
//            }
//        }
//    }
//
//
//    fun openPdf(activity: Activity, uri: String, title: String) {
////        try {
//        activity.startActivity(
//            Intent(activity, PdfPage::class.java)
//                .putExtra("file_path", uri)
//                .putExtra("title", title)
//        )
////            val file = File(uri)
////            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)?.let {
////                if (it.contains("pdf", true)) {
////                    activity.startActivity(
////                        Intent(activity, PdfPage::class.java)
////                            .putExtra("file_path", uri)
////                            .putExtra("title", title)
////                    )
////                } else {
////                    val intent = Intent()
////                        .setAction(Intent.ACTION_VIEW)
////                        .setData(Uri.parse(uri))
//////                            .setDataAndType(Uri.parse(uri), it)
//////                            .setDataAndType(Uri.fromFile(file), it)
////                        .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION)
////
////                    if (intent.resolveActivity(activity.packageManager) != null)
////                        activity.startActivity(intent)
////                    else
////                        (activity as? BasePage)?.toast("Gagal membuka file, tidak terdapat aplikasi yang sesuai")
////                }
////            }
////        } catch (e: Exception) {
////            Timber.e(e)
////            (activity as? BasePage)?.toast("Gagal membuka file")
////
//////            val intent = Intent()
//////                .setAction(Intent.ACTION_VIEW)
//////                .setData(Uri.parse(uri))
//////                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION)
//////
//////            if (intent.resolveActivity(activity.packageManager) != null)
//////                activity.startActivity(intent)
////        }
//    }

    fun copyText(activity: Activity, text: String) {
        (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("label", text))
    }

    suspend fun logOut() {
        try {
            withContext(Dispatchers.IO) {
                localDatabase.clearAllTables()
            }
            FirebaseMessaging.getInstance().apply {
                unsubscribeFromTopic("ebede-notification-user-${pref.getString("user_uuid")}")
                unsubscribeFromTopic("Klaspay-US-${pref.getInt("user_id")}")
                unsubscribeFromTopic("attendance-${pref.getInt("class_id")}")
                unsubscribeFromTopic("loggedin")

                subscribeToTopic("unlogged")
            }
            try {
//                if (pref.getBoolean("logged_in")) apiService.logout()
            } catch (e: Exception) {
                Timber.e(e)
            }
            pref.edit(true) {
                clear()
                putString("url_api", BuildConfig.API_URL)
                putBoolean("onboard", true)
            }
            pref.edit().apply {
                clear()
                apply()
                putString("url_api", BuildConfig.API_URL)
                putBoolean("onboard", true)
            }
            context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE).edit().clear().apply()

            NotificationManagerCompat.from(context).cancelAll()
            WorkManager.getInstance(context).cancelAllWork()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun logOut(context: Context) {
        GlobalScope.launch {
            localDatabase.clearAllTables()
            FirebaseMessaging.getInstance().apply {
                unsubscribeFromTopic("ebede-notification-user-${pref.getString("user_uuid")}")
                unsubscribeFromTopic("Klaspay-US-${pref.getInt("user_id")}")
                unsubscribeFromTopic("attendance-${pref.getInt("class_id")}")
                unsubscribeFromTopic("loggedin")
                subscribeToTopic("unlogged")
            }
            try {
//                if (pref.getBoolean("logged_in")) apiService.logout()
            } catch (e: Exception) {
                Timber.e(e)
            }
            pref.edit(true) {
                clear()
                putString("url_api", BuildConfig.API_URL)
                putBoolean("onboard", true)
                putBoolean("check_vc", true)
            }
            context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE).edit().clear().apply()

            NotificationManagerCompat.from(context.applicationContext).cancelAll()
            WorkManager.getInstance(context).cancelAllWork()
        }
    }


    fun showFilePermissionDialog(activity: Activity, onProceed: () -> Unit) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Akses File Diperlukan")
            .setMessage("Aplikasi ini membutuhkan akses ke penyimpanan Anda untuk memilih atau membuka file dalam proses unggah dokumen tugas dan materi. Data ini hanya digunakan dalam aplikasi dan tidak akan dibagikan tanpa izin Anda.")
            .setPositiveButton("Setuju") { _, _ -> onProceed() }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(activity.resources.getColor(R.color.black, activity.theme))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(activity.resources.getColor(R.color.black, activity.theme))
    }

    fun handleResult(
        result: ActivityResult,
        uriList: MutableList<Uri>,
        onFileProcessed: (String?, Uri?, String?, Pair<String?,Long?>) -> Unit,  // Add file name as fourth parameter
        lifecycleScope: LifecycleCoroutineScope
    ) {
        if (result.resultCode == Activity.RESULT_OK) {
            uriList.clear()
            result.data?.let { data ->
                data.data?.let { uriList.add(it) }
                data.getClipDataUris().let { uriList.addAll(it) }
            }

            if (uriList.isNotEmpty()) {
                val uri = uriList.first()
                val filePath = getFilePathFromUri(context, uri)
                val mimeType = context.contentResolver.getType(uri)
                val fileInfo = getFileInfo(context, uri)

                lifecycleScope.launch {
                    // Pass the file name along with the file path, URI, and MIME type
                    onFileProcessed(filePath, uri, mimeType, fileInfo)
                }
            }
        } else {
            Timber.e("File picker canceled or error.")
        }
    }

    fun getFileInfo(context: Context, uri: Uri): Pair<String?, Long?> {
        var fileName: String? = null
        var fileSize: Long? = null
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                // Get file name
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }

                // Get file size
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = it.getLong(sizeIndex)
                }
            }
        }
        return Pair(fileName, fileSize)
    }

    fun Intent.getClipDataUris(): ArrayList<Uri> {
        val resultSet = LinkedHashSet<Uri>()
        data?.let { data ->
            resultSet.add(data)
        }
        val clipData = clipData
        if (clipData == null && resultSet.isEmpty()) {
            return ArrayList()
        } else if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val uri = clipData.getItemAt(i).uri
                if (uri != null) {
                    resultSet.add(uri)
                }
            }
        }
        return ArrayList(resultSet)
    }

    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                filePath = cursor.getString(columnIndex)
            }
        }
        return filePath
    }

    companion object {
        const val RC_PDF_PICKER = 8329
        const val RC_CAMERA = 4382
        const val RC_GALLERY_PHOTO = 1321
        const val RC_FILE_PICKER = 2020
    }
}