package com.example.pdfcircle

import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfButtonFormField
import com.itextpdf.io.image.ImageData
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfArray
import com.itextpdf.kernel.pdf.PdfResources
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.tagging.StandardRoles
import com.itextpdf.kernel.pdf.tagutils.AccessibilityProperties
import com.itextpdf.kernel.pdf.tagutils.DefaultAccessibilityProperties
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import com.itextpdf.kernel.pdf.xobject.PdfXObject
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.element.AbstractElement
import com.itextpdf.layout.element.ILeafElement
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.layout.LayoutArea
import com.itextpdf.layout.layout.LayoutContext
import com.itextpdf.layout.layout.LayoutResult
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.layout.renderer.AbstractRenderer
import com.itextpdf.layout.renderer.DrawContext
import com.itextpdf.layout.renderer.IRenderer
import com.itextpdf.layout.tagging.IAccessibleElement


class CustomButton(var button: PdfButtonFormField) : AbstractElement<CustomButton?>(),
    ILeafElement, IAccessibleElement {
    private var accessibilityProperties: DefaultAccessibilityProperties? = null
    private var caption: String? = null
    var image: ImageData? = null
    private var rect: Rectangle? = null
    private var borderColor = ColorConstants.BLACK
    var buttonBackgroundColor = ColorConstants.WHITE

    override fun makeNewRenderer(): IRenderer {
        return CustomButtonRenderer(this)
    }

    override fun getAccessibilityProperties(): AccessibilityProperties {
        if (accessibilityProperties == null) {
            accessibilityProperties = DefaultAccessibilityProperties(StandardRoles.FIGURE)
        }
        return accessibilityProperties!!
    }

    fun getCaption(): String {
        return if (caption == null) "" else caption!!
    }

    fun getBorderColor(): Color {
        return borderColor
    }

    fun setBorderColor(borderColor: Color) {
        this.borderColor = borderColor
    }
}

class CustomButtonRenderer(button: CustomButton?) : AbstractRenderer(button) {
    override fun layout(layoutContext: LayoutContext): LayoutResult {
        val area = layoutContext.area.clone()
        val layoutBox: Rectangle = area.bBox
        applyMargins(layoutBox, false)
        val modelButton = modelElement as CustomButton
        occupiedArea = LayoutArea(area.pageNumber, Rectangle(modelButton.button.widgets[0].rectangle.toRectangle()))
        val button = (getModelElement() as CustomButton).button
        button.widgets[0].rectangle = PdfArray(occupiedArea.bBox)
        return LayoutResult(LayoutResult.FULL, occupiedArea, null, null)
    }

    override fun draw(drawContext: DrawContext) {
        val modelButton = modelElement as CustomButton
        val rect: Rectangle = modelButton.button.widgets[0].rectangle.toRectangle()
        occupiedArea.bBox = rect
        super.draw(drawContext)
        val width = occupiedArea.bBox.width
        val height = occupiedArea.bBox.height
        val str = PdfStream()
        val canvas = PdfCanvas(str, PdfResources(), drawContext.document)
        val xObject = PdfFormXObject(Rectangle(0f, 0f, width, height))
        canvas.saveState().setStrokeColor(modelButton.getBorderColor()).setLineWidth(1f)
            .rectangle(0.0, 0.0, occupiedArea.bBox.width.toDouble(), occupiedArea.bBox.height.toDouble()).stroke()
            .setFillColor(modelButton.buttonBackgroundColor).rectangle(
                0.5,
                0.5,
                (occupiedArea.bBox.width - 1).toDouble(),
                (occupiedArea.bBox.height - 1).toDouble()
            ).fill().restoreState()
        val paragraph = Paragraph(modelButton.getCaption()).setFontSize(10f).setMargin(0f).setMultipliedLeading(1f)
//        Canvas(canvas, drawContext.document, Rectangle(0f, 0f, width, height)).showTextAligned(
        Canvas(canvas, Rectangle(0f, 0f, width, height)).showTextAligned(
            paragraph,
            1f,
            1f,
            TextAlignment.LEFT,
            VerticalAlignment.BOTTOM
        )
        val imageXObject = PdfImageXObject(modelButton.image)
        var imageWidth = imageXObject.width
        if (imageXObject.width > rect.width) {
            imageWidth = rect.width
        } else if (imageXObject.height > rect.height) {
            imageWidth *= (rect.height / imageXObject.height)
        }
        val rectangle: Rectangle =
            PdfXObject.calculateProportionallyFitRectangleWithWidth(imageXObject, 0.5f, 0.5f, imageWidth - 1)

        canvas.addXObjectFittedIntoRectangle(imageXObject, rectangle)
        val button = modelButton.button
        button.widgets[0].setNormalAppearance(xObject.pdfObject)
        xObject.pdfObject.outputStream.writeBytes(str.bytes)
        xObject.resources.addImage(imageXObject)
        PdfAcroForm.getAcroForm(drawContext.document, true).addField(button, drawContext.document.getPage(1))
    }

    override fun getNextRenderer(): IRenderer? {
        return null
    }
}