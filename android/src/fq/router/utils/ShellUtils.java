package fq.router.utils;

import fq.router.life.Deployer;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ShellUtils {

    private final static String[] BINARY_PLACES = {"/data/bin/", "/system/bin/", "/system/xbin/", "/sbin/",
            "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/",
            "/data/local/"};
    private final static String PYTHON_HOME = "/data/data/fq.router/python";
    public static File DATA_DIR = new File("/data/data/fq.router");
    public static File BUSYBOX_FILE = new File(DATA_DIR, "busybox");
    private static Boolean IS_ROOTED = null;

    public static String execute(String... command) throws Exception {
        Process process = executeNoWait(new HashMap<String, String>(), command);
        return waitFor(Arrays.toString(command), process);
    }

    public static Process executeNoWait(Map<String, String> env, String... command) throws IOException {
        LogUtils.i("command: " + Arrays.toString(command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().putAll(env);
        return processBuilder
                .redirectErrorStream(true)
                .start();
    }

    public static String sudo(Map<String, String> env, String... command) throws Exception {
        if (Boolean.FALSE.equals(IS_ROOTED)) {
            if (BUSYBOX_FILE.exists()) {
                Process process = ShellUtils.executeNoWait(env, BUSYBOX_FILE.getCanonicalPath(), "sh");
                OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
                try {
                    LogUtils.i("write to stdin: " + Arrays.toString(command));
                    for (String c : command) {
                        stdin.write(c);
                        stdin.write(" ");
                    }
                    stdin.write("\nexit\n");
                } finally {
                    stdin.close();
                }
                return waitFor(Arrays.toString(command), process);
            } else {
                return waitFor(Arrays.toString(command), executeNoWait(env, command));
            }
        }
        Process process = sudoNoWait(env, command);
        return waitFor(Arrays.toString(command), process);
    }

    public static String sudo(String... command) throws Exception {
        return sudo(new HashMap<String, String>(), command);
    }


    public static Process sudoNoWait(Map<String, String> env, String... command) throws Exception {
        if (Boolean.FALSE.equals(IS_ROOTED)) {
            return executeNoWait(env, command);
        }
        LogUtils.i("sudo: " + Arrays.toString(command));
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.environment().putAll(env);
        Process process = processBuilder
                .command(findCommand("su"))
                .redirectErrorStream(true)
                .start();
        OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
        try {
            stdin.write("echo going to run some command\n");
            for (String c : command) {
                stdin.write(c);
                stdin.write(" ");
            }
            stdin.write("\nexit\n");
        } finally {
            stdin.close();
        }
        return process;
    }

    public static String findCommand(String command) {
        for (String binaryPlace : BINARY_PLACES) {
            String path = binaryPlace + command;
            if (new File(path).exists()) {
                return path;
            }
        }
        return command;
    }

    public static String waitFor(String command, Process process) throws Exception {

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        try {
            String line;
            while (null != (line = stdout.readLine())) {
                output.append(line);
                output.append("\n");
            }
        } finally {
            stdout.close();
        }
        process.waitFor();
        int exitValue = process.exitValue();
        if (0 != exitValue) {
            throw new Exception("failed to execute: " + command + ", exit value: " + exitValue + ", output: " + output);
        }
        return output.toString();
    }

    public static Map<String, String> pythonEnv() {
        return new HashMap<String, String>() {{
            put("PYTHONHOME", PYTHON_HOME);
            put("PYTHONPATH", PYTHON_HOME + "/lib/python2.7/lib-dynload:" + PYTHON_HOME + "/lib/python2.7");
            put("PATH", PYTHON_HOME + "/bin:" + System.getenv("PATH"));
            put("LD_LIBRARY_PATH", System.getenv("LD_LIBRARY_PATH") + ":" + PYTHON_HOME + "/lib:" + PYTHON_HOME + "/lib/python2.7/lib-dynload");
        }};
    }

    public static boolean checkRooted() {
        if (IS_ROOTED != null) {
            return IS_ROOTED;
        }
        try {
            IS_ROOTED = sudo("echo", "hello").contains("hello");
        } catch (Exception e) {
            IS_ROOTED = false;
        }
        return IS_ROOTED;
    }

    public static boolean isRooted() {
        return Boolean.TRUE.equals(IS_ROOTED);
    }
}
