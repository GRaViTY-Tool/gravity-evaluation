package org.gravity.eval.fase2018;

import Repair.visibility.VisibilityReducer;
import at.ac.tuwien.big.momot.TransformationResultManager;
import momotFiles.SearchParameters;
import momotFiles.SearchTypeGraph;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.gravity.hulk.HAntiPatternDetection;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.HulkFactory;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HAntiPatternGraph;
import org.gravity.hulk.antipatterngraph.HMetric;
import org.gravity.hulk.antipatterngraph.impl.AntipatterngraphFactoryImpl;
import org.gravity.hulk.detection.HulkDetector;
import org.gravity.hulk.detection.antipattern.AntipatternPackage;
import org.gravity.hulk.detection.antipattern.HBlobDetector;
import org.gravity.hulk.detection.metrics.HLcom5Calculator;
import org.gravity.hulk.detection.metrics.HTotalCouplingCalculator;
import org.gravity.hulk.detection.metrics.MetricsPackage;
import org.gravity.typegraph.basic.TAbstractType;
import org.gravity.typegraph.basic.TypeGraph;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

import FitnessCalculators.VisibilityCalculator;

@RunWith(Parameterized.class)
public class Experiment {

	private TypeGraph pg;
	private String time;

	private double cbo, lcom, blobs, visibility, members;

