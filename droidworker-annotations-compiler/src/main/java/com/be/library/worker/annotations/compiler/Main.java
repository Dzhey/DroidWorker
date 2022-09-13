package com.be.library.worker.annotations.compiler;

/**
 * Entry point to debug annotation processor
 * Created by Dzhey on 27-Mar-16.
 */
public class Main {

    private static final String TEST_JOB_PATH = "C:\\Users\\Dzhey\\Documents\\Projects\\git\\DroidWorker\\demo\\src\\main\\java\\com\\be\\android\\library\\worker\\demo\\jobs\\SimpleImageLoaderJob.java";
    private static final String TEST_JOB2_PATH = "C:\\Users\\Dzhey\\Documents\\Projects\\git\\DroidWorker\\demo\\src\\main\\java\\com\\be\\android\\library\\worker\\demo\\jobs\\LoadListEntryJob.java";

    public static void main(String[] args) throws Exception {
        if (true) throw new RuntimeException("main() test exception");
        com.sun.tools.javac.Main.main(new String[] {"-proc:only",
                "-processor", "com.be.library.worker.annotations.compiler.JobProcessor",
                TEST_JOB_PATH, TEST_JOB2_PATH});
    }
}