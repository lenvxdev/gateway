package dev.lenvx.gateway.utils

import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

object ImageUtils {

	@JvmStatic
	@Throws(IOException::class)
	fun imgToBase64String(img: RenderedImage, formatName: String): String {
		val out = ByteArrayOutputStream()
		ImageIO.write(img, formatName, out)
		return Base64.getEncoder().encodeToString(out.toByteArray())
	}

}

