package fq.router2.utils;

import java.io.File;

public class StartedAtFlag {

    private static File file = new File("/data/data/fq.router2/var/started-at");

    public static void create() {
        delete();
        IOUtils.createCommonDirs();
        IOUtils.writeToFile(file, String.valueOf(System.currentTimeMillis()));
        file.setReadable(true, false);
    }

    public static long delete() {
        try {
            if (file.exists()) {
                long elapsedTime = read();
                file.delete();
                if (elapsedTime < 0) {
                    return 0;
                }
                return elapsedTime;
            } else {
                return 0;
            }
        } catch (Exception e) {
            LogUtils.e("failed to delete started at flag", e);
            return 0;
        }
    }

    public static long read() {
        try {
            if (file.exists()) {
                String content = IOUtils.readFromFile(file);
                if (content.trim().length() == 0) {
                    return 0;
                }
                Long startedAt = Long.valueOf(content);
                LogUtils.e("started at " + startedAt + ", current is " + System.currentTimeMillis());
                return System.currentTimeMillis() - startedAt;
            } else {
                return 0;
            }
        } catch (Exception e) {
            LogUtils.e("failed to read started at flag", e);
            return 0;
        }
    }
}
