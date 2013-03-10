package fq.router.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class ShellUtils {

    public static void execute(String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        waitFor(Arrays.toString(command), process);
    }

    public static void sudo(String command) throws Exception {
        Log.d("fqrouter", "sudo: " + command);
        Process process = new ProcessBuilder()
                .command("su")
                .redirectErrorStream(true)
                .start();
        OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
        try {
            stdin.write(command + "\n");
        } finally {
            stdin.close();
        }
        waitFor(command, process);
    }

    public static void waitFor(String command, Process process) throws Exception {

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            String line;
            while (null != (line = stdout.readLine())) {
                Log.d("fqrouter", "shell: " + line);
            }
        } finally {
            stdout.close();
        }
        process.waitFor();
        if (0 != process.exitValue()) {
            throw new Exception("failed to execute: " + command);
        }
    }
}
