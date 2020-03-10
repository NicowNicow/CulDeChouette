package fr.isen.culdechouette

class SampleQueue {

    private val pool = SamplePool()
    private var oldest: Sample? = null
    private var newest: Sample? = null
    private var sampleCount: Int = 0
    private var acceleratingCount: Int = 0


    val isShaking: Boolean
        get() = (newest != null && oldest != null && newest!!.timestamp - oldest!!.timestamp >= MIN_WINDOW_SIZE && acceleratingCount >= (sampleCount shr 1) + (sampleCount shr 2))


    fun add(timestamp: Long, accelerating: Boolean) {
        purge(timestamp - MAX_WINDOW_SIZE)
        val added = pool.acquire()
        added.timestamp = timestamp
        added.accelerating = accelerating
        added.next = null
        if (newest != null) {
            newest!!.next = added
        }
        newest = added
        if (oldest == null) {
            oldest = added
        }
        sampleCount++
        if (accelerating) {
            acceleratingCount++
        }
    }

    fun clear() {
        while (oldest != null) {
            val removed = oldest
            oldest = removed!!.next
            pool.release(removed)
        }
        newest = null
        sampleCount = 0
        acceleratingCount = 0
    }


    private fun purge(cutoff: Long) {
        while (sampleCount >= MIN_QUEUE_SIZE && oldest != null && cutoff - oldest!!.timestamp > 0) {
            val removed = oldest
            if (removed!!.accelerating) {
                acceleratingCount--
            }
            sampleCount--
            oldest = removed.next
            if (oldest == null) {
                newest = null
            }
            pool.release(removed)
        }
    }

    fun asList(): List<Sample> {
        val list = ArrayList<Sample>()
        var s = oldest
        while (s != null) {
            list.add(s)
            s = s.next
        }
        return list
    }

    companion object {
        private val MAX_WINDOW_SIZE: Long = 500000000 // 0.5s
        private val MIN_WINDOW_SIZE = MAX_WINDOW_SIZE shr 1 // 0.25s
        private val MIN_QUEUE_SIZE = 4
    }
}


class SamplePool {
    private var head: Sample? = null

    fun acquire(): Sample {
        var acquired = head
        if (acquired == null) {
            acquired = Sample()
        } else {
            head = acquired.next
        }
        return acquired
    }

    fun release(sample: Sample) {
        sample.next = head
        head = sample
    }
}

class Sample {
    var timestamp: Long = 0
    var accelerating: Boolean = false
    var next: Sample? = null
}