package com.fauzi.tweaklauncher

import android.content.Context
import android.net.Uri
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/** Pembaca RAR (termasuk RAR5) memakai 7-Zip JBinding. Dipakai saat junrar menolak RAR5. */
object Rar7z {
    fun extract(ctx: Context, uri: Uri, wanted: (String) -> Boolean, sink: (String, ByteArray) -> Unit) {
        val tmp = File(ctx.cacheDir, "rar5.tmp")
        tmp.delete()
        val ins = ctx.contentResolver.openInputStream(uri) ?: throw IllegalStateException("berkas tidak terbaca")
        ins.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
        var raf: RandomAccessFile? = null
        var arc: IInArchive? = null
        try {
            raf = RandomAccessFile(tmp, "r")
            arc = SevenZip.openInArchive(null, RandomAccessFileInStream(raf))
            for (i in 0 until arc.numberOfItems) {
                if (arc.getProperty(i, PropID.IS_FOLDER) == true) continue
                val path = (arc.getProperty(i, PropID.PATH) as? String ?: continue).replace('\\', '/')
                if (!wanted(path)) continue
                val size = (arc.getProperty(i, PropID.SIZE) as? Number)?.toLong() ?: 0L
                if (size > 2_000_000L) continue
                val bos = ByteArrayOutputStream()
                arc.extractSlow(i, ISequentialOutStream { d ->
                    bos.write(d)
                    d.size
                })
                sink(path, bos.toByteArray())
            }
        } finally {
            try {
                arc?.close()
            } catch (e: Exception) {
            }
            try {
                raf?.close()
            } catch (e: Exception) {
            }
            tmp.delete()
        }
    }
}
