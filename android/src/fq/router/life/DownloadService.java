package fq.router.life;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.utils.DnsUtils;
import fq.router.utils.HttpsUtils;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DownloadService extends IntentService {

    public DownloadService() {
        super("Download");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String url = intent.getStringExtra("url");
        final String downloadTo = intent.getStringExtra("downloadTo");
        try {
            if (new FileDownloader(new URL(url), new File(downloadTo), 4, new ProgressUpdated() {
                @Override
                public void onProgressUpdated(int percent) {
                    sendBroadcast(new DownloadingIntent(url, downloadTo, percent));
                }
            }).download()) {
                sendBroadcast(new DownloadedIntent(url, downloadTo));
            } else {
                sendBroadcast(new DownloadFailedIntent(url, downloadTo));
            }
        } catch (Exception e) {
            LogUtils.e("failed to download", e);
            sendBroadcast(new DownloadFailedIntent(url, downloadTo));
        }
    }

    public static void execute(Context context, String url, String downloadTo) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("url", url);
        intent.putExtra("downloadTo", downloadTo);
        context.startService(intent);
    }

    private static interface ProgressUpdated {
        void onProgressUpdated(int percent);
    }

    private static interface BlockWriter {
        void write(int offset, byte[] buffer, int length) throws Exception;
    }

    private static interface ChunkCallback {

        void onChunkDownloaded(int from, int to);

        void onChunkDownloadFailed(int from, int failedAtOffset, int to);
    }

    private static class ChunkDownloader extends Thread {
        private final BlockWriter blockWriter;
        private final ChunkCallback chunkCallback;
        private final URL url;
        private final Inet4Address staticAddress;
        private final int from;
        private final int to;
        private int offset;

        private ChunkDownloader(BlockWriter blockWriter, ChunkCallback chunkCallback,
                                URL url, Inet4Address staticAddress, int from, int to) {
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
        private final int from;
        private final int to;

        private Chunk(int from, int to) {
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
            int result = from;
            result = 31 * result + to;
            return result;
        }
    }

    private static class FileDownloader implements BlockWriter, ChunkCallback {
        private final RandomAccessFile randomAccessFile;
        private final URL url;
        private final int concurrency;
        private final ProgressUpdated callback;
        private int totalLength;
        private int downloadedLength;
        private int addressIndex;
        private final List<String> errors = new ArrayList<String>();
        private final Set<Chunk> chunks = new HashSet<Chunk>();
        private final List<Thread> threads = new ArrayList<Thread>();
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
                int from = 0;
                int step = totalLength / concurrency;
                while (from < totalLength) {
                    int to = Math.min(from + step, totalLength - 1);
                    addChunk(from, to);
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

        public synchronized void write(int offset, byte[] buffer, int length) throws Exception {
            randomAccessFile.seek(offset);
            randomAccessFile.write(buffer, 0, length);
            downloadedLength += length;
            int percent = downloadedLength * 100 / totalLength;
            LogUtils.i("downloading " + url + ": " + percent + "%");
            callback.onProgressUpdated(percent);
        }

        @Override
        public synchronized void onChunkDownloaded(int from, int to) {
            removeChunk(from, to);
        }

        @Override
        public synchronized void onChunkDownloadFailed(int from, int failedAtOffset, int to) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
            addChunk(failedAtOffset, to);
            removeChunk(from, to);
        }

        private void removeChunk(int from, int to) {
            Chunk chunk = new Chunk(from, to);
            if (chunks.contains(chunk)) {
                chunks.remove(chunk);
            } else {
                String error = "unexpected chunk " + from + " => " + to;
                LogUtils.e(error);
                errors.add(error);
            }
        }

        private void addChunk(int from, int to) {
            chunks.add(new Chunk(from, to));
            ChunkDownloader thread = new ChunkDownloader(this, this, url, getAddress(), from, to);
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

        private static int getTotalLength(URL url, List<Inet4Address> staticAddresses) throws Exception {
            for (Inet4Address staticAddress : staticAddresses) {
                try {
                    return HttpsUtils.getTotalLength(url, staticAddress);
                } catch (Exception e) {
                    LogUtils.e("failed to get total length", e);
                }
            }
            throw new Exception("give up getting total length");
        }
    }
}
