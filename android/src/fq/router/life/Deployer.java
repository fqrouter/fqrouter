package fq.router.life;

import android.content.Context;
import fq.router.feedback.AppendLogIntent;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;
import fq.router.utils.ShellUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Deployer {

    public static File DATA_DIR = new File("/data/data/fq.router");
    public static File BUSYBOX_FILE = new File(DATA_DIR, "busybox");
    public static File PAYLOAD_ZIP = new File(DATA_DIR, "payload.zip");
    public static File PAYLOAD_CHECKSUM = new File(DATA_DIR, "payload.checksum");
    public static File PYTHON_DIR = new File(DATA_DIR, "python");
    public static File PYTHON_LAUNCHER = new File(PYTHON_DIR, "bin/python-launcher.sh");
    public static File WIFI_TOOLS_DIR = new File(DATA_DIR, "wifi-tools");
    public static File PROXY_TOOLS_DIR = new File(DATA_DIR, "proxy-tools");
    public static File MANAGER_DIR = new File(DATA_DIR, "manager");
    public static File MANAGER_MAIN_PY = new File(MANAGER_DIR, "main.py");
    private final Context context;

    public Deployer(Context context) {
        this.context = context;

    }

    public boolean deploy() {
        updateStatus("Deploying payload");
        try {
            copyBusybox();
            makeExecutable(BUSYBOX_FILE);
        } catch (Exception e) {
            reportError("failed to copy busybox", e);
            return false;
        }
        boolean foundPayloadUpdate;
        try {
            foundPayloadUpdate = shouldDeployPayload();
        } catch (Exception e) {
            reportError("failed to check update", e);
            return false;
        }
        if (foundPayloadUpdate) {
            try {
                try {
                    ManagerProcess.kill();
                } catch (Exception e) {
                    LogUtils.e("failed to kill manager before redeploy", e);
                    appendLog("failed to kill manager before redeploy");
                    // ignore and continue
                }
                clearDataDirectory();
            } catch (Exception e) {
                reportError("failed to clear data directory", e);
                return false;
            }
        }
        try {
            copyBusybox();
            makeExecutable(BUSYBOX_FILE);
            copyPayloadZip();
        } catch (Exception e) {
            reportError("failed to copy payload.zip", e);
            return false;
        }
        try {
            unzipPayloadZip();
        } catch (Exception e) {
            reportError("failed to unzip payload.zip", e);
            return false;
        }
        try {
            makePayloadExecutable();
        } catch (Exception e) {
            reportError("failed to make payload executable", e);
            return false;
        }
        updateStatus("Deployed payload");
        return true;
    }

    public boolean isRooted() {
        try {
            return ShellUtils.sudo("echo", "hello").contains("hello");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldDeployPayload() throws Exception {
        if (!PAYLOAD_CHECKSUM.exists()) {
            appendLog("no checksum, assume it is old");
            return true;
        }
        if (!MANAGER_MAIN_PY.exists()) {
            appendLog("payload is corrupted");
            return true;
        }
        String oldChecksum = IOUtils.readFromFile(PAYLOAD_CHECKSUM);
        InputStream inputStream = context.getAssets().open("payload.zip");
        try {
            String newChecksum = IOUtils.copy(inputStream, null);
            if (oldChecksum.equals(newChecksum)) {
                appendLog("no payload update found");
                return false;
            } else {
                appendLog("found payload update");
                return true;
            }
        } finally {
            inputStream.close();
        }
    }

    private void clearDataDirectory() throws Exception {
        if (DATA_DIR.exists()) {
            appendLog("clear data dir");
            deleteDirectory(DATA_DIR + "/python");
            deleteDirectory(DATA_DIR + "/wifi-tools");
            deleteDirectory(DATA_DIR + "/proxy-tools");
            deleteDirectory(DATA_DIR + "/manager");
            deleteDirectory(DATA_DIR + "/payload.zip");
            new File("/data/data/fq.router/busybox").delete();
        }
    }

    private void deleteDirectory(String path) throws Exception {
        if (new File(path).exists()) {
            ShellUtils.execute("/data/data/fq.router/busybox", "rm", "-rf", path);
        }
        if (new File(path).exists()) {
            LogUtils.e("failed to delete " + path);
        }
    }

    private void copyPayloadZip() throws Exception {
        if (PAYLOAD_ZIP.exists()) {
            appendLog("skip copy payload.zip as it already exists");
            return;
        }
        if (PYTHON_DIR.exists()) {
            appendLog("skip copy payload.zip as it has already been unzipped");
            return;
        }
        appendLog("copying payload.zip to data directory");
        InputStream inputStream = context.getAssets().open("payload.zip");
        try {
            OutputStream outputStream = new FileOutputStream(PAYLOAD_ZIP);
            try {
                String checksum = IOUtils.copy(inputStream, outputStream);
                IOUtils.writeToFile(PAYLOAD_CHECKSUM, checksum);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
        appendLog("successfully copied payload.zip");
    }

    private void copyBusybox() throws Exception {
        if (BUSYBOX_FILE.exists()) {
            appendLog("skip copy busybox as it already exists");
            return;
        }
        appendLog("copying busybox to data directory");
        InputStream inputStream = context.getAssets().open("busybox");
        try {
            OutputStream outputStream = new FileOutputStream(BUSYBOX_FILE);
            try {
                IOUtils.copy(inputStream, outputStream);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
        appendLog("successfully copied busybox");
    }

    private void unzipPayloadZip() throws Exception {
        if (PYTHON_DIR.exists()) {
            appendLog("skip unzip payload.zip as it has already been unzipped");
            return;
        }
        appendLog("unzipping payload.zip");
        Process process = Runtime.getRuntime().exec(
                BUSYBOX_FILE + " unzip -o -q payload.zip", new String[0], new File("/data/data/fq.router"));
        ShellUtils.waitFor("unzip", process);
        if (!new File("/data/data/fq.router/payload.zip").delete()) {
            appendLog("failed to delete payload.zip after unzip");
        }
        for (int i = 0; i < 5; i++) {
            Thread.sleep(2000); // wait for the files written out
            if (MANAGER_MAIN_PY.exists() && PYTHON_LAUNCHER.exists() && WIFI_TOOLS_DIR.exists()) {
                break;
            }
        }
        appendLog("successfully unzipped payload.zip");
    }

    private void makePayloadExecutable() throws Exception {
        File[] files = new File(PYTHON_DIR, "bin").listFiles();
        if (files == null) {
            throw new Exception(new File(PYTHON_DIR, "bin") + " not found");
        } else {
            for (File file : files) {
                makeExecutable(file);
            }
        }
        files = WIFI_TOOLS_DIR.listFiles();
        if (files == null) {
            throw new Exception(WIFI_TOOLS_DIR + " not found");
        } else {
            for (File file : files) {
                makeExecutable(file);
            }
        }
//        files = PROXY_TOOLS_DIR.listFiles();
//        if (files == null) {
//            throw new Exception(PROXY_TOOLS_DIR + " not found");
//        } else {
//            for (File file : files) {
//                makeExecutable(file);
//            }
//        }
    }

    private void makeExecutable(File file) throws Exception {
        if (file.canExecute()) {
            return;
        }
        if (file.setExecutable(true, true)) {
            appendLog("successfully made " + file.getName() + " executable");
        } else {
            appendLog("failed to make " + file.getName() + " executable");
            ShellUtils.sudo(ShellUtils.findCommand("chmod"), "0700", file.getCanonicalPath());
        }
    }

    private void reportError(final String msg, Exception e) {
        if (null == e) {
            LogUtils.e(msg);
        } else {
            LogUtils.e(msg, e);
        }
        updateStatus("Error: " + msg);
    }

    private void appendLog(String log) {
        context.sendBroadcast(new AppendLogIntent(log));
    }

    private void updateStatus(String status) {
        context.sendBroadcast(new UpdateStatusIntent(status));
    }
}
