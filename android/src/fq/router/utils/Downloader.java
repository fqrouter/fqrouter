package fq.router.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Downloader {

    public static void download(String url, String downloadTo, int concurrency, ProgressUpdated callback) {
        try {
            if (!new FileDownloader(new URL(url), new File(downloadTo), concurrency, callback).download()) {
                callback.onDownloadFailed();
            }
        } catch (Exception e) {
            LogUtils.e("failed to download", e);
            callback.onDownloadFailed();
        }
    }

    public static interface ProgressUpdated {
        void onProgressUpdated(int percent);

        void onDownloaded();

        void onDownloadFailed();
    }

    private static interface BlockWriter {
        void write(long offset, byte[] buffer, int length) throws Exception;
    }

    private static interface ChunkCallback {

        void onChunkDownloaded(long from, long to);

        void onChunkDownloadFailed(long from, long failedAtOffset, long to);
    }

    private static class ChunkDownloader implements Runnable {
        private final BlockWriter blockWriter;
        private final ChunkCallback chunkCallback;
        private final URL url;
        private final String staticAddress;
        private final boolean isSecure;
        private final long from;
        private final long to;
        private long offset;

        private ChunkDownloader(BlockWriter blockWriter, ChunkCallback chunkCallback,
                                URL url, String staticAddress, boolean isSecure, long from, long to) {
            this.chunkCallback = chunkCallback;
            this.blockWriter = blockWriter;
            this.url = url;
            this.isSecure = isSecure;
            this.staticAddress = staticAddress;
            this.from = from;
            this.to = to;
            this.offset = from;
        }

        @Override
        public void run() {
            try {
                HttpsUtils.download(url, staticAddress, isSecure, null, from, to, new IOUtils.ChunkCopied() {
                    @Override
                    public void onChunkCopied(byte[] buffer, int length) throws Exception {
                        blockWriter.write(offset, buffer, length);
                        offset += length;
                    }
                });
                chunkCallback.onChunkDownloaded(from, to);
            } catch (Exception e) {
                LogUtils.e("download chunk failed", e);
                chunkCallback.onChunkDownloadFailed(from, offset, to);
            }
        }
    }

    private static class Chunk {
        private final long from;
        private final long to;

        private Chunk(long from, long to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Chunk chunk = (Chunk) o;

            if (from != chunk.from) return false;
            if (to != chunk.to) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) from;
            result = (int) (31 * result + to);
            return result;
        }
    }

    private static class FileDownloader implements BlockWriter, ChunkCallback {
        private final RandomAccessFile randomAccessFile;
        private final URL url;
        private final ProgressUpdated callback;
        private long totalLength;
        private int downloadedLength;
        private int addressIndex;
        private final Queue<Chunk> chunks = new ConcurrentLinkedQueue<Chunk>();
        private final Queue<String> errors = new ConcurrentLinkedQueue<String>();
        private final ExecutorService executorService;
        private List<Inet4Address> addresses;


        public FileDownloader(URL url, File file, int concurrency, ProgressUpdated callback) throws Exception {
            this.url = url;
            this.callback = callback;
            if (file.exists()) {
                file.delete();
            }
            randomAccessFile = new RandomAccessFile(file, "rw");
            executorService = Executors.newFixedThreadPool(concurrency);
        }

        public boolean download() {
            LogUtils.i("download " + url + " started");
            try {
                addresses = DnsUtils.resolveA(url.getHost());
                LogUtils.i("resolved " + url.getHost() + " => " + addresses);
                if (0 == addresses.size()) {
                    return false;
                }
                totalLength = getTotalLength(url, addresses);
                LogUtils.i("total length of " + url + ": " + totalLength);
                long from = 0;
                long step = totalLength / 20;
                while (from < totalLength) {
                    long to = Math.min(from + step, totalLength - 1);
                    addChunk(from, to, true);
                    from = to + 1;
                }
                while (!chunks.isEmpty()) {
                    if (!errors.isEmpty()) {
                        break;
                    }
                    Thread.sleep(1);
                }
                if (errors.isEmpty()) {
                    LogUtils.i("downloaded " + url);
                    LogUtils.i("total length: " + totalLength);
                    LogUtils.i("downloaded length: " + downloadedLength);
                    if (totalLength == downloadedLength) {
                        callback.onDownloaded();
                        return true;
                    } else {
                        LogUtils.e("downloaded incomplete file");
                        return false;
                    }
                } else {
                    for (String error : errors) {
                        LogUtils.i("error: " + error);
                    }
                    return false;
                }
            } catch (Exception e) {
                LogUtils.e("download failed", e);
            } finally {
                executorService.shutdownNow();
            }
            return false;
        }

        public synchronized void write(long offset, byte[] buffer, int length) throws Exception {
            randomAccessFile.seek(offset);
            randomAccessFile.write(buffer, 0, length);
            downloadedLength += length;
            int percent = (int) (downloadedLength * 100 / totalLength);
            LogUtils.i("downloading " + url + ": " + percent + "%");
            callback.onProgressUpdated(percent);
        }

        @Override
        public synchronized void onChunkDownloaded(long from, long to) {
            removeChunk(from, to);
        }

        @Override
        public synchronized void onChunkDownloadFailed(long from, long failedAtOffset, long to) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
            addChunk(failedAtOffset, to, true);
            removeChunk(from, to);
        }

        private void removeChunk(long from, long to) {
            Chunk chunk = new Chunk(from, to);
            if (chunks.contains(chunk)) {
                chunks.remove(chunk);
            } else {
                String error = "unexpected chunk " + from + " => " + to;
                LogUtils.e(error);
                errors.add(error);
            }
        }

        private void addChunk(long from, long to, boolean isSecure) {
            chunks.add(new Chunk(from, to));
            ChunkDownloader chunkDownloader = new ChunkDownloader(
                    this, this, url, getAddress().getHostAddress(), isSecure, from, to);
            executorService.submit(chunkDownloader);
        }

        public Inet4Address getAddress() {
            addressIndex = addressIndex + 1;
            if (addressIndex >= addresses.size()) {
                addressIndex = 0;
            }
            return addresses.get(addressIndex);
        }

        private static long getTotalLength(URL url, List<Inet4Address> staticAddresses) throws Exception {
            for (Inet4Address staticAddress : staticAddresses) {
                System.out.println(new Date() + " " + staticAddress);
                try {
                    return HttpsUtils.getTotalLength(url, staticAddress.getHostAddress(), false);
                } catch (Exception e) {
                    LogUtils.e("failed to get total length", e);
                }
            }
            for (Inet4Address staticAddress : staticAddresses) {
                System.out.println(new Date() + " " + staticAddress);
                try {
                    return HttpsUtils.getTotalLength(url, staticAddress.getHostAddress(), true);
                } catch (Exception e) {
                    LogUtils.e("failed to get total length", e);
                }
            }
            throw new Exception("give up getting total length");
        }
    }
}
