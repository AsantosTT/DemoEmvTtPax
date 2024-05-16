package com.techun.demoemvttpax.domain.usecases

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.pax.gl.page.IPage
import com.pax.gl.page.PaxGLPage
import com.techun.demoemvttpax.R
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetBitmapVoucherUseCase @Inject constructor(
    @ApplicationContext private val context: Context, private val glPage: PaxGLPage
) {

    suspend operator fun invoke(voucher: String): Flow<DataState<Bitmap>> = flow {
        try {
            val page = glPage.createPage()
            val unit = page.createUnit()
            unit.align = IPage.EAlign.CENTER
            unit.text = "GLiPaxGlPage"
            val icon = BitmapFactory.decodeResource(context.resources, R.drawable.isotipo_tt)
            val width = icon.width
            val height = icon.height
            val scaleWidth = (380f / width)
            val scaleHeight = (110f / height)
            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            val resizedBitmap = Bitmap.createBitmap(icon, 0, 0, width, height, matrix, false)

            page.addLine().addUnit(resizedBitmap, IPage.EAlign.CENTER)

            val vComercio = voucher.split("|").toTypedArray()

            for (i in vComercio.indices) {
                val currentLine = vComercio[i]
                val subLine = currentLine.split("<").toTypedArray()

                if (currentLine == "") page.addLine().addUnit("\n", 10)

                when {
                    (subLine.size == 1) -> {
                        if (subLine[0].isNotBlank()) {
                            println("Level [${subLine.size}] = $currentLine")
                            if (currentLine == "") page.addLine().addUnit("\n", 10)
                            else page.addLine().addUnit(
                                currentLine, Utils.FONT_NORMAL, IPage.EAlign.CENTER
                            )
                        }
                    }

                    (subLine.size == 2) -> {
                        println("Level [${subLine.size}]")
                        val tag = subLine[0].trim()
                        val value = subLine[1].trim()
                        val totalLength = tag.length + value.length

                        if (totalLength < 34) {
                            page.addLine().addUnit(
                                tag,
                                Utils.FONT_NORMAL,
                                IPage.EAlign.LEFT,
                                IPage.ILine.IUnit.TEXT_STYLE_NORMAL,
                                if (tag.length > 16) 2f else 1f
                            ).addUnit(
                                value,
                                Utils.FONT_NORMAL,
                                IPage.EAlign.RIGHT,
                                IPage.ILine.IUnit.TEXT_STYLE_NORMAL,
                                if (value.length > 12) 2f else 1f
                            )
                        } else {
                            page.addLine().addUnit(
                                tag,
                                Utils.FONT_NORMAL,
                                IPage.EAlign.LEFT,
                                IPage.ILine.IUnit.TEXT_STYLE_NORMAL
                            )
                            page.addLine().addUnit(
                                value,
                                Utils.FONT_NORMAL,
                                IPage.EAlign.RIGHT,
                                IPage.ILine.IUnit.TEXT_STYLE_NORMAL
                            )
                        }
                    }

                    (subLine.size == 3) -> {
                        println("Level [${subLine.size}]")
                        page.addLine().addUnit(subLine[0], Utils.FONT_NORMAL, IPage.EAlign.LEFT)
                            .addUnit(subLine[1], Utils.FONT_NORMAL, IPage.EAlign.LEFT)
                            .addUnit(subLine[2], Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
                    }

                    else -> {
                        println("Level [${subLine.size}]")
                        page.addLine().addUnit(
                            currentLine, Utils.FONT_NORMAL, IPage.EAlign.CENTER
                        )
                    }
                }
            }
            page.addLine().addUnit("\n\n", Utils.FONT_NORMAL)
            val widthFinal = 384
            val imgVoucher = glPage.pageToBitmap(page, widthFinal)


            emit(DataState.Success(imgVoucher))
            emit(DataState.Finished)
        } catch (ex: Exception) {
            emit(DataState.Error(ex))
            emit(DataState.Finished)
        }
    }
}