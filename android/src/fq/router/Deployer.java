package fq.router;

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
    public static File MANAGER_CLEAN_PY = new File(MANAGER_DIR, "clean.py");
    private final StatusUpdater statusUpdater;

    public Deployer(StatusUpdater statusUpdater) {
        this.statusUpdater = statusUpdater;

    }

    public boolean deploy() {
        statusUpdater.updateStatus("Deploying payload");
        try {
            copyBusybox();
            makeExecutable(BUSYBOX_FILE);
        } catch (Exception e) {
            statusUpdater.reportError("failed to copy busybox", e);
            return false;
        }
        boolean foundPayloadUpdate;
        try {
            foundPayloadUpdate = shouldDeployPayload();
        } catch (Exception e) {
            statusUpdater.reportError("failed to check update", e);
            return false;
        }
        if (foundPayloadUpdate) {
            try {
                try {
                    ManagerProcess.kill();
                } catch (Exception e) {
                    LogUtils.e("failed to kill manager before redeploy", e);
                    statusUpdater.appendLog("failed to kill manager before redeploy");
                    // ignore and continue
                }
                clearDataDirectory();
            } catch (Exception e) {
                statusUpdater.reportError("failed to clear data directory", e);
                return false;
            }
        }
        try {
            copyBusybox();
            makeExecutable(BUSYBOX_FILE);
            copyPayloadZip();
        } catch (Exception e) {
            statusUpdater.reportError("failed to copy payload.zip", e);
            return false;
        }
        try {
            unzipPayloadZip();
            linkLibs();
        } catch (Exception e) {
            statusUpdater.reportError("failed to unzip payload.zip", e);
            return false;
        }
        try {
            makePayloadExecutable();
        } catch (Exception e) {
            statusUpdater.reportError("failed to make payload executable", e);
            return false;
        }
        statusUpdater.updateStatus("Deployed payload");
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
            statusUpdater.appendLog("no checksum, assume it is old");
            return true;
        }
        if (!MANAGER_MAIN_PY.exists()) {
            statusUpdater.appendLog("payload is corrupted");
            return true;
        }
        String oldChecksum = IOUtils.readFromFile(PAYLOAD_CHECKSUM);
        InputStream inputStream = statusUpdater.getAssets().open("payload.zip");
        try {
            String newChecksum = IOUtils.copy(inputStream, null);
            if (oldChecksum.equals(newChecksum)) {
                statusUpdater.appendLog("no payload update found");
                return false;
            } else {
                statusUpdater.appendLog("found payload update");
                return true;
            }
        } finally {
            inputStream.close();
        }
    }

    private void clearDataDirectory() throws Exception {
        if (DATA_DIR.exists()) {
            statusUpdater.appendLog("clear data dir");
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
            statusUpdater.appendLog("skip copy payload.zip as it already exists");
            return;
        }
        if (PYTHON_DIR.exists()) {
            statusUpdater.appendLog("skip copy payload.zip as it has already been unzipped");
            return;
        }
        statusUpdater.appendLog("copying payload.zip to data directory");
        InputStream inputStream = statusUpdater.getAssets().open("payload.zip");
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
        statusUpdater.appendLog("successfully copied payload.zip");
    }

    private void copyBusybox() throws Exception {
        if (BUSYBOX_FILE.exists()) {
            statusUpdater.appendLog("skip copy busybox as it already exists");
            return;
        }
        statusUpdater.appendLog("copying busybox to data directory");
        InputStream inputStream = statusUpdater.getAssets().open("busybox");
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
        statusUpdater.appendLog("successfully copied busybox");
    }

    private void unzipPayloadZip() throws Exception {
        if (PYTHON_DIR.exists()) {
            statusUpdater.appendLog("skip unzip payload.zip as it has already been unzipped");
            return;
        }
        statusUpdater.appendLog("unzipping payload.zip");
        Process process = Runtime.getRuntime().exec(
                BUSYBOX_FILE + " unzip -o -q payload.zip", new String[0], new File("/data/data/fq.router"));
        ShellUtils.waitFor("unzip", process);
        if (!new File("/data/data/fq.router/payload.zip").delete()) {
            statusUpdater.appendLog("failed to delete payload.zip after unzip");
        }
        statusUpdater.appendLog("successfully unzipped payload.zip");
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000); // wait for the files written out
            if (MANAGER_MAIN_PY.exists()) {
                break;
            }
        }
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
        files = PROXY_TOOLS_DIR.listFiles();
        if (files == null) {
            throw new Exception(PROXY_TOOLS_DIR + " not found");
        } else {
            for (File file : files) {
                makeExecutable(file);
            }
        }
    }

    private void makeExecutable(File file) throws Exception {
        if (file.canExecute()) {
            return;
        }
        if (file.setExecutable(true, true)) {
            statusUpdater.appendLog("successfully made " + file.getName() + " executable");
        } else {
            statusUpdater.appendLog("failed to make " + file.getName() + " executable");
        }
    }

    private void linkLibs() throws Exception {
        File libDir = new File("/data/data/fq.router/lib");
        String linkTo = "/data/data/fq.router/python/lib";
        if (libDir.exists() && libDir.getCanonicalPath().equals(linkTo)) {
            return;
        }
        try {
            if (libDir.exists()) {
                ShellUtils.sudo(BUSYBOX_FILE.getAbsolutePath(), "rm", "-r", "-f", libDir.getPath());
            }
            ShellUtils.sudo(BUSYBOX_FILE.getAbsolutePath(), "ln", "-s", linkTo, libDir.getPath());
        } catch (Exception e) {
            LogUtils.e("failed to link libs");
        }
    }
}
