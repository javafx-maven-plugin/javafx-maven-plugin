package com.zenjava.javafx.maven.plugin.util;

import com.zenjava.javafx.deploy.log.Log;

public class MavenLog implements Log {

    private org.apache.maven.plugin.logging.Log consoleLog;

    public MavenLog(org.apache.maven.plugin.logging.Log consoleLog) {
        this.consoleLog = consoleLog;
    }

    @Override
    public void debug(String message, Object... values) {
        consoleLog.debug(String.format(message, values));
    }

    @Override
    public void info(String message, Object... values) {
        consoleLog.info(String.format(message, values));
    }

    @Override
    public void warn(String message, Object... values) {
        consoleLog.warn(String.format(message, values));
    }

    @Override
    public void error(String message, Object... values) {
        consoleLog.error(String.format(message, values));
    }

    @Override
    public void exception(String message, Throwable error, Object... values) {
        consoleLog.error(String.format(message, values), error);
    }
}
