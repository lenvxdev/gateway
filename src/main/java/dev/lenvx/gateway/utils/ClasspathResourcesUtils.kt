package dev.lenvx.gateway.utils

import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import java.util.zip.ZipFile

object ClasspathResourcesUtils {

	@JvmStatic
	fun getResources(pattern: Pattern): Collection<String> {
		val retval = mutableListOf<String>()
		val classPath = System.getProperty("java.class.path", ".")
		val classPathElements = classPath.split(File.pathSeparator)
		for (element in classPathElements) {
			retval.addAll(getResources(element, pattern))
		}
		return retval
	}

	private fun getResources(element: String, pattern: Pattern): Collection<String> {
		val retval = mutableListOf<String>()
		val file = File(element)
		if (file.isDirectory) {
			retval.addAll(getResourcesFromDirectory(file, pattern))
		} else if (file.exists()) {
			retval.addAll(getResourcesFromJarFile(file, pattern))
		}
		return retval
	}

	private fun getResourcesFromJarFile(file: File, pattern: Pattern): Collection<String> {
		val retval = mutableListOf<String>()
		try {
			ZipFile(file).use { zf ->
				val e = zf.entries()
				while (e.hasMoreElements()) {
					val ze = e.nextElement()
					val fileName = ze.name
					if (pattern.matcher(fileName).matches()) {
						retval.add(fileName)
					}
				}
			}
		} catch (e: IOException) {
		}
		return retval
	}

	private fun getResourcesFromDirectory(directory: File, pattern: Pattern): Collection<String> {
		val retval = mutableListOf<String>()
		val fileList = directory.listFiles() ?: return retval
		for (file in fileList) {
			if (file.isDirectory) {
				retval.addAll(getResourcesFromDirectory(file, pattern))
			} else {
				try {
					val fileName = file.canonicalPath
					if (pattern.matcher(fileName).matches()) {
						retval.add(fileName)
					}
				} catch (e: IOException) {
					throw Error(e)
				}
			}
		}
		return retval
	}
}

