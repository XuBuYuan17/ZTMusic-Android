package com.zheting.mobile.playback

import com.zheting.mobile.core.model.Song
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 协调器核心行为的 JVM 单测：候选顺序 / 空 URL / 换源恢复 / 有限次数 / 切歌取消 / 迟到响应隔离。 */
class SongLoadCoordinatorTest {

    private val songA = Song(id = "1", name = "A")
    private val songB = Song(id = "2", name = "B")

    /** 即时决策的假装载器：记录调用，按队列依次返回 outcome。 */
    private class FakeSink : MediaSink {
        val urls = mutableListOf<String>()
        val trials = mutableListOf<Boolean>()
        val positions = mutableListOf<Long>()
        var exhaustedSong: Song? = null
        private val queue = ArrayDeque<LoadOutcome>()

        fun feed(vararg outcomes: LoadOutcome) = queue.addAll(outcomes.toList()).let { this }

        override suspend fun load(url: String, song: Song, isTrial: Boolean, startPositionMs: Long): LoadOutcome {
            urls += url
            trials += isTrial
            positions += startPositionMs
            return queue.removeFirst()
        }

        override suspend fun onExhausted(song: Song) {
            exhaustedSong = song
        }
    }

    /** 受控假 resolver：每次调用挂起在给定 gate 上，测试代码决定何时放行。 */
    private class GateResolver(
        private val gates: List<CompletableDeferred<List<AudioCandidate>>>,
    ) : PlaybackUrlResolver {
        private var i = 0
        override suspend fun resolve(song: Song): List<AudioCandidate> = gates[i++].await()
    }

    private fun coordinator(
        resolver: PlaybackUrlResolver,
        sink: MediaSink,
        scope: CoroutineScope,
    ) = SongLoadCoordinator(resolver, sink, scope)

    @Test
    fun `候选顺序被遵守且换源沿用失败位置`() = runTest {
        val sink = FakeSink().feed(LoadOutcome.Failed(1_000), LoadOutcome.Success)
        val resolver = object : PlaybackUrlResolver {
            override suspend fun resolve(song: Song) = listOf(
                AudioCandidate("", isTrial = false), // 空 URL：跳过，不触发装载
                AudioCandidate("http://a", isTrial = true),
                AudioCandidate("http://b", isTrial = false),
            )
        }
        coordinator(resolver, sink, this).playSong(songA)
        advanceUntilIdle()

        // 第一个可播地址失败（失败时已到 1s），第二个成功 → 顺序 + 进度恢复
        assertEquals(listOf("http://a", "http://b"), sink.urls)
        assertEquals(listOf(0L, 1_000L), sink.positions)
        assertEquals(listOf(true, false), sink.trials)
        assertTrue(sink.exhaustedSong == null)
    }

    @Test
    fun `全是空候选直接判耗尽且不装载`() = runTest {
        val sink = FakeSink()
        val resolver = object : PlaybackUrlResolver {
            override suspend fun resolve(song: Song) = listOf(
                AudioCandidate(""),
                AudioCandidate(""),
            )
        }
        coordinator(resolver, sink, this).playSong(songA)
        advanceUntilIdle()

        assertTrue(sink.urls.isEmpty())
        assertEquals(songA, sink.exhaustedSong)
    }

    @Test
    fun `全部候选装载失败判耗尽`() = runTest {
        val sink = FakeSink().feed(LoadOutcome.Failed(0), LoadOutcome.Failed(0))
        val resolver = object : PlaybackUrlResolver {
            override suspend fun resolve(song: Song) = listOf(
                AudioCandidate("http://a"),
                AudioCandidate("http://b"),
            )
        }
        coordinator(resolver, sink, this).playSong(songA)
        advanceUntilIdle()

        assertEquals(listOf("http://a", "http://b"), sink.urls)
        assertEquals(songA, sink.exhaustedSong)
    }

    @Test
    fun `试听标记透传给装载`() = runTest {
        val sink = FakeSink().feed(LoadOutcome.Success)
        val resolver = object : PlaybackUrlResolver {
            override suspend fun resolve(song: Song) = listOf(AudioCandidate("http://trial", isTrial = true))
        }
        coordinator(resolver, sink, this).playSong(songA)
        advanceUntilIdle()

        assertEquals(listOf(true), sink.trials)
    }

    @Test
    fun `有限换源_病态候选超上限后停止`() = runTest {
        val sink = FakeSink()
        repeat(SongLoadCoordinator.MAX_CANDIDATE_ATTEMPTS + 3) { sink.feed(LoadOutcome.Failed(0)) }
        val candidates = (0 until SongLoadCoordinator.MAX_CANDIDATE_ATTEMPTS + 3)
            .map { AudioCandidate("http://u$it") }
        val resolver = object : PlaybackUrlResolver {
            override suspend fun resolve(song: Song) = candidates
        }
        coordinator(resolver, sink, this).playSong(songA)
        advanceUntilIdle()

        assertEquals(SongLoadCoordinator.MAX_CANDIDATE_ATTEMPTS, sink.urls.size)
        assertEquals(songA, sink.exhaustedSong)
    }

    @Test
    fun `快速切歌_在飞解析被取消且迟到解析不装载`() = runTest {
        val g1 = CompletableDeferred<List<AudioCandidate>>()
        val g2 = CompletableDeferred<List<AudioCandidate>>()
        val sink = FakeSink().feed(LoadOutcome.Success)
        val c = coordinator(GateResolver(listOf(g1, g2)), sink, this)

        c.playSong(songA) // A 挂起在 g1
        advanceUntilIdle()
        c.playSong(songB) // B 挂起在 g2，取消 A 的 Job
        advanceUntilIdle()

        // A 的候选此刻才放行——迟到，要求被丢弃
        g1.complete(listOf(AudioCandidate("http://late-a")))
        advanceUntilIdle()
        assertTrue(sink.urls.isEmpty())

        g2.complete(listOf(AudioCandidate("http://b")))
        advanceUntilIdle()
        assertEquals(listOf("http://b"), sink.urls)
        assertEquals(songB.id, c.currentTarget)
    }

    @Test
    fun `迟到候选列表整体丢弃_不触发装载`() = runTest {
        val g1 = CompletableDeferred<List<AudioCandidate>>()
        val sink = FakeSink().feed(LoadOutcome.Success)
        val c = coordinator(GateResolver(listOf(g1)), sink, this)

        c.playSong(songA)
        advanceUntilIdle()
        c.stop() // 目标清空（等价于清队/切走）
        advanceUntilIdle()

        g1.complete(listOf(AudioCandidate("http://late")))
        advanceUntilIdle()
        assertTrue(sink.urls.isEmpty())
        assertTrue(c.currentTarget == null)
    }

    @Test
    fun `stop后无装载且目标为空`() = runTest {
        val sink = FakeSink()
        val resolver = object : PlaybackUrlResolver {
            override suspend fun resolve(song: Song) = listOf(AudioCandidate("http://a"))
        }
        val c = coordinator(resolver, sink, this)
        c.playSong(songA)
        c.stop()
        advanceUntilIdle()

        assertTrue(sink.urls.isEmpty())
        assertTrue(c.currentTarget == null)
    }
}