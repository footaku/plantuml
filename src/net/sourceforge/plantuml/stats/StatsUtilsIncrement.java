/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2017, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 *
 *
 */
package net.sourceforge.plantuml.stats;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.PSystemError;
import net.sourceforge.plantuml.activitydiagram3.ActivityDiagram3;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.directdot.PSystemDot;
import net.sourceforge.plantuml.ditaa.PSystemDitaa;
import net.sourceforge.plantuml.eggs.PSystemEmpty;
import net.sourceforge.plantuml.jcckit.PSystemJcckit;
import net.sourceforge.plantuml.math.PSystemMath;
import net.sourceforge.plantuml.salt.PSystemSalt;
import net.sourceforge.plantuml.stats.api.Stats;
import net.sourceforge.plantuml.sudoku.PSystemSudoku;

public class StatsUtilsIncrement {

	final private static Preferences prefs = StatsUtils.prefs;

	final private static ConcurrentMap<String, ParsedGenerated> byTypeEver = StatsUtils.byTypeEver;
	final private static ConcurrentMap<String, ParsedGenerated> byTypeCurrent = StatsUtils.byTypeCurrent;

	final private static FormatCounter formatCounterCurrent = StatsUtils.formatCounterCurrent;
	final private static FormatCounter formatCounterEver = StatsUtils.formatCounterEver;

	public static void onceMoreParse(long duration, Class<? extends Diagram> type) {
		getByTypeCurrent(type).parsed().addValue(duration);
		final ParsedGenerated byTypeEver = getByTypeEver(type);
		byTypeEver.parsed().addValue(duration);
		StatsUtils.fullEver.parsed().addValue(duration);
		StatsUtils.historicalData.current().parsed().addValue(duration);

		StatsUtils.historicalData.current().parsed().save(prefs);
		StatsUtils.fullEver.parsed().save(prefs);
		byTypeEver.parsed().save(prefs);
		realTimeExport();
	}

	public static void onceMoreGenerate(long duration, Class<? extends Diagram> type, FileFormat fileFormat) {
		getByTypeCurrent(type).generated().addValue(duration);
		final ParsedGenerated byTypeEver = getByTypeEver(type);
		byTypeEver.generated().addValue(duration);
		StatsUtils.fullEver.generated().addValue(duration);
		StatsUtils.historicalData.current().generated().addValue(duration);
		formatCounterCurrent.plusOne(fileFormat, duration);
		formatCounterEver.plusOne(fileFormat, duration);

		formatCounterEver.save(prefs, fileFormat);
		StatsUtils.historicalData.current().generated().save(prefs);
		StatsUtils.fullEver.generated().save(prefs);
		byTypeEver.generated().save(prefs);
		realTimeExport();
	}

	private static ParsedGenerated getByTypeCurrent(Class<? extends Diagram> type) {
		final String name = name(type);
		ParsedGenerated n = byTypeCurrent.get(name);
		if (n == null) {
			byTypeCurrent.putIfAbsent(name, ParsedGenerated.createVolatileDated());
			n = byTypeCurrent.get(name);
		}
		return n;
	}

	private static ParsedGenerated getByTypeEver(Class<? extends Diagram> type) {
		final String name = name(type);
		ParsedGenerated n = byTypeEver.get(name);
		if (n == null) {
			byTypeEver.putIfAbsent(name, ParsedGenerated.loadDated(prefs, "type." + name));
			n = byTypeEver.get(name);
		}
		return n;
	}

	private static String name(Class<? extends Diagram> type) {
		if (type == PSystemError.class) {
			return "Error";
		}
		if (type == ActivityDiagram3.class) {
			return "ActivityDiagramBeta";
		}
		if (type == PSystemSalt.class) {
			return "Salt";
		}
		if (type == PSystemSudoku.class) {
			return "Sudoku";
		}
		if (type == PSystemDot.class) {
			return "Dot";
		}
		if (type == PSystemEmpty.class) {
			return "Welcome";
		}
		if (type == PSystemDitaa.class) {
			return "Ditaa";
		}
		if (type == PSystemJcckit.class) {
			return "Jcckit";
		}
		if (type == PSystemMath.class) {
			return "Math";
		}
		final String name = type.getSimpleName();
		if (name.endsWith("Diagram")) {
			return name;
		}
		// return "Other " + name;
		return "Other";
	}

	private static final Lock lockXml = new ReentrantLock();
	private static final Lock lockHtml = new ReentrantLock();

	private static void realTimeExport() {
		if (StatsUtils.realTimeStats) {
			final Stats stats = StatsUtils.getStatsLazzy();
			try {
				if (StatsUtils.xmlStats && lockXml.tryLock())
					try {
						StatsUtils.xmlOutput(stats);
					} finally {
						lockXml.unlock();
					}
				if (StatsUtils.htmlStats && lockHtml.tryLock())
					try {
						StatsUtils.htmlOutput(stats);
					} finally {
						lockHtml.unlock();
					}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

}
