package ee.episodeday.app

data class Show(val id: Int, val name: String)
data class Episode(
    val showId: Int,
    val showName: String,
    val name: String,
    val season: Int,
    val number: Int,
    val airdate: String,
    val summary: String = ""
)
