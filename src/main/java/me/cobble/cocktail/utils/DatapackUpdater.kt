package me.cobble.cocktail.utils

import it.unimi.dsi.fastutil.io.FastBufferedOutputStream
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
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import me.cobble.cocktail.Cocktail
import me.cobble.cocktail.config.Config
import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath
import org.slf4j.Logger

class DatapackUpdater(server: MinecraftServer) {
  private val datapackPath: Path = server.getSavePath(WorldSavePath.DATAPACKS)
  private val log: Logger = Cocktail.logger
  private val client: HttpClient =
    HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .followRedirects(HttpClient.Redirect.ALWAYS)
      .connectTimeout(Duration.ofSeconds(10))
      .build()

  /** Runs the datapack updater process. */
  fun run() {
    val config = Config.get()

    // Check if datapack downloader is enabled
    if (!config.packDownloader.enabled) {
      log.warn("Datapack downloader is disabled! Enable it in cocktail/config.jsonc")
      return
    }

    log.info("Starting datapack updater...")

    // Process each datapack URL
    config.datapackUrls.entries.forEach { (packName, packUrl) ->
      log.info("Downloading [{}]", packName)
      // Create HTTP request for the pack URL
      val request =
        runCatching {
            HttpRequest.newBuilder()
              .uri(URI.create(packUrl))
              .GET()
              .timeout(Duration.ofSeconds(30))
              .build()
          }
          .getOrNull()

      if (request == null) {
        log.error("Failed to create HTTP request for [{}]", packName)
        return
      }
      // Define the file path to save the downloaded pack
      val packFilePath = datapackPath.resolve("$packName.zip")

      // Send the HTTP request and get the response
      client
        .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
        .thenApply {
          // Download the pack file
          FastBufferedOutputStream(FileOutputStream(packFilePath.toFile())).use { fos ->
            it.body().transferTo(fos)
          }
          log.info("Downloaded successfully!")
        }
        .join()

      // Check for nested folders in the downloaded pack
      if (config.packDownloader.checkForNestedFolders) {
        log.info("Initializing nested folder check for [{}]", packName)
        log.info("Unzipping {}", packName)
        val tempDir = unzip(packFilePath)

        // Move resources to the root directory
        log.info("Moving resources to root...")
        moveResourcesIfFound(tempDir, datapackPath)

        // Delete nested folders if needed
        log.info("Deleting nested folders (if needed)...")
        cleanNoNestingCopies(tempDir)

        // Move contents to the root directory
        log.info("Moving contents to root...")
        moveContentsToParent(tempDir, datapackPath.resolve(packName))

        log.info("Check completed!")
      }
    }
  }

  /**
   * Unzips a pack zip file to a temporary directory.
   *
   * @param packZip The path to the pack zip file.
   * @return The path to the temporary directory where the pack zip file was unzipped.
   */
  private fun unzip(packZip: Path): Path {
    // Extract the pack name from the zip file name
    val packName = packZip.nameWithoutExtension

    // Create the temporary directory path
    val destinationDir = datapackPath.resolve("$packName-temp")

    runCatching {
        // Open the zip file and unzip it to the temporary directory
        ZipInputStream(FileInputStream(packZip.toFile())).use { zis ->
          unzipFile(destinationDir, zis)
        }
      }
      .getOrElse {
        // Log an error if an I/O exception occurs while unzipping the file
        log.error("An error occurred while unzipping {}", packName, it)
      }

    // Return the path to the temporary directory
    return destinationDir
  }

  /**
   * Unzips a file to a destination directory.
   *
   * @param destinationDir The directory to unzip the file to.
   * @param zis The ZipInputStream containing the file to unzip.
   * @throws IOException If an I/O error occurs while unzipping the file.
   */
  @Throws(IOException::class)
  private fun unzipFile(destinationDir: Path, zis: ZipInputStream) {
    val buffer = ByteArray(4096)

    var entry = zis.nextEntry
    while (entry != null) {
      val newEntry = newFile(destinationDir.toFile(), entry)

      if (entry.isDirectory) {
        if (!newEntry.isDirectory && !newEntry.mkdirs())
          throw IOException("Failed to create directory $newEntry")
      } else {
        val parentDir = newEntry.parentFile
        if (!parentDir.isDirectory && !parentDir.mkdirs())
          throw IOException("Failed to create directory $parentDir")

        FastBufferedOutputStream(FileOutputStream(newEntry)).use { outputStream ->
          var length: Int
          while ((zis.read(buffer).also { length = it }) > 0) {
            outputStream.write(buffer, 0, length)
          }
        }
      }

      entry = zis.nextEntry
    }
  }

  /**
   * Removes duplicate folder if the temporary directory is not nested.
   *
   * @param tempDir The temporary directory to clean up.
   */
  private fun cleanNoNestingCopies(tempDir: Path) {
    // Get the list of files in the temporary directory
    Files.list(tempDir).parallel().use { dirs ->
      if (dirs.count() <= 1) {
        return
      }
    }

    Files.list(datapackPath).use { allPacks ->
      val fileName = tempDir.nameWithoutExtension
      val compressedFileName = fileName.dropLast(5) + ".zip"
      log.info("Cleaning up {}", compressedFileName)
      if (allPacks.noneMatch { f: Path -> f.name == compressedFileName }) {
        return
      }
      Files.walk(tempDir).use { walk ->
        // Sort the files in reverse order and delete each file
        walk
          .sorted(Comparator.reverseOrder())
          .map { obj: Path -> obj.toFile() }
          .forEach { obj: File -> obj.delete() }
      }
    }
  }

  @Throws(IOException::class)
  private fun moveResourcesIfFound(targetFile: Path, destinationDir: Path) {
    // Get the list of files in the directory
    val dirs = targetFile.toFile().listFiles() ?: return

    // Check if any files in the root directory contain "resources"
    for (file in dirs) {
      if (file.name.contains("resources")) {
        // Move the resources file out of the root directory
        val targetResourcesFile = targetFile.resolve(file.name)
        log.info("Moving {} to {}", targetResourcesFile, destinationDir)
        Files.move(
          targetResourcesFile,
          destinationDir.resolve(file.name),
          StandardCopyOption.REPLACE_EXISTING,
        )
      }
    }
  }

  /**
   * This method moves the contents of a directory to its parent directory. If the parent directory
   * already contains a directory, it will replace the old one. If the parent directory does not
   * contain a directory, it will create one.
   *
   * @param targetFile The file to be deleted if the contents of the directory cannot be moved.
   * @param destinationDir The directory whose contents will be moved to its parent directory.
   * @throws IOException If an I/O error occurs when deleting or moving files.
   */
  @Throws(IOException::class)
  private fun moveContentsToParent(targetFile: Path, destinationDir: Path) {
    // Get the list of files in the directory
    val dirs = targetFile.toFile().listFiles()

    // Check if the directory contains exactly one subdirectory
    if (dirs != null && dirs.size == 1 && dirs[0].isDirectory) {
      val path = dirs[0]
      val targetSubDir = datapackPath.resolve(path.name)

      Files.move(targetSubDir, destinationDir, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  @Throws(IOException::class)
  private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
    val file = File(destinationDir, zipEntry.name)
    val dirPath = destinationDir.canonicalPath
    val filePath = file.canonicalPath

    if (!filePath.startsWith(dirPath + File.separator)) {
      throw IOException("Entry is outside of the target dir: " + zipEntry.name)
    }
    return file
  }
}
