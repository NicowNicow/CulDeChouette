package fr.isen.culdechouette

/** Pools samples. Avoids garbage collection.  */
class SamplePool {
    private var head: Sample? = null

    /** Acquires a sample from the pool.  */
    fun acquire(): Sample {
        var acquired = head
        if (acquired == null) {
            acquired = Sample()
        } else {
            // Remove instance from pool.
            head = acquired.next
        }
        return acquired
    }

    /** Returns a sample to the pool.  */
    fun release(sample: Sample) {
        sample.next = head
        head = sample
    }
}