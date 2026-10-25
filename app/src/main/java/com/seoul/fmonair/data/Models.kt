package com.seoul.fmonair.data

enum class StreamType {
    DIRECT, KBS, MBC, SBS
}

enum class StationCategory(val label: String) {
    ALL("전체"),
    FAVORITES("즐겨찾기"),
    KBS("KBS"),
    MBC("MBC"),
    SBS("SBS"),
    CURRENT_EVENTS("시사/교육"),
    MUSIC_RELIGION("음악/종교")
}

data class RadioStation(
    val id: String,
    val name: String,
    val frequency: Double,
    val category: StationCategory,
    val type: StreamType,
    val directUrl: String = "",
    val channelCode: String = "",
    val description: String = "",
    var isFavorite: Boolean = false
)

data class RecognizedSong(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val artist: String,
    val album: String = "",
    val stationName: String,
    val frequency: Double,
    val timeFormatted: String,
    val lyrics: String = ""
)

data class LyricsLine(
    val timeMs: Long,
    val text: String
)
