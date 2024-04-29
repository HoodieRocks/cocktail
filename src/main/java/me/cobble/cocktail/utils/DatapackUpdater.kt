package me.cobble.cocktail.utils

import me.cobble.cocktail.Cocktail
import me.cobble.cocktail.config.Config
import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath
import org.slf4j.Logger
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DatapackUpdater(server: MinecraftServer) {
  private val datapackPath: Path = server.getSavePath(WorldSavePath.DATAPACKS)
  private val log: Logger = Cocktail.logger
  private val client: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .followRedirects(HttpClient.Redirect.ALWAYS)
    .build()

  fun run() {
    val config = Config.get()

    if (!config.packDownloader.enabled) {
      log.warn("Datapack downloader is disabled! If you want to enable it, set 'pack-downloader.enabled' to true in config.jsonc")
      return
    }

    log.info("Starting datapack updater...")

    try {
      config.datapackUrls.entries.forEach { (packName, packUrl) ->

        log.info("Downloading [{}]", packName)
        val request = HttpRequest.newBuilder()
          .uri(URI.create(packUrl))
          .GET()
          .timeout(Duration.ofSeconds(30))
          .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        val packFilePath = datapackPath.resolve("$packName.zip")

        BufferedOutputStream(FileOutputStream(packFilePath.toFile())).use { fos ->
          response.body().transferTo(fos)
        }
        log.info("Downloaded successfully!")

        if (config.packDownloader.checkForNestedFolders) {
          log.info("Initalizing nested folder check for [{}]", packName)
          log.info("[NFC] Unzipping {}", packName)
          val tempDir = unzip(packFilePath)

          log.info("[NFC] Moving resources to root...")
          moveResourcesIfFound(tempDir, datapackPath)

          log.info("[NFC] Deleting nested folders (if needed)...")
          cleanNoNestingCopies(tempDir)

          log.info("[NFC] Moving contents to root...")
          moveContentsToParent(tempDir, datapackPath.resolve(packName))

          log.info("[NFC] Check completed!")
        }
      }
    } catch (e: IOException) {
      log.error("Failed to update datapacks", e)
    } catch (e: InterruptedException) {
      log.error("Failed to update datapacks", e)
    }
  }

  private fun unzip(packZip: Path): Path {
    val packName = packZip.toFile().name.substring(0, packZip.toFile().name.length - 4)
    val destDir = datapackPath.resolve("$packName-temp")

    try {
      ZipInputStream(FileInputStream(packZip.toFile())).use { zis ->
        unzipFile(destDir, zis)
      }
    } catch (e: IOException) {
      log.error("An error occurred while unzipping {}", packName, e)
    }
    return destDir
  }

  /**
   * Unzips a file to a destination directory.
   *
   * @param destDir The directory to unzip the file to.
   * @param zis     The ZipInputStream containing the file to unzip.
   * @throws IOException If an I/O error occurs while unzipping the file.
   */
  @Throws(IOException::class)
  fun unzipFile(destDir: Path, zis: ZipInputStream) {
    // Buffer for reading the zip file
    val buffer = ByteArray(4096)

    // Get the next entry in the zip file
    var ze = zis.nextEntry

    // Loop through each entry in the zip file
    while (ze != null) {
      // Create the new file in the destination directory
      val newFile = newFile(destDir.toFile(), ze)

      // If the entry is a directory
      if (ze.isDirectory) {
        // If the directory doesn't already exist, create it
        if (!newFile.isDirectory && !newFile.mkdirs()) {
          throw IOException("Failed to create directory $newFile")
        }
      } else {
        // If the entry is a file

        // Create the parent directory if it doesn't already exist

        val parent = newFile.parentFile
        if (!parent.isDirectory && !parent.mkdirs()) {
          throw IOException("Failed to create directory $parent")
        }

        BufferedOutputStream(FileOutputStream(newFile)).use { fos ->
          var len: Int
          while ((zis.read(buffer).also { len = it }) > 0) {
            fos.write(buffer, 0, len)
          }
        }
      }

      // Get the next entry in the zip file
      ze = zis.nextEntry
    }
  }

  /**
   * Removes duplicate folder if the temporary directory is not nested.
   *
   * @param tempDir The temporary directory to clean up.
   */
  private fun cleanNoNestingCopies(tempDir: Path) {
    // Get the list of files in the temporary directory
    try {
      Files.list(tempDir).use { dirs ->
        if (dirs == null || dirs.count() <= 1) {
          return
        }
        Files.list(datapackPath).use { allPacks ->
          val fileName = tempDir.fileName.toString()
          val compressedFileName = fileName.substring(0, fileName.length - 5) + ".zip"
          log.info("Cleaning up {}", compressedFileName)
          if (allPacks.noneMatch { f: Path -> f.fileName.toString() == compressedFileName }) {
            return
          }
          Files.walk(tempDir).use { walk ->
            // Sort the files in reverse order and delete each file
            walk.sorted(Comparator.reverseOrder())
              .map { obj: Path -> obj.toFile() }
              .forEach { obj: File -> obj.delete() }
          }
        }
      }
    } catch (e: IOException) {
      // Handle the exception
      log.error("An error occurred while cleaning up", e)
    }
  }

  @Throws(IOException::class)
  fun moveResourcesIfFound(targetFile: Path, destDir: Path) {
    // Get the list of files in the directory

    val dirs = targetFile.toFile().listFiles() ?: return

    // Check if any files in the root directory contain "resources"
    for (file in dirs) {
      if (file.name.contains("resources")) {
        // Move the resources file out of the root directory
        val targetResourcesFile = targetFile.resolve(file.name)
        log.info("Moving {} to {}", targetResourcesFile, destDir)
        Files.move(targetResourcesFile, destDir.resolve(file.name), StandardCopyOption.REPLACE_EXISTING)
      }
    }
  }

  /**
   * This method moves the contents of a directory to its parent directory.
   * If the parent directory already contains a directory, it will replace the old one.
   * If the parent directory does not contain a directory, it will create one.
   *
   * @param targetFile The file to be deleted if the contents of the directory cannot be moved.
   * @param destDir    The directory whose contents will be moved to its parent directory.
   * @throws IOException If an I/O error occurs when deleting or moving files.
   */
  @Throws(IOException::class)
  private fun moveContentsToParent(targetFile: Path, destDir: Path) {
    // Get the list of files in the directory

    val dirs = targetFile.toFile().listFiles()

    // Check if the directory contains exactly one subdirectory
    if (dirs != null && dirs.size == 1 && dirs[0].isDirectory) {
      val path = dirs[0]
      val targetSubDir = datapackPath.resolve(path.name)

      Files.move(targetSubDir, destDir, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  companion object {
    @Throws(IOException::class)
    private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
      val destFile = File(destinationDir, zipEntry.name)
      val destDirPath = destinationDir.canonicalPath
      val destFilePath = destFile.canonicalPath

      if (!destFilePath.startsWith(destDirPath + File.separator)) {
        throw IOException("Entry is outside of the target dir: " + zipEntry.name)
      }
      return destFile
    }
  }
}
