package org.gravity.eval.runtime.tgg.modisco;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.tgg.modisco.MoDiscoTGGConverter;
import org.gravity.tgg.test.complete.AbstractParameterizedTransformationTest;
import org.junit.AfterClass;

/**
 * This class contains a JUnit test to measures the runtime of the Java -> program model transformation
 * 
 * @author speldszus
 *
 */
public class RuntimeMeasurements extends AbstractParameterizedTransformationTest {

	/*
	 * The order of this constants is important for the initialization!
	 */
	private static final Path OUTPUT = Paths.get("results.csv");
	private static final String DATE = new SimpleDateFormat().format(new Date());
	private static final Logger LOGGER = Logger.getLogger(RuntimeMeasurements.class);
	private static final List<String> OLD_LINES = readOldData();

	private static int oldValues;

	/**
	 * Creates a test instance for a Java project.
	 * 
	 * @param name The name of the project
	 * @param project The project
	 */
	public RuntimeMeasurements(String name, IJavaProject project) {
		super(name, project);
		Logger.getRootLogger().setLevel(Level.ERROR);
	}

	@Override
	public void testForward() throws Exception {
		MoDiscoTGGConverter converter = new MoDiscoTGGConverter();
		long start = System.nanoTime();
		boolean success = converter.convertProject(project, new NullProgressMonitor());
		long stop = System.nanoTime();
		assertTrue(success);
		addResults(start / 1000 / 1000, stop / 1000 / 1000);
	}

	/**
	 * Adds the results to the list of results
	 * 
	 * @param start The start time stamp
	 * @param stop  The stop time stamp
	 */
	private void addResults(long start, long stop) {
		String name = project.getProject().getName();
		int index = -1;
		StringBuilder nextLine = null;
		for (int i = 0; i < OLD_LINES.size(); i++) {
			String line = OLD_LINES.get(i);
			final int indexOf = line.indexOf(',');
			if (name.equals(line.substring(0, indexOf))) {
				nextLine = new StringBuilder(line);
				index = i;
				break;
			}
		}
		if (nextLine == null) {
			nextLine = new StringBuilder(name);
			for (int j = 0; j < oldValues; j++) {
				nextLine.append(',');
			}
		}
		nextLine.append(',');
		nextLine.append(stop - start);
		nextLine.append('\n');
		try {
			Files.write(OUTPUT, nextLine.toString().getBytes(), StandardOpenOption.APPEND);
			if (index != -1) {
				OLD_LINES.remove(index);
			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Writes the results
	 * 
	 * @throws IOException if an I/O error occurs writing to or creating the file, or the text cannot be encoded as UTF-8
	 */
	@AfterClass
	public static void write() throws IOException {
		Files.write(OUTPUT, OLD_LINES, StandardOpenOption.APPEND);
	}

	/**
	 * Reads all lines from the output folder
	 * 
	 * @return All lines of the output folder
	 */
	private static List<String> readOldData() {
		List<String> data;
		if (Files.exists(OUTPUT)) {
			try {
				data = Files.readAllLines(OUTPUT);
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
				data = Collections.emptyList();
			}
	
		} else {
			data = Collections.emptyList();
		}
	
		try {
			String next;
			if (data.size() > 0) {
				String firstLine = data.remove(0);
				oldValues = firstLine.split(",").length;
				next = firstLine + "," + DATE;
				Files.delete(OUTPUT);
	
			} else {
				oldValues = 0;
				next = "project," + DATE;
			}
			Files.write(OUTPUT, (next + '\n').getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return data;
	}
}
