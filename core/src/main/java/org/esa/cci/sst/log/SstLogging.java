package org.esa.cci.sst.log;

import org.esa.beam.util.logging.BeamLogManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class SstLogging {

    private static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.INFO;

    static {
        BeamLogManager.removeRootLoggerHandlers();
    }

    private static Logger logger;

    public static LogLevel getDefaultLevel() {
        return DEFAULT_LOG_LEVEL;
    }

    public static Logger getLogger() {
        return getLogger(DEFAULT_LOG_LEVEL);
    }

    public static Logger getLogger(LogLevel logLevel) {
        if (logger == null) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // @todo 3 tb/tb extract class and test 2014-11-12
            final Formatter formatter = new Formatter() {
                @Override
                public String format(LogRecord record) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(dateFormat.format(new Date(record.getMillis())));
                    sb.append(" - ");
                    sb.append(record.getLevel().getName());
                    sb.append(": ");
                    sb.append(record.getMessage());
                    sb.append("\n");
                    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                    final Throwable thrown = record.getThrown();
                    if (thrown != null) {
                        sb.append(thrown.toString());
                        sb.append("\n");
                    }
                    return sb.toString();
                }
            };

            final ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(formatter);
            handler.setLevel(Level.ALL);

            logger = Logger.getLogger("org.esa.cci.sst");

            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        }
        logger.setLevel(logLevel.getValue());

        return logger;
    }

    public static void setLevelDebug() {
        if (logger != null) {
            logger.setLevel(Level.ALL);
        }
    }

    public static void setLevelSilent() {
        if (logger != null) {
            logger.setLevel(Level.WARNING);
        }
    }
}
