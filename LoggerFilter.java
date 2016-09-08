package com.wellsfargo.transferandpay.util;

import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
/**
 * Platform-X WB Helper Class for to handle log events and apply filter
 * 
 * @author Karthiga Baskaran
 */
public class LoggerFilter extends Filter<ILoggingEvent> {
    private Level level;
    private String logger;

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (!event.getLoggerName().startsWith(logger))
            return FilterReply.NEUTRAL;
        
        List<Level> eventsToKeep = Arrays.asList(Level.INFO);
        if (eventsToKeep.contains(event.getLevel()))
        {
            return FilterReply.NEUTRAL;
        }
        else
        {
            return FilterReply.DENY;
        }
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public void start() {
        if (this.level != null && this.logger != null) {
            super.start();
        }
    }
}