	public Experiment(String name, TypeGraph pg, String time, double cbo, double lcom, double blobs, double visibility,
			double members) {
		this.pg = pg;
		this.time = time;
		this.cbo = cbo;
		this.lcom = lcom;
		this.blobs = blobs;
		this.visibility = visibility;
		this.members = members;
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> params() {
		String time = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss").format(new Date(System.currentTimeMillis()));
		LinkedList<Object[]> param = new LinkedList<>();
		File[] files = new File("input").listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".xmi");
			}
		});
		for (File f : files) {
			Resource r = new ResourceSetImpl().createResource(URI.createFileURI(f.getAbsolutePath()));
			try {
				r.load(Collections.EMPTY_MAP);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			TypeGraph pg = (TypeGraph) r.getContents().get(0);
			List<Double> metrics = calcMetrics(pg);

			param.add(new Object[] {pg.getTName(), pg, time, metrics.get(METRICS.CBO.getId()), metrics.get(METRICS.LCOM.getId()),
					metrics.get(METRICS.BLOBS.getId()), metrics.get(METRICS.VISIBILITY.getId()),
					metrics.get(METRICS.MEMBERS.getId()) });
		}
		return param;
	}

	/**
	 * Experiment 1 - Global visibility reduction before all refactorings
	 * 
	 * @throws IOException
	 */
	@Test
	public void exp1() throws IOException {
		VisibilityReducer.reduce(pg);

		VisibilityCalculator visibilityCalculator = new VisibilityCalculator();
		double before = visibilityCalculator.calculate(pg);

		File outputFolder = new File(new File(new File(new File("output"), time), pg.getTName()), "exp1");
		outputFolder.mkdirs();

		File model = new File(outputFolder, pg.getTName() + ".xmi");
		pg.eResource().save(new FileOutputStream(model), Collections.EMPTY_MAP);

		SearchTypeGraph search = new SearchTypeGraph();
		SearchParameters.units = new String[] { "MoveMethod::rules::MoveMethodMain" };
		search.initializeFitnessFunctions();
		search.initializeConstraints();
		TransformationResultManager results = search.performSearch(model.getAbsolutePath(), 10, outputFolder);

		try (FileWriter s = new FileWriter(new File(outputFolder, time + "_exp1.csv"), true)) {
			s.append("version;refactorings;coupling;lcom;blobs;visibility;members\n");
			s.append("initial;0;" + cbo + ";" + lcom + ";" + blobs + ";" + visibility + ';' + members + '\n');
			s.append("reduced;0;" + cbo + ";" + lcom + ";" + blobs + ";" + before + ';' + members + '\n');

			for (List<NondominatedPopulation> val : results.getResults().values()) {
				for (NondominatedPopulation pop : val) {
					for (Solution sol : pop) {
						String fileName = pg.getTName();
						for (double obj : sol.getObjectives()) {
							fileName += "_" + obj;
						}
						fileName += ".xmi";

						s.append(fileName);
						double[] obj = sol.getObjectives();
						for (int i = 0; i < 5; i++) {
							s.append(';');
							if (i < obj.length) {
								s.append(Double.toString(obj[i]));
							}
						}
						s.append(';' + Double.toString(members) + '\n');
					}
				}

			}
		}
	}

	/**
	 * Experiment 2 - Global visibility reduction after all refactorings
	 * 
	 * @throws IOException
	 */
	@Test
	public void exp2() throws IOException {
		File outputFolder = new File(new File(new File(new File("output"), time), pg.getTName()), "exp2");
		outputFolder.mkdirs();

		outputFolder.mkdirs();

		try (FileWriter s = new FileWriter(new File(outputFolder, time + "_exp2.csv"), true)) {
			s.append("version;refactorings;coupling;lcom;blobs;visibility;visibility_reduced;members\n");
			s.append("initial;0;" + cbo + ";" + lcom + ";" + blobs + ";" + visibility + ";" + visibility + ';' + members
					+ '\n');

			File model = new File(outputFolder, pg.getTName() + ".xmi");
			pg.eResource().save(new FileOutputStream(model), Collections.EMPTY_MAP);

			SearchTypeGraph search = new SearchTypeGraph();
			SearchParameters.units = new String[] { "MoveMethod::rules::MoveMethodMain" };
			search.initializeFitnessFunctions();
			search.initializeConstraints();
			TransformationResultManager results = search.performSearch(model.getAbsolutePath(), 10, outputFolder);

			VisibilityCalculator visibilityCalculator = new VisibilityCalculator();

			for (List<NondominatedPopulation> val : results.getResults().values()) {
				for (NondominatedPopulation pop : val) {
					for (Solution sol : pop) {
						File file = new File(outputFolder, "models");
						String fileName = pg.getTName();
						for (double obj : sol.getObjectives()) {
							fileName += "_" + obj;
						}
						fileName += ".xmi";
						file = new File(file, fileName);
						if (!file.exists()) {
							Files.write(new File(outputFolder, "errors.log").toPath(),
									("Exp2: Result model file \"" + file + "\" not found.\n").getBytes(),
									StandardOpenOption.CREATE, StandardOpenOption.APPEND);
							continue;
						}

						Resource res = new ResourceSetImpl().createResource(URI.createFileURI(file.getAbsolutePath()));
						res.load(Collections.EMPTY_MAP);

						TypeGraph solPG = null;
						EObject eObject = res.getContents().get(0);
						if (eObject instanceof TypeGraph) {
							solPG = (TypeGraph) eObject;
						} else if (eObject instanceof HAntiPatternDetection) {
							solPG = ((HAntiPatternDetection) eObject).getApg().getPg();

						}

						VisibilityReducer.reduce(solPG);
						double vis = visibilityCalculator.calculate(solPG);

						s.append(fileName);
						for (double obj : sol.getObjectives()) {
							s.append(";" + obj);
						}
						s.append(';' + vis + ';' + Double.toString(members) + '\n');
					}
				}

			}
		}
	}

	/**
	 * Experiment 3 - reduce visibility in each step
	 * 
	 * @throws IOException
	 */
	@Test
	public void exp3() throws IOException {
		fail("Not implemented yet");

		File outputFolder = new File(new File(new File(new File("output"), time), pg.getTName()), "exp3");
		outputFolder.mkdirs();

		SearchParameters.units = new String[] { "MoveMethod::rules::MoveMethodMain" };
	}

	/**
	 * Experiment 4 - Visibility changes only due to refactorings
	 * 
	 * @throws IOException
	 */
	@Test
	public void exp4() throws IOException {
		File outputFolder = new File(new File(new File(new File("output"), time), pg.getTName()), "exp4");
		outputFolder.mkdirs();

		File model = new File(outputFolder, pg.getTName() + ".xmi");
		pg.eResource().save(new FileOutputStream(model), Collections.EMPTY_MAP);

		SearchTypeGraph search = new SearchTypeGraph();
		SearchParameters.units = new String[] { "MoveMethod::rules::MoveMethodMain" };
		search.initializeFitnessFunctions();
		search.initializeConstraints();
		TransformationResultManager results = search.performSearch(model.getAbsolutePath(), 10, outputFolder);

		try (FileWriter s = new FileWriter(new File(outputFolder, time + "_exp4.csv"), true)) {
			s.append("version;refactorings;coupling;lcom;blobs;visibility;members\n");
			s.append("initial;0;" + cbo + ";" + lcom + ";" + blobs + ";" + visibility + ';' + members + '\n');

			for (List<NondominatedPopulation> val : results.getResults().values()) {
				for (NondominatedPopulation pop : val) {
					for (Solution sol : pop) {
						String fileName = pg.getTName();
						for (double obj : sol.getObjectives()) {
							fileName += "_" + obj;
						}
						fileName += ".xmi";

						s.append(fileName);
						double[] obj = sol.getObjectives();
						for (int i = 0; i < 5; i++) {
							s.append(';');
							if (i < obj.length) {
								s.append(Double.toString(obj[i]));
							}
						}
						s.append(';' + Double.toString(members) + '\n');
					}
				}

			}
		}
	}

	private static List<Double> calcMetrics(TypeGraph pg) {
		List<Double> values = new ArrayList<>(5);
		for(int i = 0; i < 5; i++) {
			values.add(null);
		}

		// Calculate Hulk metrics
		HAntiPatternGraph antipatternGraph = AntipatterngraphFactoryImpl.eINSTANCE.createHAntiPatternGraph();
		antipatternGraph.setPg(pg);
		HAntiPatternDetection hulk = HulkFactory.eINSTANCE.createHAntiPatternDetection();
		hulk.setApg(antipatternGraph);
		HulkDetector hulkDetector = new HulkDetector(hulk, new Hashtable<String, String>());

		Set<EClass> selection = new HashSet<>();
		selection.add(MetricsPackage.eINSTANCE.getHLcom5Calculator());
		selection.add(MetricsPackage.eINSTANCE.getHTotalCouplingCalculator());
		selection.add(AntipatternPackage.eINSTANCE.getHBlobDetector());

		HashSet<HDetector> executed = new HashSet<HDetector>();
		hulkDetector.detectSelectedAntiPattern(selection, executed, new HashSet<HDetector>());

		for (HDetector next : executed) {
			double fitness = 0;
			for (HAnnotation metric : next.getHAnnotation()) {
				if (metric instanceof HMetric) {
					fitness += ((HMetric) metric).getValue();
				}
			}

			if (next instanceof HTotalCouplingCalculator) {
				values.set(METRICS.CBO.getId(), fitness);
			} else if (next instanceof HLcom5Calculator) {
				values.set(METRICS.LCOM.getId(), fitness);
			} else if (next instanceof HBlobDetector) {
				values.set(METRICS.BLOBS.getId(), fitness);
			}

		}

		// Calculate visibility
		VisibilityCalculator visibilityCalculator = new VisibilityCalculator();
		values.set(METRICS.VISIBILITY.getId(), visibilityCalculator.calculate(pg));

		// Count members
		double members = 0;
		for (TAbstractType tType : pg.getOwnedTypes()) {
			if (!tType.isTLib()) {
				members += tType.getDefines().size();
			}
		}
		values.set(METRICS.MEMBERS.getId(), members);

		return values;
	}

	enum METRICS {
		CBO(0), LCOM(1), BLOBS(2), VISIBILITY(3), MEMBERS(4);

		private int id;

		METRICS(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

	}
}
