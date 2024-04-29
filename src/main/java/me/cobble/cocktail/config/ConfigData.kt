package me.cobble.cocktail.config

data class ConfigData(
  val configVersion: Int,
  val packDownloader: PackDownloaderData,
  val datapackUrls: Map<String, String>
) {

  inner class PackDownloaderData(
    val enabled: Boolean,
    val checkForNestedFolders: Boolean
  )
}
