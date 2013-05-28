package fq.router.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private static class ChunkDownloader extends Thread {
        private final BlockWriter blockWriter;
        private final ChunkCallback chunkCallback;
        private final String url;
        private final String staticAddress;
        private final long from;
        private final long to;
        private long offset;

        private ChunkDownloader(BlockWriter blockWriter, ChunkCallback chunkCallback,
                                String url, String staticAddress, long from, long to) {
            this.chunkCallback = chunkCallback;
            this.blockWriter = blockWriter;
            this.url = url;
            this.staticAddress = staticAddress;
            this.from = from;
            this.to = to;
            this.offset = from;
        }

        @Override
        public void run() {
            try {
                HttpsUtils.download(url, staticAddress, null, from, to, new IOUtils.ChunkCopied() {
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
        private final int concurrency;
        private final ProgressUpdated callback;
        private long totalLength;
        private int downloadedLength;
        private int addressIndex;
        private final Queue<Chunk> chunks = new ConcurrentLinkedQueue<Chunk>();
        private final Queue<String> errors = new ConcurrentLinkedQueue<String>();
        private final Queue<Thread> threads = new ConcurrentLinkedQueue<Thread>();
        private List<Inet4Address> addresses;


        public FileDownloader(URL url, File file, int concurrency, ProgressUpdated callback) throws Exception {
            this.url = url;
            this.concurrency = concurrency;
            this.callback = callback;
            if (file.exists()) {
                file.delete();
            }
            randomAccessFile = new RandomAccessFile(file, "rw");
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
                long step = totalLength / concurrency;
                while (from < totalLength) {
                    long to = Math.min(from + step, totalLength - 1);
                    addChunk(from, to, false);
                    from = to + 1;
                }
                while (!chunks.isEmpty()) {
                    if (!errors.isEmpty()) {
                        break;
                    }
                    Thread.sleep(1);
                }
                LogUtils.i("downloaded " + url);
                return true;
            } catch (Exception e) {
                LogUtils.e("download failed", e);
            } finally {
                for (Thread thread : threads) {
                    try {
                        thread.join(1000);
                    } catch (InterruptedException e) {
                        LogUtils.e("found hanging thread");
                    }
                }
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
            if (percent == 100) {
                callback.onDownloaded();
            }
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

        private void addChunk(long from, long to, boolean secure) {
            chunks.add(new Chunk(from, to));
            String chunkUrl;
            if (secure) {
                chunkUrl = url.toString().replace(url.getProtocol(), "https");
            } else {
                chunkUrl = url.toString().replace(url.getProtocol(), "http");
            }
            ChunkDownloader thread = new ChunkDownloader(this, this, chunkUrl, getAddress().getHostAddress(), from, to);
            thread.start();
            threads.add(thread);
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
                    return HttpsUtils.getTotalLength(
                            url.toString().replace(url.getProtocol(), "http"), staticAddress.getHostAddress());
                } catch (Exception e) {
                    LogUtils.e("failed to get total length", e);
                }
            }
            for (Inet4Address staticAddress : staticAddresses) {
                System.out.println(new Date() + " " + staticAddress);
                try {
                    return HttpsUtils.getTotalLength(
                            url.toString().replace(url.getProtocol(), "https"), staticAddress.getHostAddress());
                } catch (Exception e) {
                    LogUtils.e("failed to get total length", e);
                }
            }
            throw new Exception("give up getting total length");
        }
    }
}
