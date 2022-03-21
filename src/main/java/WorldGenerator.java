import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Interface one can implement to define the generation of new chunks
 */
public abstract class WorldGenerator {

    // Currently running jobs
    public HashMap<Thread, GenerationJob> jobs;
    private Lock jobLock;
    // All keys of chunks that are currently either running or in the queue
    public Set<Vector3i> inProgress;
    // Chunks that are done and need to be gathered by main thread
    private List<Chunk> chunkQueue;
    // Mutex for the previous list
    private Lock chunkQueueLock;

    public HashMap<String, String> config;

    public WorldGenerator() {
        jobs = new HashMap<>();
        jobLock = new ReentrantLock(true);
        inProgress = new HashSet<>();
        chunkQueue = new ArrayList<>();
        chunkQueueLock = new ReentrantLock(true);
        config = new HashMap<>();
    }

    /**
     * Generate the given chunk for the given world
     * @param world
     * @param chunk
     * @return
     */
    abstract Chunk generate(World world, Chunk chunk);

    /**
     * Dispatch a job to generate the given chunk for the given world
     * @param world
     * @param chunk
     */
    public void dispatch(World world, Chunk chunk) {
        GenerationJob job = new GenerationJob(this, world, chunk);
        Thread thread = new Thread(job);
        jobLock.lock();
        jobs.put(thread, job);
        inProgress.add(chunk.origin);
        jobLock.unlock();
        thread.start();
    }

    /**
     * Gather all 'done' chunks and clear the queue
     * also eliminate the threads
     * @return
     */
    public List<Chunk> gather() {
        List<Thread> finished = new ArrayList<>();
        jobLock.lock();
        for (Thread thread : jobs.keySet()) {
            if (!thread.isAlive()) {
                finished.add(thread);
            }
        }
        for (Thread thread : finished) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (Thread thread : finished) jobs.remove(thread);
        List<Chunk> results = clearQueue();
        for (Chunk c : results) inProgress.remove(c.origin);
        jobLock.unlock();
        return results;
    }

    /**
     * Get all chunks from the queue, and clear it
     * @return
     */
    public List<Chunk> clearQueue() {
        chunkQueueLock.lock();
        List<Chunk> result = new ArrayList<>(chunkQueue);
        chunkQueue.clear();
        chunkQueueLock.unlock();
        return result;
    }

    /**
     * Add the chunk to the queue
     * @param chunk
     */
    public void addChunkToQueue(Chunk chunk) {
        chunkQueueLock.lock();
        chunkQueue.add(chunk);
        chunkQueueLock.unlock();
    }

}
