package com.seoul.fmonair.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object StationRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    val defaultStations = listOf(
        RadioStation("kbs_1", "KBS 1라디오", 97.3, StationCategory.KBS, StreamType.KBS, channelCode = "21", description = "종합 뉴스 & 시사"),
        RadioStation("kbs_2", "KBS 해피FM", 106.1, StationCategory.KBS, StreamType.KBS, channelCode = "22", description = "대중가요 & 오락"),
        RadioStation("kbs_cool", "KBS 쿨FM", 89.1, StationCategory.KBS, StreamType.KBS, channelCode = "25", description = "K-POP & 라디오 쇼"),
        RadioStation("kbs_classic", "KBS 클래식FM", 93.1, StationCategory.KBS, StreamType.KBS, channelCode = "24", description = "정통 클래식 & 국악"),
        RadioStation("mbc_sfm", "MBC 표준FM", 95.9, StationCategory.MBC, StreamType.MBC, channelCode = "sfm", description = "종합 정보 & 교양"),
        RadioStation("mbc_fm4u", "MBC FM4U", 91.9, StationCategory.MBC, StreamType.MBC, channelCode = "mfm", description = "음악 전문 채널"),
        RadioStation("sbs_power", "SBS 파워FM", 107.7, StationCategory.SBS, StreamType.SBS, channelCode = "powerpc/powerfm", description = "최신 음악 & 종합 엔터"),
        RadioStation("sbs_love", "SBS 러브FM", 103.5, StationCategory.SBS, StreamType.SBS, channelCode = "lovepc/lovefm", description = "가요 & 생활 시사"),
        RadioStation("ebs_fm", "EBS FM", 104.5, StationCategory.CURRENT_EVENTS, StreamType.DIRECT, directUrl = "https://ebsonair.ebs.co.kr/fmradiofamilypc/familypc1m/playlist.m3u8", description = "어학 & 교육"),
        RadioStation("tbs_fm", "TBS FM", 95.1, StationCategory.CURRENT_EVENTS, StreamType.DIRECT, directUrl = "https://cdnfm.tbs.seoul.kr/tbs/_definst_/tbs_fm_web_360.smil/playlist.m3u8", description = "수도권 교통 & 시민 생활"),
        RadioStation("tbs_efm", "TBS eFM", 101.3, StationCategory.CURRENT_EVENTS, StreamType.DIRECT, directUrl = "https://cdnefm.tbs.seoul.kr/tbs/_definst_/tbs_efm_web_360.smil/playlist.m3u8", description = "영어 & 다국어 전문"),
        RadioStation("ytn_fm", "YTN 라디오", 94.5, StationCategory.CURRENT_EVENTS, StreamType.DIRECT, directUrl = "https://radiolive.ytn.co.kr/radio/_definst_/20211118_fmlive/playlist.m3u8", description = "24시간 뉴스 전문"),
        RadioStation("gugak_fm", "국악방송 FM", 99.1, StationCategory.MUSIC_RELIGION, StreamType.DIRECT, directUrl = "https://mgugaklive.nowcdn.co.kr/gugakradio/gugakradio.stream/playlist.m3u8", description = "전통 국악 & 한국 음악"),
        RadioStation("cbs_sfm", "CBS 표준FM", 98.1, StationCategory.MUSIC_RELIGION, StreamType.DIRECT, directUrl = "https://maac.cbs.co.kr/cbs981/_definst_/cbs981.stream/playlist.m3u8", description = "보도 & 선교"),
        RadioStation("cbs_mfm", "CBS 음악FM", 93.9, StationCategory.MUSIC_RELIGION, StreamType.DIRECT, directUrl = "https://maac.cbs.co.kr/cbs939/_definst_/cbs939.stream/playlist.m3u8", description = "팝 & 가요 명곡"),
        RadioStation("febc_fm", "FEBC 극동방송", 106.9, StationCategory.MUSIC_RELIGION, StreamType.DIRECT, directUrl = "https://mlive1.febc.net/live/seoulfm/playlist.m3u8", description = "기독교 복음 방송"),
        RadioStation("bbs_fm", "BBS 불교방송", 101.9, StationCategory.MUSIC_RELIGION, StreamType.DIRECT, directUrl = "https://bbslive.clouducs.com/bbsradio-live/livestream/playlist.m3u8", description = "불교 교양 & 음악"),
        RadioStation("cpbc_fm", "CPBC 평화방송", 105.3, StationCategory.MUSIC_RELIGION, StreamType.DIRECT, directUrl = "https://onair.cpbc.co.kr/live/fm/playlist.m3u8", description = "가톨릭 평화 방송")
    )

    suspend fun resolvePlayableStreamUrl(station: RadioStation): String? = withContext(Dispatchers.IO) {
        try {
            when (station.type) {
                StreamType.DIRECT -> station.directUrl
                StreamType.SBS -> {
                    val endpoint = "https://apis.sbs.co.kr/play-api/1.0/livestream/\${station.channelCode}?protocol=hls&ssl=Y"
                    val request = Request.Builder().url(endpoint).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) response.body?.string()?.trim() else null
                    }
                }
                StreamType.MBC -> {
                    val endpoint = "https://sminiplay.imbc.com/aacplay.ashx?agent=webapp&channel=\${station.channelCode}"
                    val request = Request.Builder().url(endpoint).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) response.body?.string()?.trim() else null
                    }
                }
                StreamType.KBS -> {
                    val endpoint = "https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/\${station.channelCode}"
                    val request = Request.Builder().url(endpoint).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val jsonStr = response.body?.string() ?: return@use null
                            val json = JSONObject(jsonStr)
                            val channelItem = json.getJSONArray("channel_item").getJSONObject(0)
                            channelItem.getString("service_url")
                        } else null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
