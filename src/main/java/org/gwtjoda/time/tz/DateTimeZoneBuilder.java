/*
 *  Copyright 2001-2005 Stephen Colebourne
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gwtjoda.time.tz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gwtjoda.time.Chronology;
import org.gwtjoda.time.DateTime;
import org.gwtjoda.time.DateTimeUtils;
import org.gwtjoda.time.DateTimeZone;
import org.gwtjoda.time.Period;
import org.gwtjoda.time.PeriodType;
import org.gwtjoda.time.calendar.TimeZoneList;
import org.gwtjoda.time.chrono.ISOChronology;

/**
 * DateTimeZoneBuilder allows complex DateTimeZones to be constructed. Since
 * creating a new DateTimeZone this way is a relatively expensive operation,
 * built zones can be written to a file. Reading back the encoded data is a
 * quick operation.
 * <p>
 * DateTimeZoneBuilder itself is mutable and not thread-safe, but the
 * DateTimeZone objects that it builds are thread-safe and immutable.
 * <p>
 * It is intended that {@link ZoneInfoCompiler} be used to read time zone data
 * files, indirectly calling DateTimeZoneBuilder. The following complex
 * example defines the America/Los_Angeles time zone, with all historical
 * transitions:
 * 
 * <pre>
 * DateTimeZone America_Los_Angeles = new DateTimeZoneBuilder()
 *     .addCutover(-2147483648, 'w', 1, 1, 0, false, 0)
 *     .setStandardOffset(-28378000)
 *     .setFixedSavings("LMT", 0)
 *     .addCutover(1883, 'w', 11, 18, 0, false, 43200000)
 *     .setStandardOffset(-28800000)
 *     .addRecurringSavings("PDT", 3600000, 1918, 1919, 'w',  3, -1, 7, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1918, 1919, 'w', 10, -1, 7, false, 7200000)
 *     .addRecurringSavings("PWT", 3600000, 1942, 1942, 'w',  2,  9, 0, false, 7200000)
 *     .addRecurringSavings("PPT", 3600000, 1945, 1945, 'u',  8, 14, 0, false, 82800000)
 *     .addRecurringSavings("PST",       0, 1945, 1945, 'w',  9, 30, 0, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1948, 1948, 'w',  3, 14, 0, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1949, 1949, 'w',  1,  1, 0, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1950, 1966, 'w',  4, -1, 7, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1950, 1961, 'w',  9, -1, 7, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1962, 1966, 'w', 10, -1, 7, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1967, 2147483647, 'w', 10, -1, 7, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1967, 1973, 'w', 4, -1,  7, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1974, 1974, 'w', 1,  6,  0, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1975, 1975, 'w', 2, 23,  0, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1976, 1986, 'w', 4, -1,  7, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1987, 2147483647, 'w', 4, 1, 7, true, 7200000)
 *     .toDateTimeZone("America/Los_Angeles");
 * </pre>
 *
 * @author Brian S O'Neill
 * @see ZoneInfoCompiler
 * @see ZoneInfoProvider
 * @since 1.0
 */
public class DateTimeZoneBuilder {
    private static DateTimeZone buildFixedZone(String id, String nameKey,
                                               int wallOffset, int standardOffset) {
        if ("UTC".equals(id) && id.equals(nameKey) &&
            wallOffset == 0 && standardOffset == 0) {
            return DateTimeZone.UTC;
        }
        return new FixedDateTimeZone(id, nameKey, wallOffset, standardOffset);
    }

    // List of RuleSets.
    private final ArrayList iRuleSets;

    public DateTimeZoneBuilder() {
        iRuleSets = new ArrayList(10);
    }

    /**
     * Adds a cutover for added rules. The standard offset at the cutover
     * defaults to 0. Call setStandardOffset afterwards to change it.
     *
     * @param year year of cutover
     * @param mode 'u' - cutover is measured against UTC, 'w' - against wall
     * offset, 's' - against standard offset.
     * @param dayOfMonth if negative, set to ((last day of month) - ~dayOfMonth).
     * For example, if -1, set to last day of month
     * @param dayOfWeek if 0, ignore
     * @param advanceDayOfWeek if dayOfMonth does not fall on dayOfWeek, advance to
     * dayOfWeek when true, retreat when false.
     * @param millisOfDay additional precision for specifying time of day of
     * cutover
     */
    public DateTimeZoneBuilder addCutover(int year,
                                          char mode,
                                          int monthOfYear,
                                          int dayOfMonth,
                                          int dayOfWeek,
                                          boolean advanceDayOfWeek,
                                          int millisOfDay)
    {
        OfYear ofYear = new OfYear
            (mode, monthOfYear, dayOfMonth, dayOfWeek, advanceDayOfWeek, millisOfDay);
        if (iRuleSets.size() > 0) {
            RuleSet lastRuleSet = (RuleSet)iRuleSets.get(iRuleSets.size() - 1);
            lastRuleSet.setUpperLimit(year, ofYear);
        }
        iRuleSets.add(new RuleSet());
        return this;
    }

    /**
     * Sets the standard offset to use for newly added rules until the next
     * cutover is added.
     */
    public DateTimeZoneBuilder setStandardOffset(int standardOffset) {
        getLastRuleSet().setStandardOffset(standardOffset);
        return this;
    }

