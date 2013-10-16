package fq.router2.utils;

import org.json.JSONObject;

import java.io.File;

public class ConfigUtils {

    public static int getHttpManagerPort() {
        File configFile = new File("/data/data/fq.router2/etc/fqsocks.json");
        if (!configFile.exists()) {
            return 2515;
        }
        try {
            return new JSONObject(IOUtils.readFromFile(configFile)).getJSONObject("http_manager").getInt("port");
        } catch (Exception e) {
            LogUtils.e("failed to parse config", e);
            return 2515;
        }
    }
}
