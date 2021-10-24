package com.shawnyang.jpreader_lib

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.mcxiaoke.koi.ext.toast
import com.shawnyang.jpreader_lib.data.Book
import com.shawnyang.jpreader_lib.data.BooksDatabase
import com.shawnyang.jpreader_lib.data.books
import com.shawnyang.jpreader_lib.exts.ContentResolverUtil
import com.shawnyang.jpreader_lib.exts.moveTo
import com.shawnyang.jpreader_lib.exts.toFile
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.extensions.mediaType
import org.readium.r2.shared.extensions.toPng
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.URL
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author ShineYang
 * @date 2021/10/22
 * description:
 */
class ReaderEntranceDelegate(val activity: ComponentActivity) : Application.ActivityLifecycleCallbacks, CoroutineScope {
    private val CHANNEL_NAME = "native_plugin"
    private lateinit var R2DIRECTORY: String
    private var localPort: Int = 0
    private lateinit var database: BooksDatabase
    private lateinit var server: Server
    private lateinit var streamer: Streamer
    private lateinit var readerLauncher: ActivityResultLauncher<ReaderContract.Input>
    private lateinit var documentPickerLauncher: ActivityResultLauncher<String>

    private val FLUTTER_CHANNEL_NAME = "update_tunnel"
    private val UPDATE_DB_SUCCESS = "update_db_success"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        //EventBus.getDefault().register(activity)
        initServer()
    }

    private fun initServer() {
        //init streamer(parser)
        streamer = Streamer(activity)
        val s = ServerSocket(if (BuildConfig.DEBUG) 8080 else 0)
        s.localPort
        s.close()
        localPort = s.localPort
        server = Server(localPort, activity.applicationContext)

        val properties = Properties()
        val inputStream = activity.assets.open("config/config.properties")
        properties.load(inputStream)
        val useExternalFileDir = properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        R2DIRECTORY = if (useExternalFileDir) {
            activity.getExternalFilesDir(null)?.path + "/"
        } else {
            activity.filesDir.path + "/"
        }

        database = BooksDatabase(activity)
        books = database.books.list()

        readerLauncher = activity.registerForActivityResult(ReaderContract()) { pubData: ReaderContract.Output? ->
            if (pubData == null)
                return@registerForActivityResult

            tryOrNull { pubData.publication.close() }
            Timber.d("Publication closed")
            if (pubData.deleteOnResult)
                tryOrNull { pubData.file.delete() }
        }

        //文件选择器
        documentPickerLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                importPublicationFromUri(it)
            }
        }
    }

    fun onCall(call: Map<String, String>, onResult: ((String) -> Unit)){
        when (call["methodName"]) {
            MessageType.OpenBook.methodName -> {
                //打开书籍
                val id: String? = call["param"]
                if (!id.isNullOrEmpty()) {
                    launch {
                        openBook(id)
                    }
                }
            }
            MessageType.GetBookList.methodName -> {
                //处理本地书籍列表的参数
                val bookListJson: String = Gson().toJson(database.books.list())
                onResult(bookListJson)
            }

            MessageType.FilePicker.methodName -> {
                //file picker
                documentPickerLauncher.launch("application/epub+zip")
                //todo remove this test code
                onResult("打开文件选择器成功")
            }

            MessageType.RemoveFromLibrary.methodName -> {
                //删除指定id的书籍
                val id: String? = call["param"]
                if (!id.isNullOrEmpty()) {
                    database.books.deleteById(id)
                    onResult("1")
                }
            }

//            MessageType.FlutterComponentInitCompleted.methodName -> {
//                //初始化message channel
//                messageChannel = BasicMessageChannel(flutterEngine.dartExecutor.binaryMessenger, FLUTTER_CHANNEL_NAME, StringCodec.INSTANCE)
//                Timber.e("native: 初始化message channel")
//            }
            else -> {
                onResult("")
            }
        }
    }

    //打开path下的书籍
    private suspend fun openBook(id: String) {
        var book: Book?
        coroutineScope {
            launch {
                book = database.books.queryById(id)
                if (book != null) {
                    val asset = FileAsset(File(book!!.href))
                    streamer.open(asset, allowUserInteraction = true, sender = activity)
                        .onFailure {
                            Timber.d(it.getUserMessage(activity))
                        }
                        .onSuccess { publication ->
                            if (publication.isRestricted) {
                                publication.protectionError?.let { error ->
                                    Timber.d(error.getUserMessage(activity))
                                }
                            } else {
                                readerLauncher.launch(
                                    ReaderContract.Input(
                                        file = asset.file,
                                        mediaType = asset.mediaType(),
                                        publication = publication,
                                        bookId = book!!.id!!,
                                        deleteOnResult = false,
                                        initialLocator = database.books.currentLocator(book!!.id!!),
                                        baseUrl = prepareToServe(publication, asset)
                                    )
                                )
                            }
                        }
                }
            }
        }
    }

    //打开path文件
    private fun importPublicationFromUri(uri: Uri) {
        launch {
            uri.copyToTempFile()
                ?.let { importPublication(it, sourceUrl = uri.toString()) }
        }
    }

    private suspend fun importPublication(sourceFile: File, sourceUrl: String? = null) {
        val sourceMediaType = sourceFile.mediaType()

        val publicationAsset = FileAsset(sourceFile, sourceMediaType)

        val mediaType = publicationAsset.mediaType()
        val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
        val libraryAsset = FileAsset(File(R2DIRECTORY + fileName), mediaType)

        try {
            publicationAsset.file.moveTo(libraryAsset.file)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { publicationAsset.file.delete() }
            //todothat notice unable to move publication into the library
            return
        }

        val extension = libraryAsset.let {
            it.mediaType().fileExtension ?: it.file.extension
        }
        val isRwpm = libraryAsset.mediaType().isRwpm
        val bddHref =
            if (!isRwpm)
                libraryAsset.file.path
            else
                sourceUrl ?: run {
                    Timber.e("Trying to add a RWPM to the database from a file without sourceUrl.")
                    return
                }
        streamer.open(libraryAsset, allowUserInteraction = false, sender = activity)
            .onSuccess {
                addPublicationToDatabase(bddHref, extension, it).let { success ->
                    //给出添加结果提示
                    //发送更新成功消息
                    if (success) {
                        activity.toast("添加成功")
                        //todo messageChannel?.send(UPDATE_DB_SUCCESS)
                    }
                    if (success && isRwpm)
                        tryOrNull { libraryAsset.file.delete() }
                }
            }
            .onFailure {
                tryOrNull { libraryAsset.file.delete() }
                Timber.d(it)
                //打开书籍失败
                activity.toast("打开失败")
            }
    }


    private suspend fun addPublicationToDatabase(href: String, extension: String, publication: Publication): Boolean {
        val publicationIdentifier = publication.metadata.identifier ?: ""
        val author = publication.metadata.authors.firstOrNull()?.name ?: ""
        val cover = publication.cover()?.toPng()

        val book = Book(
            title = publication.metadata.title,
            author = author,
            href = href,
            identifier = publicationIdentifier,
            cover = cover,
            ext = ".$extension",
            progression = "{}"
        )

        return addBookToDatabase(book)
    }

    private suspend fun addBookToDatabase(book: Book, alertDuplicates: Boolean = true): Boolean {
        database.books.insert(book, allowDuplicates = !alertDuplicates)?.let { id ->
            book.id = id
            books.add(0, book)
            return true
        }

        return if (alertDuplicates && confirmAddDuplicateBook())
            addBookToDatabase(book, alertDuplicates = false)
        else
            false
    }


    // 重复添加验证
    private suspend fun confirmAddDuplicateBook(): Boolean = suspendCoroutine { cont ->
        MaterialDialog(activity).show {
            cancelOnTouchOutside(false)
            title(R.string.dialog_add_title)
            message(text = "这本书已经存在了, 是否再次添加?")
            positiveButton(R.string.dialog_add) { thisDialog ->
                thisDialog.dismiss()
                cont.resume(true)
            }
            negativeButton(R.string.dialog_cancel) { thisDialog ->
                thisDialog.dismiss()
                cont.resume(false)
            }
        }
    }

    private suspend fun Uri.copyToTempFile(): File? = tryOrNull {
        val filename = UUID.randomUUID().toString()
        val mediaType = MediaType.ofUri(this, activity.contentResolver)
        val path = "$R2DIRECTORY$filename.${mediaType?.fileExtension ?: "tmp"}"
        ContentResolverUtil.getContentInputStream(activity, this, path)
        return File(path)
    }

    private fun prepareToServe(publication: Publication, asset: PublicationAsset): URL? {
        val userProperties =
            activity.applicationContext.filesDir.path + "/" + Injectable.Style.rawValue + "/UserProperties.json"
        return server.addPublication(publication, userPropertiesFile = File(userProperties))
    }

    private suspend fun InputStream.copyToTempFile(): File? = tryOrNull {
        val filename = UUID.randomUUID().toString()
        File(R2DIRECTORY + filename)
            .also { toFile(it.path) }
    }


    private fun startServer() {
        if (!server.isAlive) {
            try {
                server.start()
            } catch (e: IOException) {
                // do nothing
                if (BuildConfig.DEBUG) Timber.e(e)
            }
            if (server.isAlive) {
                server.loadCustomResource(activity.assets.open("action/paragraph.js"), "paragraph.js", Injectable.Script)
                server.loadCustomResource(activity.assets.open("search/mark.js"), "mark.js", Injectable.Script)
                server.loadCustomResource(activity.assets.open("search/search.js"), "search.js", Injectable.Script)
                server.loadCustomResource(activity.assets.open("search/mark.css"), "mark.css", Injectable.Style)
            }
        }
    }

    private fun stopServer() {
        if (server.isAlive) {
            server.stop()
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startServer()
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        //EventBus.getDefault().unregister(this)
        stopServer()
    }
}