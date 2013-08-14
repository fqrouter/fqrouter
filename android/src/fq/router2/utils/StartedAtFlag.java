package fq.router2.utils;

import java.io.File;

public class StartedAtFlag {

    private static File file = new File("/data/data/fq.router2/var/started-at");

    public static void create() {
        delete();
        IOUtils.writeToFile(file, String.valueOf(System.currentTimeMillis()));
        file.setReadable(true, false);
    }

    public static void delete() {
        if (file.exists()) {
            file.delete();
        }
    }

    public static long read() {
        if (file.exists()) {
            Long startedAt = Long.valueOf(IOUtils.readFromFile(file));
            LogUtils.e("started at " + startedAt + ", current is " + System.currentTimeMillis());
            return startedAt;
        } else {
            return 0;
        }
    }
}
