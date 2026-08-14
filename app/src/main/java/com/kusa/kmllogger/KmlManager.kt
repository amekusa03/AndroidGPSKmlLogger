package com.kusa.kmllogger

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KmlManager(private val context: Context) {

    private var currentFile: File? = null
    private val footer = "\n  </Document>\n</kml>"

    fun startNewLog(fileName: String? = null): File {
        val name = fileName?.takeIf { it.isNotBlank() } ?: SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "$name.kml")
        
        val header = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <name>$name</name>
        """.trimIndent()

        FileOutputStream(file).use { 
            it.write(header.toByteArray())
            it.write(footer.toByteArray())
        }
        
        currentFile = file
        return file
    }

    fun appendLocation(lat: Double, lng: Double, alt: Double) {
        val file = currentFile ?: run {
            Log.w("KmlManager", "No current file to append location")
            return
        }
        if (!file.exists()) {
            Log.e("KmlManager", "File does not exist: ${file.absolutePath}")
            return
        }

        Log.d("KmlManager", "Appending location to ${file.name}")

        val placemark = """
                <Placemark>
                  <TimeStamp><when>${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())}</when></TimeStamp>
                  <Point>
                    <coordinates>$lng,$lat,$alt</coordinates>
                  </Point>
                </Placemark>
        """.trimIndent()

        RandomAccessFile(file, "rw").use { raf ->
            val length = raf.length()
            // Seek back before the footer
            val footerBytes = footer.toByteArray()
            raf.seek(length - footerBytes.size)
            
            // Overwrite footer with new placemark + footer
            raf.write(placemark.toByteArray())
            raf.write(footerBytes)
        }
    }
    
    fun finishLog() {
        currentFile = null
    }
    
    fun getCurrentFilePath(): String? = currentFile?.absolutePath
}
