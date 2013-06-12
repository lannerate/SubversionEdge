package com.collabnet.svnedge.util
;

import org.codehaus.groovy.grails.commons.ApplicationHolder;

/**
 * The statistics time.
 * @author Marcello de Sales (mdesales@collab.net)
 */
public enum StatisticsTime {

    FIVE_MINUTES(300),
    HOUR(3600),
    DAY(86400),
    WEEK(604800),
    THIRTY_DAYS(2592000);

    private app = ApplicationHolder.application

    private int inSeconds;

    private StatisticsTime(int inSeconds) {
        this.inSeconds = inSeconds;
    }

    /**
     * Gets an i18n message from the messages.properties file without providing
     * parameters using the default locale.
     * @param key is the key in the messages.properties file.
     * @return the message related to the key in the messages.properties file
     * using the default locale.
     */
    private def getMessage(String key) {
        def appCtx = app.getMainContext()
        return appCtx.getMessage(key, null, Locale.getDefault())
    }

    /**
     * The number of seconds in the given time.
     */
    public int getSeconds() {
        return this.inSeconds
    }

    @Override
    public String toString() {
        switch (this) {
        case FIVE_MINUTES:
            return getMessage("statistics.graph.timespan.fiveMinutes")
        case HOUR:
            return getMessage("statistics.graph.timespan.anHour")
        case DAY:
            return getMessage("statistics.graph.timespan.aDay")
        case WEEK:
            return getMessage("statistics.graph.timespan.aWeek")
        case THIRTY_DAYS:
            return getMessage("statistics.graph.timespan.thirtyDays")
        }
    }
}