    /**
     * Set a fixed savings rule at the cutover.
     */
    public DateTimeZoneBuilder setFixedSavings(String nameKey, int saveMillis) {
        getLastRuleSet().setFixedSavings(nameKey, saveMillis);
        return this;
    }

    /**
     * Add a recurring daylight saving time rule.
     *
     * @param nameKey name key of new rule
     * @param saveMillis milliseconds to add to standard offset
     * @param fromYear First year that rule is in effect. MIN_VALUE indicates
     * beginning of time.
     * @param toYear Last year (inclusive) that rule is in effect. MAX_VALUE
     * indicates end of time.
     * @param mode 'u' - transitions are calculated against UTC, 'w' -
     * transitions are calculated against wall offset, 's' - transitions are
     * calculated against standard offset.
     * @param dayOfMonth if negative, set to ((last day of month) - ~dayOfMonth).
     * For example, if -1, set to last day of month
     * @param dayOfWeek if 0, ignore
     * @param advanceDayOfWeek if dayOfMonth does not fall on dayOfWeek, advance to
     * dayOfWeek when true, retreat when false.
     * @param millisOfDay additional precision for specifying time of day of
     * transitions
     */
    public DateTimeZoneBuilder addRecurringSavings(String nameKey, int saveMillis,
                                                   int fromYear, int toYear,
                                                   char mode,
                                                   int monthOfYear,
                                                   int dayOfMonth,
                                                   int dayOfWeek,
                                                   boolean advanceDayOfWeek,
                                                   int millisOfDay)
    {
        if (fromYear <= toYear) {
            OfYear ofYear = new OfYear
                (mode, monthOfYear, dayOfMonth, dayOfWeek, advanceDayOfWeek, millisOfDay);
            Recurrence recurrence = new Recurrence(ofYear, nameKey, saveMillis);
            Rule rule = new Rule(recurrence, fromYear, toYear);
            getLastRuleSet().addRule(rule);
        }
        return this;
    }

    private RuleSet getLastRuleSet() {
        if (iRuleSets.size() == 0) {
            addCutover(Integer.MIN_VALUE, 'w', 1, 1, 0, false, 0);
        }
        return (RuleSet)iRuleSets.get(iRuleSets.size() - 1);
    }
    
    /**
     * Processes all the rules and builds a DateTimeZone.
     *
     * @param id  time zone id to assign
     * @param outputID  true if the zone id should be output
     */
    public DateTimeZone toDateTimeZone(String id, boolean outputID) {
        if (id == null) {
            throw new IllegalArgumentException();
        }

        // Discover where all the transitions occur and store the results in
        // these lists.
        ArrayList transitions = new ArrayList();

        // Tail zone picks up remaining transitions in the form of an endless
        // DST cycle.
        DSTZone tailZone = null;

        long millis = Long.MIN_VALUE;
        int saveMillis = 0;
            
        int ruleSetCount = iRuleSets.size();
        for (int i=0; i<ruleSetCount; i++) {
            RuleSet rs = (RuleSet)iRuleSets.get(i);
            Transition next = rs.firstTransition(millis);
            if (next == null) {
                continue;
            }
            addTransition(transitions, next);
            millis = next.getMillis();
            saveMillis = next.getSaveMillis();

            // Copy it since we're going to destroy it.
            rs = new RuleSet(rs);

            while ((next = rs.nextTransition(millis, saveMillis)) != null) {
                if (addTransition(transitions, next)) {
                    if (tailZone != null) {
                        // Got the extra transition before DSTZone.
                        break;
                    }
                }
                millis = next.getMillis();
                saveMillis = next.getSaveMillis();
                if (tailZone == null && i == ruleSetCount - 1) {
                    tailZone = rs.buildTailZone(id);
                    // If tailZone is not null, don't break out of main loop until
                    // at least one more transition is calculated. This ensures a
                    // correct 'seam' to the DSTZone.
                }
            }

            millis = rs.getUpperLimit(saveMillis);
        }

        // Check if a simpler zone implementation can be returned.
        if (transitions.size() == 0) {
            if (tailZone != null) {
                // This shouldn't happen, but handle just in case.
                return tailZone;
            }
            return buildFixedZone(id, "UTC", 0, 0);
        }
        if (transitions.size() == 1 && tailZone == null) {
            Transition tr = (Transition)transitions.get(0);
            return buildFixedZone(id, tr.getNameKey(),
                                  tr.getWallOffset(), tr.getStandardOffset());
        }

        PrecalculatedZone zone = PrecalculatedZone.create(id, outputID, transitions, tailZone);
        if (zone.isCachable()) {
            return CachedDateTimeZone.forZone(zone);
        }
        return zone;
    }

    private boolean addTransition(ArrayList transitions, Transition tr) {
        int size = transitions.size();
        if (size == 0) {
            transitions.add(tr);
            return true;
        }

        Transition last = (Transition)transitions.get(size - 1);
        if (!tr.isTransitionFrom(last)) {
            return false;
        }

        // If local time of new transition is same as last local time, just
        // replace last transition with new one.
        int offsetForLast = 0;
        if (size >= 2) {
            offsetForLast = ((Transition)transitions.get(size - 2)).getWallOffset();
        }
        int offsetForNew = last.getWallOffset();

        long lastLocal = last.getMillis() + offsetForLast;
        long newLocal = tr.getMillis() + offsetForNew;

        if (newLocal != lastLocal) {
            transitions.add(tr);
            return true;
        }

        transitions.remove(size - 1);
        return addTransition(transitions, tr);
    }

