package fq.router2.utils;

import java.io.File;

public class StartedAtFlag {

    private static File file = new File("/data/data/fq.router2/var/started-at");

    public static void create() {
        delete();
        IOUtils.writeToFile(file, String.valueOf(System.currentTimeMillis()));
        file.setReadable(true, false);
    }

    public static long delete() {
        if (file.exists()) {
            long elapsedTime = read();
            file.delete();
            return elapsedTime;
        } else {
            return 0;
        }
    }

    public static long read() {
        if (file.exists()) {
            Long startedAt = Long.valueOf(IOUtils.readFromFile(file));
            LogUtils.e("started at " + startedAt + ", current is " + System.currentTimeMillis());
            return System.currentTimeMillis() - startedAt;
        } else {
            return 0;
        }
    }
}
