package com.fauzi.tweaklauncher

import android.content.Context
import android.net.Uri
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
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
            for (item in arc.simpleInArchive.archiveItems) {
                if (item.isFolder) continue
                val path = (item.path ?: continue).replace('\\', '/')
                if (!wanted(path)) continue
                if ((item.size ?: 0L) > 2_000_000L) continue
                val bos = ByteArrayOutputStream()
                item.extractSlow(ISequentialOutStream { d ->
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
