package org.gravity.eval.runtime.tgg.modisco;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.uml2.uml.Model;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.tests.TestHelper;
import org.gravity.tgg.modisco.pm.MoDiscoTGGConverter;
import org.gravity.tgg.uml.Transformation;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class contains a JUnit test to measures the runtime of the Java ->
 * program model transformation
 *
 * @author speldszus
 *
 */
@RunWith(Parameterized.class)
public class RuntimeMeasurements {

	/*
	 * The order of this constants is important for the initialization!
	 */
	private static final Path OUTPUT = Paths.get("results.csv");
	private static final String DATE = new SimpleDateFormat().format(new Date());
	private static final Logger LOGGER = Logger.getLogger(RuntimeMeasurements.class);
	private static final List<String> OLD_LINES = readOldData();
	private static final List<String> SKIP = Arrays.asList(
			//			"00_JavaSolitaire1.3",
			//			"01_QuickUML2001",
			//			"02_JSciCalc2.1.0",
			//			"03_JUnit3.8.2",
			//			"04_JSSE_OpenJDK8",
			//			"05_Gantt1.10.2",
			//			"06_Nutch0.9",
			//			"07_Lucene1.4.3",
			//			"08_log4j1.2.17",
			//			"09_JHotDraw7.6",
			//			"10_JEdit4.0",
			//			"11_PMD3.9",
			//			"12_JTransform3.1",
			//			"13_iTrust21.0",
			//			"14_JabRef2.7",
			//			"15_Xerces2.7.0",
			//			"16_ArgoUML0.19.8",
			//			"17_jfreechart1.0.19",
			//			"18_Tomcat6.0.45",
			//			"19_Azureus2.3.0.6"
			"20_SvnKit1.8.12"
			);

	private static int oldValues;

	protected final IJavaProject project;
	protected final String name;

	/**
	 * Creates a test instance for a Java project.
	 *
	 * @param name    The name of the project
	 * @param project The project
	 */
	public RuntimeMeasurements(final String name, final IJavaProject project) {
		this.name = name;
		this.project = project;
		Logger.getRootLogger().setLevel(Level.ERROR);
	}


	@Test
	public void measurePM() throws Exception {
		final ResourceSet set = new ResourceSetImpl();
		final MoDiscoTGGConverter converter = new MoDiscoTGGConverter(this.project, set, false);
		converter.disableAutosave();

		GravityActivator.setRecordKey("pm");
		GravityActivator.record("Measure " +this.name);
		System.gc();

		final long start = System.nanoTime();
		final boolean success = converter.convertProject(new NullProgressMonitor());
		final long stop = System.nanoTime();


		GravityActivator.setRecordKey("size");
		GravityActivator.record(this.name);
		GravityActivator.record("PM: " + countElements(converter.getTrg()));

		converter.discard();
		set.getResources().forEach(Resource::unload);
		set.getResources().clear();
		assertTrue(success);
		addResults(start / 1000 / 1000, stop / 1000 / 1000);
		System.gc();
	}

	@Test
	public void measureUML() throws Exception {
		final Transformation converter = new Transformation(this.project, null, false);
		converter.disableAutosave();

		GravityActivator.setRecordKey("uml");
		GravityActivator.record("Measure " +this.name);
		System.gc();

		final long start = System.nanoTime();
		final Model model = converter.projectToModel(false, new NullProgressMonitor());
		final long stop = System.nanoTime();

		GravityActivator.setRecordKey("size");
		GravityActivator.record(this.name);
		GravityActivator.record("MoDisco: " + countElements(converter.getSrc()));
		GravityActivator.record("UML: " + countElements(converter.getTrg()));


		assertTrue(model != null);
		addResults(start / 1000 / 1000, stop / 1000 / 1000);
		final ResourceSet set = model.eResource().getResourceSet();
		set.getResources().forEach(Resource::unload);
		set.getResources().clear();

		System.gc();
	}

	private long countElements(final EObject src) {
		long i = 1;
		final TreeIterator<EObject> iterator = src.eAllContents();
		while(iterator.hasNext()) {
			i++;
			iterator.next();
		}
		return i;
	}


	/**
	 * The method for collecting the java projects from the workspace.
	 *
	 * This constructor should be only called by junit!
	 *
	 * @return The test parameters as needed by junit paramterized tests
	 * @throws CoreException
	 */
	@Parameters(name = "{index}: Forward Transformation From Src: {0}")
	public static final Collection<Object[]> data() throws CoreException {
		LOGGER.info("Collect test data");
		final List<IProject> projects = Stream.of(ResourcesPlugin.getWorkspace().getRoot().getProjects()).parallel()
				.filter(project -> !SKIP.contains(project.getName())).sorted((o1, o2) -> {
					if(o1 == o2) {
						return 0;
					}
					return o1.getName().compareTo(o2.getName())*-1;
				}).collect(Collectors.toList());
		LOGGER.info("Imported " + projects.size() + "projects into workspace.");
		return TestHelper.prepareTestData(projects);
	}

	/**
	 * Adds the results to the list of results
	 *
	 * @param start The start time stamp
	 * @param stop  The stop time stamp
	 */
	private void addResults(final long start, final long stop) {
		final String name = this.project.getProject().getName();
		int index = -1;
		StringBuilder nextLine = null;
		for (int i = 0; i < OLD_LINES.size(); i++) {
			final String line = OLD_LINES.get(i);
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
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Writes the results
	 *
	 * @throws IOException if an I/O error occurs writing to or creating the file,
	 *                     or the text cannot be encoded as UTF-8
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
			} catch (final IOException e) {
				LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
				data = Collections.emptyList();
			}

		} else {
			data = Collections.emptyList();
		}

		try {
			String next;
			if (data.size() > 0) {
				final String firstLine = data.remove(0);
				oldValues = firstLine.split(",").length;
				next = firstLine + "," + DATE;
				Files.delete(OUTPUT);

			} else {
				oldValues = 0;
				next = "project," + DATE;
			}
			Files.write(OUTPUT, (next + '\n').getBytes(), StandardOpenOption.CREATE);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return data;
	}
}