    /**
     * Supports setting fields of year and moving between transitions.
     */
    private static final class OfYear {
        // Is 'u', 'w', or 's'.
        final char iMode;

        final int iMonthOfYear;
        final int iDayOfMonth;
        final int iDayOfWeek;
        final boolean iAdvance;
        final int iMillisOfDay;

        OfYear(char mode,
               int monthOfYear,
               int dayOfMonth,
               int dayOfWeek, boolean advanceDayOfWeek,
               int millisOfDay)
        {
            if (mode != 'u' && mode != 'w' && mode != 's') {
                throw new IllegalArgumentException("Unknown mode: " + mode);
            }

            iMode = mode;
            iMonthOfYear = monthOfYear;
            iDayOfMonth = dayOfMonth;
            iDayOfWeek = dayOfWeek;
            iAdvance = advanceDayOfWeek;
            iMillisOfDay = millisOfDay;
        }

        /**
         * @param standardOffset standard offset just before instant
         */
        public long setInstant(int year, int standardOffset, int saveMillis) {
            int offset;
            if (iMode == 'w') {
                offset = standardOffset + saveMillis;
            } else if (iMode == 's') {
                offset = standardOffset;
            } else {
                offset = 0;
            }

            Chronology chrono = ISOChronology.getInstanceUTC();
            long millis = chrono.year().set(0, year);
            millis = chrono.monthOfYear().set(millis, iMonthOfYear);
            millis = chrono.millisOfDay().set(millis, iMillisOfDay);
            millis = setDayOfMonth(chrono, millis);

            if (iDayOfWeek != 0) {
                millis = setDayOfWeek(chrono, millis);
            }

            // Convert from local time to UTC.
            return millis - offset;
        }

        /**
         * @param standardOffset standard offset just before next recurrence
         */
        public long next(long instant, int standardOffset, int saveMillis) {
            int offset;
            if (iMode == 'w') {
                offset = standardOffset + saveMillis;
            } else if (iMode == 's') {
                offset = standardOffset;
            } else {
                offset = 0;
            }

            // Convert from UTC to local time.
            instant += offset;

            Chronology chrono = ISOChronology.getInstanceUTC();
            long next = chrono.monthOfYear().set(instant, iMonthOfYear);
            // Be lenient with millisOfDay.
            next = chrono.millisOfDay().set(next, 0);
            next = chrono.millisOfDay().add(next, iMillisOfDay);
            next = setDayOfMonthNext(chrono, next);

            if (iDayOfWeek == 0) {
                if (next <= instant) {
                    next = chrono.year().add(next, 1);
                    next = setDayOfMonthNext(chrono, next);
                }
            } else {
                next = setDayOfWeek(chrono, next);
                if (next <= instant) {
                    next = chrono.year().add(next, 1);
                    next = chrono.monthOfYear().set(next, iMonthOfYear);
                    next = setDayOfMonthNext(chrono, next);
                    next = setDayOfWeek(chrono, next);
                }
            }

            // Convert from local time to UTC.
            return next - offset;
        }

        /**
         * @param standardOffset standard offset just before previous recurrence
         */
        public long previous(long instant, int standardOffset, int saveMillis) {
            int offset;
            if (iMode == 'w') {
                offset = standardOffset + saveMillis;
            } else if (iMode == 's') {
                offset = standardOffset;
            } else {
                offset = 0;
            }

            // Convert from UTC to local time.
            instant += offset;

            Chronology chrono = ISOChronology.getInstanceUTC();
            long prev = chrono.monthOfYear().set(instant, iMonthOfYear);
            // Be lenient with millisOfDay.
            prev = chrono.millisOfDay().set(prev, 0);
            prev = chrono.millisOfDay().add(prev, iMillisOfDay);
            prev = setDayOfMonthPrevious(chrono, prev);

            if (iDayOfWeek == 0) {
                if (prev >= instant) {
                    prev = chrono.year().add(prev, -1);
                    prev = setDayOfMonthPrevious(chrono, prev);
                }
            } else {
                prev = setDayOfWeek(chrono, prev);
                if (prev >= instant) {
                    prev = chrono.year().add(prev, -1);
                    prev = chrono.monthOfYear().set(prev, iMonthOfYear);
                    prev = setDayOfMonthPrevious(chrono, prev);
                    prev = setDayOfWeek(chrono, prev);
                }
            }

            // Convert from local time to UTC.
            return prev - offset;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof OfYear) {
                OfYear other = (OfYear)obj;
                return
                    iMode == other.iMode &&
                    iMonthOfYear == other.iMonthOfYear &&
                    iDayOfMonth == other.iDayOfMonth &&
                    iDayOfWeek == other.iDayOfWeek &&
                    iAdvance == other.iAdvance &&
                    iMillisOfDay == other.iMillisOfDay;
            }
            return false;
        }

