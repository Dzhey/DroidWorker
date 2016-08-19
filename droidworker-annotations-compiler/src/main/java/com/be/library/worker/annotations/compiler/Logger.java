package com.be.library.worker.annotations.compiler;


import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

/**
 * Handle error reporting for an annotation processor.
 *
 * @author Éamonn McManus
 */
class Logger {
    private final Messager mMessager;

    Logger(ProcessingEnvironment processingEnv) {
        mMessager = processingEnv.getMessager();
    }

    void note(String msg) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, msg);
    }
}