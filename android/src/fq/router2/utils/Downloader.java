package fq.router2.utils;

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

    private static ExecutorService executorService;
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
        private final long from;
        private final long to;
        private long offset;

        private ChunkDownloader(BlockWriter blockWriter, ChunkCallback chunkCallback,
                                URL url, String staticAddress, long from, long to) {
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
        private RandomAccessFile randomAccessFile;
        private final URL url;
        private final File file;
        private final ProgressUpdated callback;
        private long totalLength;
        private int downloadedLength;
        private int addressIndex;
        private final Queue<Chunk> chunks = new ConcurrentLinkedQueue<Chunk>();
        private final Queue<String> errors = new ConcurrentLinkedQueue<String>();
        private List<Inet4Address> addresses;


        public FileDownloader(URL url, File file, int concurrency, ProgressUpdated callback) throws Exception {
            this.url = url;
            this.file = file;
            this.callback = callback;
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
                HttpsUtils.Headers headers = getHeaders(url, addresses);
                if (file.exists()) {
                    LogUtils.i("remote etag: " + headers.etag);
                    if (headers.etag != null && headers.etag.length() > 0) {
                        String localEtag = IOUtils.md5Checksum(file);
                        LogUtils.i("local etag: " + localEtag);
                        if (headers.etag.replace("\"", "").equals(localEtag)) {
                            LogUtils.i("already downloaded same file");
                            callback.onDownloaded();
                            return true;
                        }
                    }
                    file.delete();
                }
                randomAccessFile = new RandomAccessFile(file, "rw");
                totalLength = headers.contentLength;
                LogUtils.i("total length of " + url + ": " + totalLength);
                long from = 0;
                long step = totalLength / 20;
                while (from < totalLength) {
                    long to = Math.min(from + step, totalLength - 1);
                    addChunk(from, to);
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
            addChunk(failedAtOffset, to);
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

        private void addChunk(long from, long to) {
            chunks.add(new Chunk(from, to));
            ChunkDownloader chunkDownloader = new ChunkDownloader(
                    this, this, url, getAddress().getHostAddress(), from, to);
            executorService.submit(chunkDownloader);
        }

        public Inet4Address getAddress() {
            addressIndex = addressIndex + 1;
            if (addressIndex >= addresses.size()) {
                addressIndex = 0;
            }
            return addresses.get(addressIndex);
        }

        private static HttpsUtils.Headers getHeaders(URL url, List<Inet4Address> staticAddresses) throws Exception {
            try {
                return HttpsUtils.getHeadersUsingUrl(url);
            } catch (Exception e) {
                LogUtils.e("failed to get headers using url", e);
                for (Inet4Address staticAddress : staticAddresses) {
                    System.out.println(new Date() + " " + staticAddress);
                    try {
                        return HttpsUtils.getHeadersUsingRequest(url, staticAddress.getHostAddress());
                    } catch (Exception e2) {
                        LogUtils.e("failed to get headers using url", e2);
                    }
                }
            }
            throw new Exception("give up getting headers");
        }
    }

    public static void shutdown() {
        try {
            if (null != executorService) {
                executorService.shutdownNow();
            }
        } catch (Exception e) {
            LogUtils.e("failed to shutdown executor service", e);
        }
    }
}