        /*
        public String toString() {
            return
                "[OfYear]\n" + 
                "Mode: " + iMode + '\n' +
                "MonthOfYear: " + iMonthOfYear + '\n' +
                "DayOfMonth: " + iDayOfMonth + '\n' +
                "DayOfWeek: " + iDayOfWeek + '\n' +
                "AdvanceDayOfWeek: " + iAdvance + '\n' +
                "MillisOfDay: " + iMillisOfDay + '\n';
        }
        */

        /**
         * If month-day is 02-29 and year isn't leap, advances to next leap year.
         */
        private long setDayOfMonthNext(Chronology chrono, long next) {
            try {
                next = setDayOfMonth(chrono, next);
            } catch (IllegalArgumentException e) {
                if (iMonthOfYear == 2 && iDayOfMonth == 29) {
                    while (chrono.year().isLeap(next) == false) {
                        next = chrono.year().add(next, 1);
                    }
                    next = setDayOfMonth(chrono, next);
                } else {
                    throw e;
                }
            }
            return next;
        }

        /**
         * If month-day is 02-29 and year isn't leap, retreats to previous leap year.
         */
        private long setDayOfMonthPrevious(Chronology chrono, long prev) {
            try {
                prev = setDayOfMonth(chrono, prev);
            } catch (IllegalArgumentException e) {
                if (iMonthOfYear == 2 && iDayOfMonth == 29) {
                    while (chrono.year().isLeap(prev) == false) {
                        prev = chrono.year().add(prev, -1);
                    }
                    prev = setDayOfMonth(chrono, prev);
                } else {
                    throw e;
                }
            }
            return prev;
        }

        private long setDayOfMonth(Chronology chrono, long instant) {
            if (iDayOfMonth >= 0) {
                instant = chrono.dayOfMonth().set(instant, iDayOfMonth);
            } else {
                instant = chrono.dayOfMonth().set(instant, 1);
                instant = chrono.monthOfYear().add(instant, 1);
                instant = chrono.dayOfMonth().add(instant, iDayOfMonth);
            }
            return instant;
        }

