package com.be.library.worker.annotations.compiler;

/**
 * Entry point to debug annotation processor
 * Created by Dzhey on 27-Mar-16.
 */
public class Main {

    private static final String TEST_JOB_PATH = "/path/to/job.java";

    public static void main(String[] args) throws Exception {
        com.sun.tools.javac.Main.main(new String[] {"-proc:only",
                "-processor", "com.be.library.worker.annotations.compiler.JobProcessor",
                TEST_JOB_PATH});
    }
}