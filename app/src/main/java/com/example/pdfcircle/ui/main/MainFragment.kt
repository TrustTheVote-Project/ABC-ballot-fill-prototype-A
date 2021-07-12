package com.example.pdfcircle.ui.main

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pdfcircle.CustomButton
import com.example.pdfcircle.R
import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfButtonFormField
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import kotlinx.android.synthetic.main.main_fragment.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


//private const val CURRENT_PAGE_INDEX_KEY = "com.example.pdf.openDocument.state.CURRENT_PAGE_INDEX_KEY"
private const val TAG = "OpenDocumentFragment"

private const val FILENAME = "part1"
private const val EXT = ".pdf"
//private const val FILENAME = "awesomeBallot.pdf"
//private const val FILENAME = "awesomeBallot (1).pdf"
//private const val INITIAL_PAGE_INDEX = 0

class MainFragment : Fragment() {

//    private lateinit var viewModel: MainViewModel

//    private lateinit var pdfRenderer: PdfRenderer
//    private lateinit var currentPage: PdfRenderer.Page
//    private var currentPageNumber: Int = INITIAL_PAGE_INDEX

    private var bitmap: Bitmap? = null
    private var pdfFile: File? = null
    private var pageNumber = 0

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_fill_save.setOnClickListener {
            setAllAndSaveInExternalDir(true)
        }

        btn_clear_save.setOnClickListener {
            setAllAndSaveInExternalDir(false)
        }
    }

    private fun setAllAndSaveInExternalDir(check: Boolean = true) {
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED != state) {

            Toast.makeText(requireContext(), "it isn't mounted", Toast.LENGTH_LONG).show()
            //If it isn't mounted - we can't write into it.
            return
        }

        val outFile = File(requireActivity().getExternalFilesDir(null), FILENAME + "_out${EXT}")
        val reader = PdfReader(pdfFile)
        val writer = PdfWriter(outFile)

        val pdfDoc = PdfDocument(reader, writer)
        val form = PdfAcroForm.getAcroForm(pdfDoc, true)
        val fields = form.formFields

        val d = Document(pdfDoc)

        val imageBytes = requireActivity().assets.open("ic_fill.png").readBytes()
        val imageData = ImageDataFactory.create(imageBytes)

        val selFields = fields.filter { field ->
            if (field.value == null) return@filter false
            if (field.value.formType == null) return@filter false
            if (PdfName.Btn != field.value.formType) return@filter false
            true
        }

        if (check)
            selFields.forEach { field ->
                field.value?.setValue("Yes")

                val ad = CustomButton(field.value as PdfButtonFormField)

                ad.image = imageData
                ad.setBorderColor(com.itextpdf.kernel.colors.Color.convertRgbToCmyk(DeviceRgb(255, 255, 255)))

                form.removeField(field.value?.fieldName.toString())

                d.add(Paragraph().add(ad))
            }
        else {
            selFields.forEach {
                it.value?.setValue("Off")
            }
        }
        pdfDoc.close()
        d.close()


        outFile.copyTo(pdfFile!!, true)
        renderPdf()
//        pdfView.fromFile(outFile)
//            .enableAnnotationRendering(true)
//            .spacing(10)
//            .load()

        Toast.makeText(requireContext(), "File saved in ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
    }

    private fun imageTapped(relativeX: Float, relativeY: Float) {
        val tempFile = File(context!!.cacheDir, "temp.pdf")
        val reader = PdfReader(pdfFile)
        val writer = PdfWriter(tempFile)

//        reader.isCloseStream = false
//        writer.isCloseStream = false

        val pdfDoc = PdfDocument(reader, writer)

        val size = pdfDoc.getPage(1).pageSize

        val x = relativeX * size.width
        val y = size.height - relativeY * size.height

        val form = PdfAcroForm.getAcroForm(pdfDoc, true)
        val fields = form.formFields

        Log.d(TAG, "size " + fields.size.toString())

        val field = fields.firstNotNullOfOrNull {
            it.takeIf { f ->
                if (f.value == null) return@takeIf false
                if (f.value.formType == null) return@takeIf false
                if (PdfName.Btn != f.value.formType) return@takeIf false

                val opt = f.value.widgets[0].rectangle
                val array = opt.toFloatArray()
                //нижнего левого и верхнего правого
                //[ llx, lly, urx, ury]

                Log.d(
                    TAG, f.value?.fieldName.toString() +
                            " " + array[0] + " " + array[1] + " " + array[2] + " " + array[3]
                )
                Log.d(TAG, f.value.valueAsString)
                return@takeIf x > array[0] && x < array[2] && y > array[1] && y < array[3]
            }
        }

//        field?.value?.setCheckType(PdfFormField.TYPE_CIRCLE)
//        field?.value?.regenerateField()

        if (field != null) {
//            val a = field.value.valueAsString

            if (field.value == null || field.value.valueAsString == "Off") {
                field.value?.setValue("Yes")

                val ad = CustomButton(field.value as PdfButtonFormField)

                val imageBytes = requireActivity().assets.open("ic_fill.png").readBytes()
                val imageData = ImageDataFactory.create(imageBytes)

                ad.image = imageData

                ad.setBorderColor(com.itextpdf.kernel.colors.Color.convertRgbToCmyk(DeviceRgb(255, 255, 255)))

                form.removeField(field.value?.fieldName.toString())

                val d = Document(pdfDoc)
                d.add(Paragraph().add(ad))
                d.close()
            } else
                field.value?.setValue("Off")
        }
        pdfDoc.close()

        if (field != null) {
            tempFile.copyTo(pdfFile!!, true)
            renderPdf()
        }

        tempFile.deleteOnExit()
    }

    override fun onStart() {
        super.onStart()
        try {
            openPdf()
        } catch (ioException: IOException) {
            Log.d(TAG, "Exception opening document", ioException)
        }
    }

    @Throws(IOException::class)
    fun openPdf() {
//        pdfFile = File(requireActivity().getExternalFilesDir(null), FILENAME + "_out${EXT}")
        pdfFile = File(context!!.cacheDir, FILENAME + EXT)
        copyToCache(pdfFile, FILENAME + EXT)
        renderPdf()
    }

    private fun renderPdf() {
        val cx = pdfView.currentXOffset
        val cy = pdfView.currentYOffset
        val z = pdfView.zoom

        pdfView.fromFile(pdfFile)
            .defaultPage(pageNumber)
            .enableAnnotationRendering(true)
            .spacing(10) // in dp
            .onTap { e ->
                val x = pdfView.toRealScale(e.x - pdfView.currentXOffset)
                val y = pdfView.toRealScale(e.y - pdfView.currentYOffset)

                imageTapped(x / pdfView.optimalPageWidth, y / pdfView.optimalPageHeight)

                true
            }
            .onLoad {
//                if (z != 0f) {
//                    pdfView.zoomTo(z)
//                    pdfView.moveTo(cx, cy)
//                }
            }
            .load()
    }

    @Throws(IOException::class)
    fun copyToCache(file: File?, fileName: String) {
        if (!file?.exists()!!) {

            val input: InputStream = context!!.assets.open(fileName)
            val output = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var size: Int

            while (input.read(buffer).also { size = it } != -1) {
                output.write(buffer, 0, size)
            }

            input.close()
            output.close()
        }
    }
}