        private long setDayOfWeek(Chronology chrono, long instant) {
            int dayOfWeek = chrono.dayOfWeek().get(instant);
            int daysToAdd = iDayOfWeek - dayOfWeek;
            if (daysToAdd != 0) {
                if (iAdvance) {
                    if (daysToAdd < 0) {
                        daysToAdd += 7;
                    }
                } else {
                    if (daysToAdd > 0) {
                        daysToAdd -= 7;
                    }
                }
                instant = chrono.dayOfWeek().add(instant, daysToAdd);
            }
            return instant;
        }
    }

    /**
     * Extends OfYear with a nameKey and savings.
     */
    private static final class Recurrence {
        final OfYear iOfYear;
        final String iNameKey;
        final int iSaveMillis;

        Recurrence(OfYear ofYear, String nameKey, int saveMillis) {
            iOfYear = ofYear;
            iNameKey = nameKey;
            iSaveMillis = saveMillis;
        }

        public OfYear getOfYear() {
            return iOfYear;
        }

        /**
         * @param standardOffset standard offset just before next recurrence
         */
        public long next(long instant, int standardOffset, int saveMillis) {
            return iOfYear.next(instant, standardOffset, saveMillis);
        }

        /**
         * @param standardOffset standard offset just before previous recurrence
         */
        public long previous(long instant, int standardOffset, int saveMillis) {
            return iOfYear.previous(instant, standardOffset, saveMillis);
        }

        public String getNameKey() {
            return iNameKey;
        }

        public int getSaveMillis() {
            return iSaveMillis;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Recurrence) {
                Recurrence other = (Recurrence)obj;
                return
                    iSaveMillis == other.iSaveMillis &&
                    iNameKey.equals(other.iNameKey) &&
                    iOfYear.equals(other.iOfYear);
            }
            return false;
        }

        Recurrence rename(String nameKey) {
            return new Recurrence(iOfYear, nameKey, iSaveMillis);
        }

        Recurrence renameAppend(String appendNameKey) {
            return rename((iNameKey + appendNameKey).intern());
        }
    }

    /**
     * Extends Recurrence with inclusive year limits.
     */
    private static final class Rule {
        final Recurrence iRecurrence;
        final int iFromYear; // inclusive
        final int iToYear;   // inclusive

        Rule(Recurrence recurrence, int fromYear, int toYear) {
            iRecurrence = recurrence;
            iFromYear = fromYear;
            iToYear = toYear;
        }

        public int getFromYear() {
            return iFromYear;
        }

        public int getToYear() {
            return iToYear;
        }

        public OfYear getOfYear() {
            return iRecurrence.getOfYear();
        }

        public String getNameKey() {
            return iRecurrence.getNameKey();
        }

        public int getSaveMillis() {
            return iRecurrence.getSaveMillis();
        }

        public long next(final long instant, int standardOffset, int saveMillis) {
            Chronology chrono = ISOChronology.getInstanceUTC();

            final int wallOffset = standardOffset + saveMillis;
            long testInstant = instant;

            int year;
            if (instant == Long.MIN_VALUE) {
                year = Integer.MIN_VALUE;
            } else {
                year = chrono.year().get(instant + wallOffset);
            }

            if (year < iFromYear) {
                // First advance instant to start of from year.
                testInstant = chrono.year().set(0, iFromYear) - wallOffset;
                // Back off one millisecond to account for next recurrence
                // being exactly at the beginning of the year.
                testInstant -= 1;
            }

            long next = iRecurrence.next(testInstant, standardOffset, saveMillis);

            if (next > instant) {
                year = chrono.year().get(next + wallOffset);
                if (year > iToYear) {
                    // Out of range, return original value.
                    next = instant;
                }
            }

            return next;
        }
    }

    private static final class Transition {
        private final long iMillis;
        private final String iNameKey;
        private final int iWallOffset;
        private final int iStandardOffset;

        Transition(long millis, Transition tr) {
            iMillis = millis;
            iNameKey = tr.iNameKey;
            iWallOffset = tr.iWallOffset;
            iStandardOffset = tr.iStandardOffset;
        }

        Transition(long millis, Rule rule, int standardOffset) {
            iMillis = millis;
            iNameKey = rule.getNameKey();
            iWallOffset = standardOffset + rule.getSaveMillis();
            iStandardOffset = standardOffset;
        }

        Transition(long millis, String nameKey,
                   int wallOffset, int standardOffset) {
            iMillis = millis;
            iNameKey = nameKey;
            iWallOffset = wallOffset;
            iStandardOffset = standardOffset;
        }

        public long getMillis() {
            return iMillis;
        }

        public String getNameKey() {
            return iNameKey;
        }

        public int getWallOffset() {
            return iWallOffset;
        }

        public int getStandardOffset() {
            return iStandardOffset;
        }

        public int getSaveMillis() {
            return iWallOffset - iStandardOffset;
        }

        /**
         * There must be a change in the millis, wall offsets or name keys.
         */
        public boolean isTransitionFrom(Transition other) {
            if (other == null) {
                return true;
            }
            return iMillis > other.iMillis &&
                (iWallOffset != other.iWallOffset ||
                 //iStandardOffset != other.iStandardOffset ||
                 !(iNameKey.equals(other.iNameKey)));
        }
    }

    private static final class RuleSet {
        private static final int YEAR_LIMIT;

        static {
            // Don't pre-calculate more than 100 years into the future. Almost
            // all zones will stop pre-calculating far sooner anyhow. Either a
            // simple DST cycle is detected or the last rule is a fixed
            // offset. If a zone has a fixed offset set more than 100 years
            // into the future, then it won't be observed.
            long now = DateTimeUtils.currentTimeMillis();
            YEAR_LIMIT = ISOChronology.getInstanceUTC().year().get(now) + 100;
        }

        private int iStandardOffset;
        private ArrayList iRules;

        // Optional.
        private String iInitialNameKey;
        private int iInitialSaveMillis;

        // Upper limit is exclusive.
        private int iUpperYear;
        private OfYear iUpperOfYear;

        RuleSet() {
            iRules = new ArrayList(10);
            iUpperYear = Integer.MAX_VALUE;
        }

        /**
         * Copy constructor.
         */
        RuleSet(RuleSet rs) {
            iStandardOffset = rs.iStandardOffset;
            iRules = new ArrayList(rs.iRules);
            iInitialNameKey = rs.iInitialNameKey;
            iInitialSaveMillis = rs.iInitialSaveMillis;
            iUpperYear = rs.iUpperYear;
            iUpperOfYear = rs.iUpperOfYear;
        }

        public int getStandardOffset() {
            return iStandardOffset;
        }

        public void setStandardOffset(int standardOffset) {
            iStandardOffset = standardOffset;
        }

        public void setFixedSavings(String nameKey, int saveMillis) {
            iInitialNameKey = nameKey;
            iInitialSaveMillis = saveMillis;
        }

        public void addRule(Rule rule) {
            if (!iRules.contains(rule)) {
                iRules.add(rule);
            }
        }

        public void setUpperLimit(int year, OfYear ofYear) {
            iUpperYear = year;
            iUpperOfYear = ofYear;
        }

        /**
         * Returns a transition at firstMillis with the first name key and
         * offsets for this rule set. This method may return null.
         *
         * @param firstMillis millis of first transition
         */
        public Transition firstTransition(final long firstMillis) {
            if (iInitialNameKey != null) {
                // Initial zone info explicitly set, so don't search the rules.
                return new Transition(firstMillis, iInitialNameKey,
                                      iStandardOffset + iInitialSaveMillis, iStandardOffset);
            }

            // Make a copy before we destroy the rules.
            ArrayList copy = new ArrayList(iRules);

            // Iterate through all the transitions until firstMillis is
            // reached. Use the name key and savings for whatever rule reaches
            // the limit.

            long millis = Long.MIN_VALUE;
            int saveMillis = 0;
            Transition first = null;

            Transition next;
            while ((next = nextTransition(millis, saveMillis)) != null) {
                millis = next.getMillis();

                if (millis == firstMillis) {
                    first = new Transition(firstMillis, next);
                    break;
                }

                if (millis > firstMillis) {
                    if (first == null) {
                        // Find first rule without savings. This way a more
                        // accurate nameKey is found even though no rule
                        // extends to the RuleSet's lower limit.
                        Iterator it = copy.iterator();
                        while (it.hasNext()) {
                            Rule rule = (Rule)it.next();
                            if (rule.getSaveMillis() == 0) {
                                first = new Transition(firstMillis, rule, iStandardOffset);
                                break;
                            }
                        }
                    }
                    if (first == null) {
                        // Found no rule without savings. Create a transition
                        // with no savings anyhow, and use the best available
                        // name key.
                        first = new Transition(firstMillis, next.getNameKey(),
                                               iStandardOffset, iStandardOffset);
                    }
                    break;
                }
                
                // Set first to the best transition found so far, but next
                // iteration may find something closer to lower limit.
                first = new Transition(firstMillis, next);

                saveMillis = next.getSaveMillis();
            }

            iRules = copy;
            return first;
        }

        /**
         * Returns null if RuleSet is exhausted or upper limit reached. Calling
         * this method will throw away rules as they each become
         * exhausted. Copy the RuleSet before using it to compute transitions.
         *
         * Returned transition may be a duplicate from previous
         * transition. Caller must call isTransitionFrom to filter out
         * duplicates.
         *
         * @param saveMillis savings before next transition
         */
        public Transition nextTransition(final long instant, final int saveMillis) {
            Chronology chrono = ISOChronology.getInstanceUTC();

            // Find next matching rule.
            Rule nextRule = null;
            long nextMillis = Long.MAX_VALUE;
            
            Iterator it = iRules.iterator();
            while (it.hasNext()) {
                Rule rule = (Rule)it.next();
                long next = rule.next(instant, iStandardOffset, saveMillis);
                if (next <= instant) {
                    it.remove();
                    continue;
                }
                // Even if next is same as previous next, choose the rule
                // in order for more recently added rules to override.
                if (next <= nextMillis) {
                    // Found a better match.
                    nextRule = rule;
                    nextMillis = next;
                }
            }
            
            if (nextRule == null) {
                return null;
            }
            
            // Stop precalculating if year reaches some arbitrary limit.
            if (chrono.year().get(nextMillis) >= YEAR_LIMIT) {
                return null;
            }
            
            // Check if upper limit reached or passed.
            if (iUpperYear < Integer.MAX_VALUE) {
                long upperMillis =
                    iUpperOfYear.setInstant(iUpperYear, iStandardOffset, saveMillis);
                if (nextMillis >= upperMillis) {
                    // At or after upper limit.
                    return null;
                }
            }
            
            return new Transition(nextMillis, nextRule, iStandardOffset);
        }

        /**
         * @param saveMillis savings before upper limit
         */
        public long getUpperLimit(int saveMillis) {
            if (iUpperYear == Integer.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            return iUpperOfYear.setInstant(iUpperYear, iStandardOffset, saveMillis);
        }

        /**
         * Returns null if none can be built.
         */
        public DSTZone buildTailZone(String id) {
            if (iRules.size() == 2) {
                Rule startRule = (Rule)iRules.get(0);
                Rule endRule = (Rule)iRules.get(1);
                if (startRule.getToYear() == Integer.MAX_VALUE &&
                    endRule.getToYear() == Integer.MAX_VALUE) {

                    // With exactly two infinitely recurring rules left, a
                    // simple DSTZone can be formed.

                    // The order of rules can come in any order, and it doesn't
                    // really matter which rule was chosen the 'start' and
                    // which is chosen the 'end'. DSTZone works properly either
                    // way.
                    return new DSTZone(id, iStandardOffset,
                                       startRule.iRecurrence, endRule.iRecurrence);
                }
            }
            return null;
        }
    }

    private static final class DSTZone extends DateTimeZone {
        private static final long serialVersionUID = 6941492635554961361L;

        final int iStandardOffset;
        final Recurrence iStartRecurrence;
        final Recurrence iEndRecurrence;

        DSTZone(String id, int standardOffset,
                Recurrence startRecurrence, Recurrence endRecurrence) {
            super(id);
            iStandardOffset = standardOffset;
            iStartRecurrence = startRecurrence;
            iEndRecurrence = endRecurrence;
        }

        public String getNameKey(long instant) {
            return findMatchingRecurrence(instant).getNameKey();
        }

        public int getOffset(long instant) {
            return iStandardOffset + findMatchingRecurrence(instant).getSaveMillis();
        }

        public int getStandardOffset(long instant) {
            return iStandardOffset;
        }

        public boolean isFixed() {
            return false;
        }

        public long nextTransition(long instant) {
            int standardOffset = iStandardOffset;
            Recurrence startRecurrence = iStartRecurrence;
            Recurrence endRecurrence = iEndRecurrence;

            long start, end;

            try {
                start = startRecurrence.next
                    (instant, standardOffset, endRecurrence.getSaveMillis());
                if (instant > 0 && start < 0) {
                    // Overflowed.
                    start = instant;
                }
            } catch (IllegalArgumentException e) {
                // Overflowed.
                start = instant;
            } catch (ArithmeticException e) {
                // Overflowed.
                start = instant;
            }

            try {
                end = endRecurrence.next
                    (instant, standardOffset, startRecurrence.getSaveMillis());
                if (instant > 0 && end < 0) {
                    // Overflowed.
                    end = instant;
                }
            } catch (IllegalArgumentException e) {
                // Overflowed.
                end = instant;
            } catch (ArithmeticException e) {
                // Overflowed.
                end = instant;
            }

            return (start > end) ? end : start;
        }

        public long previousTransition(long instant) {
            // Increment in order to handle the case where instant is exactly at
            // a transition.
            instant++;

            int standardOffset = iStandardOffset;
            Recurrence startRecurrence = iStartRecurrence;
            Recurrence endRecurrence = iEndRecurrence;

            long start, end;

            try {
                start = startRecurrence.previous
                    (instant, standardOffset, endRecurrence.getSaveMillis());
                if (instant < 0 && start > 0) {
                    // Overflowed.
                    start = instant;
                }
            } catch (IllegalArgumentException e) {
                // Overflowed.
                start = instant;
            } catch (ArithmeticException e) {
                // Overflowed.
                start = instant;
            }

            try {
                end = endRecurrence.previous
                    (instant, standardOffset, startRecurrence.getSaveMillis());
                if (instant < 0 && end > 0) {
                    // Overflowed.
                    end = instant;
                }
            } catch (IllegalArgumentException e) {
                // Overflowed.
                end = instant;
            } catch (ArithmeticException e) {
                // Overflowed.
                end = instant;
            }

            return ((start > end) ? start : end) - 1;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DSTZone) {
                DSTZone other = (DSTZone)obj;
                return
                    getID().equals(other.getID()) &&
                    iStandardOffset == other.iStandardOffset &&
                    iStartRecurrence.equals(other.iStartRecurrence) &&
                    iEndRecurrence.equals(other.iEndRecurrence);
            }
            return false;
        }

        private Recurrence findMatchingRecurrence(long instant) {
            int standardOffset = iStandardOffset;
            Recurrence startRecurrence = iStartRecurrence;
            Recurrence endRecurrence = iEndRecurrence;

            long start, end;

            try {
                start = startRecurrence.next
                    (instant, standardOffset, endRecurrence.getSaveMillis());
            } catch (IllegalArgumentException e) {
                // Overflowed.
                start = instant;
            } catch (ArithmeticException e) {
                // Overflowed.
                start = instant;
            }

            try {
                end = endRecurrence.next
                    (instant, standardOffset, startRecurrence.getSaveMillis());
            } catch (IllegalArgumentException e) {
                // Overflowed.
                end = instant;
            } catch (ArithmeticException e) {
                // Overflowed.
                end = instant;
            }

            return (start > end) ? startRecurrence : endRecurrence;
        }
    }

    private static final class PrecalculatedZone extends DateTimeZone {
        private static final long serialVersionUID = 7811976468055766265L;

        /**
         * Factory to create instance from builder.
         * 
         * @param id  the zone id
         * @param outputID  true if the zone id should be output
         * @param transitions  the list of Transition objects
         * @param tailZone  optional zone for getting info beyond precalculated tables
         */
        static PrecalculatedZone create(String id, boolean outputID, ArrayList transitions, DSTZone tailZone) {
            int size = transitions.size();
            if (size == 0) {
                throw new IllegalArgumentException();
            }

            long[] trans = new long[size];
            int[] wallOffsets = new int[size];
            int[] standardOffsets = new int[size];
            String[] nameKeys = new String[size];

            Transition last = null;
            for (int i=0; i<size; i++) {
                Transition tr = (Transition)transitions.get(i);

                if (!tr.isTransitionFrom(last)) {
                    throw new IllegalArgumentException(id);
                }

                trans[i] = tr.getMillis();
                wallOffsets[i] = tr.getWallOffset();
                standardOffsets[i] = tr.getStandardOffset();
                nameKeys[i] = tr.getNameKey();

                last = tr;
            }

            // Some timezones (Australia) have the same name key for
            // summer and winter which messes everything up. Fix it here.
            String[] zoneNameData = new String[5];
            String[][] zoneStrings = TimeZoneList.getTimeZones();
            for (int j = 0; j < zoneStrings.length; j++) {
                String[] set = zoneStrings[j];
                if (set != null && set.length == 5 && id.equals(set[0])) {
                    zoneNameData = set;
                }
            }
            for (int i = 0; i < nameKeys.length - 1; i++) {
                String curNameKey = nameKeys[i];
                String nextNameKey = nameKeys[i + 1];
                long curOffset = wallOffsets[i];
                long nextOffset = wallOffsets[i + 1];
                long curStdOffset = standardOffsets[i];
                long nextStdOffset = standardOffsets[i + 1];
                Period p = new Period(trans[i], trans[i + 1], PeriodType.yearMonthDay());
                if (curOffset != nextOffset &&
                        curStdOffset == nextStdOffset &&
                        curNameKey.equals(nextNameKey) &&
                        p.getYears() == 0 && p.getMonths() > 4 && p.getMonths() < 8 &&
                        curNameKey.equals(zoneNameData[2]) &&
                        curNameKey.equals(zoneNameData[4])) {
                    
                    System.out.println("Fixing duplicate name key - " + nextNameKey);
                    System.out.println("     - " + new DateTime(trans[i]) + " - " + new DateTime(trans[i + 1]));
                    if (curOffset > nextOffset) {
                        nameKeys[i] = (curNameKey + "-Summer").intern();
                    } else if (curOffset < nextOffset) {
                        nameKeys[i + 1] = (nextNameKey + "-Summer").intern();
                        i++;
                    }
                }
            }
            if (tailZone != null) {
                if (tailZone.iStartRecurrence.getNameKey().equals(tailZone.iEndRecurrence.getNameKey())) {
                    System.out.println("Fixing duplicate recurrent name key - " + tailZone.iStartRecurrence.getNameKey());
                    if (tailZone.iStartRecurrence.getSaveMillis() > 0) {
                        tailZone = new DSTZone(
                            tailZone.getID(),
                            tailZone.iStandardOffset,
                            tailZone.iStartRecurrence.renameAppend("-Summer"),
                            tailZone.iEndRecurrence);
                    } else {
                        tailZone = new DSTZone(
                            tailZone.getID(),
                            tailZone.iStandardOffset,
                            tailZone.iStartRecurrence,
                            tailZone.iEndRecurrence.renameAppend("-Summer"));
                    }
                }
            }
            
            return new PrecalculatedZone((outputID ? id : ""), trans, wallOffsets, standardOffsets, nameKeys, tailZone);
        }

        // All array fields have the same length.

        private final long[] iTransitions;

        private final int[] iWallOffsets;
        private final int[] iStandardOffsets;
        private final String[] iNameKeys;

        private final DSTZone iTailZone;

        /**
         * Constructor used ONLY for valid input, loaded via static methods.
         */
        private PrecalculatedZone(String id, long[] transitions, int[] wallOffsets,
                          int[] standardOffsets, String[] nameKeys, DSTZone tailZone)
        {
            super(id);
            iTransitions = transitions;
            iWallOffsets = wallOffsets;
            iStandardOffsets = standardOffsets;
            iNameKeys = nameKeys;
            iTailZone = tailZone;
        }

        public String getNameKey(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            if (i >= 0) {
                return iNameKeys[i];
            }
            i = ~i;
            if (i < transitions.length) {
                if (i > 0) {
                    return iNameKeys[i - 1];
                }
                return "UTC";
            }
            if (iTailZone == null) {
                return iNameKeys[i - 1];
            }
            return iTailZone.getNameKey(instant);
        }

        public int getOffset(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            if (i >= 0) {
                return iWallOffsets[i];
            }
            i = ~i;
            if (i < transitions.length) {
                if (i > 0) {
                    return iWallOffsets[i - 1];
                }
                return 0;
            }
            if (iTailZone == null) {
                return iWallOffsets[i - 1];
            }
            return iTailZone.getOffset(instant);
        }

        public int getStandardOffset(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            if (i >= 0) {
                return iStandardOffsets[i];
            }
            i = ~i;
            if (i < transitions.length) {
                if (i > 0) {
                    return iStandardOffsets[i - 1];
                }
                return 0;
            }
            if (iTailZone == null) {
                return iStandardOffsets[i - 1];
            }
            return iTailZone.getStandardOffset(instant);
        }

        public boolean isFixed() {
            return false;
        }

        public long nextTransition(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            i = (i >= 0) ? (i + 1) : ~i;
            if (i < transitions.length) {
                return transitions[i];
            }
            if (iTailZone == null) {
                return instant;
            }
            long end = transitions[transitions.length - 1];
            if (instant < end) {
                instant = end;
            }
            return iTailZone.nextTransition(instant);
        }

        public long previousTransition(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            if (i >= 0) {
                if (instant > Long.MIN_VALUE) {
                    return instant - 1;
                }
                return instant;
            }
            i = ~i;
            if (i < transitions.length) {
                if (i > 0) {
                    long prev = transitions[i - 1];
                    if (prev > Long.MIN_VALUE) {
                        return prev - 1;
                    }
                }
                return instant;
            }
            if (iTailZone != null) {
                long prev = iTailZone.previousTransition(instant);
                if (prev < instant) {
                    return prev;
                }
            }
            long prev = transitions[i - 1];
            if (prev > Long.MIN_VALUE) {
                return prev - 1;
            }
            return instant;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PrecalculatedZone) {
                PrecalculatedZone other = (PrecalculatedZone)obj;
                return
                    getID().equals(other.getID()) &&
                    Arrays.equals(iTransitions, other.iTransitions) &&
                    Arrays.equals(iNameKeys, other.iNameKeys) &&
                    Arrays.equals(iWallOffsets, other.iWallOffsets) &&
                    Arrays.equals(iStandardOffsets, other.iStandardOffsets) &&
                    ((iTailZone == null)
                     ? (null == other.iTailZone)
                     : (iTailZone.equals(other.iTailZone)));
            }
            return false;
        }

        public boolean isCachable() {
            if (iTailZone != null) {
                return true;
            }
            long[] transitions = iTransitions;
            if (transitions.length <= 1) {
                return false;
            }

            // Add up all the distances between transitions that are less than
            // about two years.
            double distances = 0;
            int count = 0;

            for (int i=1; i<transitions.length; i++) {
                long diff = transitions[i] - transitions[i - 1];
                if (diff < ((366L + 365) * 24 * 60 * 60 * 1000)) {
                    distances += (double)diff;
                    count++;
                }
            }

            if (count > 0) {
                double avg = distances / count;
                avg /= 24 * 60 * 60 * 1000;
                if (avg >= 25) {
                    // Only bother caching if average distance between
                    // transitions is at least 25 days. Why 25?
                    // CachedDateTimeZone is more efficient if the distance
                    // between transitions is large. With an average of 25, it
                    // will on average perform about 2 tests per cache
                    // hit. (49.7 / 25) is approximately 2.
                    return true;
                }
            }

            return false;
        }
    }
}
