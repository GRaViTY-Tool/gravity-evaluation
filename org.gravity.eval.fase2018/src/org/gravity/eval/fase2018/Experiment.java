package org.gravity.eval.fase2018;

import at.ac.tuwien.big.momot.TransformationResultManager;
import at.ac.tuwien.big.momot.problem.solution.variable.UnitApplicationVariable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.gravity.goblin.SearchParameters;
import org.gravity.goblin.fitness.VisibilityCalculator;
import org.gravity.goblin.momot.SearchTypeGraph;
import org.gravity.goblin.repair.VisibilityReducer;
import org.gravity.hulk.HAntiPatternDetection;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.HulkFactory;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HAntiPatternGraph;
import org.gravity.hulk.antipatterngraph.HMetric;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.antipatterngraph.impl.AntipatterngraphFactoryImpl;
import org.gravity.hulk.antipatterngraph.metrics.HLCOM5Metric;
import org.gravity.hulk.antipatterngraph.metrics.HTotalCouplingMetric;
import org.gravity.hulk.detection.HulkDetector;
import org.gravity.hulk.detection.antipattern.AntipatternPackage;
import org.gravity.hulk.detection.antipattern.HBlobDetector;
import org.gravity.hulk.detection.metrics.HLcom5Calculator;
import org.gravity.hulk.detection.metrics.HTotalCouplingCalculator;
import org.gravity.hulk.detection.metrics.MetricsPackage;
import org.gravity.typegraph.basic.TAbstractType;
import org.gravity.typegraph.basic.TClass;
import org.gravity.typegraph.basic.TPackage;
import org.gravity.typegraph.basic.TypeGraph;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;

@RunWith(Parameterized.class)
public class Experiment {

	private File file;
	private String time;

	private double cbo, lcom, blobs, visibility, members;

	public Experiment(String name, File file, String time, double cbo, double lcom, double blobs, double visibility,
			double members) {
		this.file = file;
		this.time = time;
		this.cbo = cbo;
		this.lcom = lcom;
		this.blobs = blobs;
		this.visibility = visibility;
		this.members = members;
	}
	
	
//------------Tests---------------------------
	
	@Test
	public void differentWeights() throws IOException{
		String exp = "differentWeightsExp";
		boolean saveReducedVisibility = true;
		boolean useOptimization = true;

		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 2.0);
		SearchParameters.publicValue = 5;
		runExperiment(exp + "_2_5", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 2.0);
		SearchParameters.publicValue = 7;
		runExperiment(exp + "_2_7", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 2.0);
		SearchParameters.publicValue = 10;
		runExperiment(exp + "_2_10", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 5.0);
		SearchParameters.publicValue = 3;
		runExperiment(exp + "_5_3", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 5.0);
		SearchParameters.publicValue = 5;
		runExperiment(exp + "_5_5", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 5.0);
		SearchParameters.publicValue = 7;
		runExperiment(exp + "_5_7", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 5.0);
		SearchParameters.publicValue = 10;
		runExperiment(exp + "_5_10", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 7.0);
		SearchParameters.publicValue = 3;
		runExperiment(exp + "_7_3", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 7.0);
		SearchParameters.publicValue = 5;
		runExperiment(exp + "_7_5", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 7.0);
		SearchParameters.publicValue = 7;
		runExperiment(exp + "_7_7", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 7.0);
		SearchParameters.publicValue = 10;
		runExperiment(exp + "_7_10", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 10.0);
		SearchParameters.publicValue = 3;
		runExperiment(exp + "_10_3", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 10.0);
		SearchParameters.publicValue = 5;
		runExperiment(exp + "_10_5", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 10.0);
		SearchParameters.publicValue = 7;
		runExperiment(exp + "_10_7", saveReducedVisibility, useOptimization);
		
		//-------------------------------------------------------------
		SearchParameters.weight.set(4, 10.0);
		SearchParameters.publicValue = 10;
		runExperiment(exp + "_10_10", saveReducedVisibility, useOptimization);
		
	}
	
	//@Test
		public void RunTenTimes() throws IOException{
			String exp = "exp10Times_run";
			boolean saveReducedVisibility = true;
			boolean useOptimization = true;
			for(int i = 0; i < 10; i++) {
				runExperiment(exp + String.valueOf(i+1), saveReducedVisibility, useOptimization);
			}
		}
		
		/**
		 * Experiment 1 - Global visibility reduction before all refactorings
		 * 
		 * @throws IOException
		 */
		//@Test
		public void exp1() throws IOException {
			String exp = "exp1";
			boolean saveReducedVisibility = true;
			boolean useOptimizationRepair = false;
			runExperiment(exp, saveReducedVisibility, useOptimizationRepair);/*
			NeededResources resources = initializeResources(exp);
			VisibilityReducer.reduce(resources.pg);
			double before = resources.visibilityCalculator.calculate(resources.pg);
			resources.pg.eResource().save(new FileOutputStream(resources.model), Collections.EMPTY_MAP);
			TransformationResultManager results = performSearch(false, resources);		
			saveResults(results, resources, before, exp);
			unload(resources.rs);*/
		}
		


		/**
		 * Experiment 2 - Global visibility reduction after all refactorings
		 * 
		 * @throws IOException
		 */
		//@Test
		public void exp2() throws IOException {
			String exp = "exp2";
			boolean saveReducedVisibility = false;
			boolean useOptimizationRepair = false;
			runExperiment(exp, saveReducedVisibility, useOptimizationRepair);/*
			NeededResources resources = initializeResources(exp);
			VisibilityReducer.reduce(resources.pg);	
			double reduced = resources.visibilityCalculator.calculate(resources.pg);
			TransformationResultManager results = performSearch(false, resources);
			saveResults(results, resources, reduced, exp);
			unload(resources.rs);*/
		}

		/**
		 * Experiment 3 - reduce visibility in each step
		 * 
		 * @throws IOException
		 */
		//@Test
		public void exp3() throws IOException {
			String exp = "exp3";
			boolean saveReducedVisibility = true;
			boolean useOptimizationRepair = true;
			runExperiment(exp, saveReducedVisibility, useOptimizationRepair);/*
			NeededResources resources = initializeResources(exp);
			VisibilityReducer.reduce(resources.pg);
			double before = resources.visibilityCalculator.calculate(resources.pg);
			resources.pg.eResource().save(new FileOutputStream(resources.model), Collections.EMPTY_MAP);
			TransformationResultManager results = performSearch(true, resources);
			saveResults(results, resources, before, exp);
			unload(resources.rs);*/
		}
		
		
//-------------------------------------Util--------------------

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
			ResourceSetImpl rs = new ResourceSetImpl();
			Resource r = rs.createResource(URI.createFileURI(f.getAbsolutePath()));
			try {
				r.load(Collections.EMPTY_MAP);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			TypeGraph pg = (TypeGraph) r.getContents().get(0);
			List<Double> metrics = calcMetrics(pg);

			param.add(new Object[] { pg.getTName(), f, time, metrics.get(METRICS.CBO.getId()),
					metrics.get(METRICS.LCOM.getId()), metrics.get(METRICS.BLOBS.getId()),
					metrics.get(METRICS.VISIBILITY.getId()), metrics.get(METRICS.MEMBERS.getId()) });

			unload(rs);
		}
		return param;
	}
	
	

	
	private class NeededResources {
		public ResourceSetImpl rs;
		public Resource r;
		public TypeGraph pg;
		public File outputFolder;
		public File model;
		public VisibilityCalculator visibilityCalculator;
	}
	
	private NeededResources initializeResources(String exp) throws IOException {
		NeededResources res = new NeededResources();
		res.rs = new ResourceSetImpl();
		res.r = res.rs.createResource(URI.createFileURI(file.getAbsolutePath()));
		res.r.load(Collections.EMPTY_MAP);
		res.pg = (TypeGraph) res.r.getContents().get(0);
		res.visibilityCalculator = new VisibilityCalculator();
		res.outputFolder = createOutputFolder(exp, res.pg);
		res.model = new File(res.outputFolder, res.pg.getTName() + ".xmi");
		return res;		
	}

	private TransformationResultManager performSearch(boolean useOptimizationRepair, NeededResources resources) {
		SearchTypeGraph search = new SearchTypeGraph();
		SearchParameters.units = Arrays.asList("MoveMethod::rules::MoveMethodMain");
		SearchParameters.useOptimizationRepair = useOptimizationRepair;
		search.initializeFitnessFunctions();
		search.initializeConstraints();
		TransformationResultManager results = search.performSearch(resources.model.getAbsolutePath(), 10, resources.outputFolder);
		return results;
	}
	
	
	private void saveResults(TransformationResultManager results, NeededResources resources, double reducedVisibility, String exp) throws IOException {
		try (FileWriter s = new FileWriter(new File(resources.outputFolder, time + "_" + resources.pg.getTName() + "_"+exp+".csv"), true)) {
			s.append(
					"version;interpackage;refactorings;coupling;lcom;blobs;visibility;visibilityDelta;reducedVisibility;reducedVisibilityDelta;members\n");
			s.append("initial;0;0;" + cbo + ";" + lcom + ";" + blobs + ";" + visibility + ";0;" + reducedVisibility + ";" + (visibility-reducedVisibility)+";"
					+ members + '\n');

			int j = 0;
			double[] averages = new double[10];
			Arrays.fill(averages, 0);

			for (List<NondominatedPopulation> val : results.getResults().values()) {
				for (NondominatedPopulation pop : val) {
					for (Solution sol : pop) {
						int interpackageMoves = getNumInterPackageMoves(sol);

						File file = new File(resources.outputFolder, "models");
						String fileName = resources.pg.getTName();
						for (double obj : sol.getObjectives()) {
							fileName += "_" + obj;
						}
						fileName += ".xmi";

						file = new File(file, fileName);
						if (!file.exists()) {
							Files.write(new File(resources.outputFolder, "errors.log").toPath(),
									("Exp2: Result model file \"" + file + "\" not found.\n").getBytes(),
									StandardOpenOption.CREATE, StandardOpenOption.APPEND);
							continue;
						}

						Resource res = resources.rs.createResource(URI.createFileURI(file.getAbsolutePath()));
						res.load(Collections.EMPTY_MAP);

						TypeGraph solPG = null;
						EObject eObject = res.getContents().get(0);
						if (eObject instanceof TypeGraph) {
							solPG = (TypeGraph) eObject;
						} else if (eObject instanceof HAntiPatternDetection) {
							solPG = ((HAntiPatternDetection) eObject).getApg().getPg();

						}

						VisibilityReducer.reduce(solPG);
						double vis = resources.visibilityCalculator.calculate(solPG);

						s.append(fileName + ";" + interpackageMoves);
						averages[0] += interpackageMoves;
						double[] obj = sol.getObjectives();
						for (int i = 0; i < 5; i++) {
							s.append(';');
							if (i < obj.length) {
								s.append(Double.toString(obj[i]));
								averages[i + 1] += obj[i];
							}
						}
						s.append(";" + (visibility - obj[4]) + ";" + vis + ";" + (visibility - vis) + ";"
								+ Double.toString(members) + '\n');
						averages[6] += (visibility - obj[4]);
						averages[7] += vis;
						averages[8] += (visibility - vis);
						averages[9] += members;
						j++;
					}
				}
				s.append("average");
				for (double d : averages) {
					s.append(";" + (d / j));
				}
				s.append("\n");

			}
		}
	}
	
	public File createOutputFolder(String exp, TypeGraph pg) throws IOException {
		File outputFolder = new File(new File(new File(new File("output"), time), pg.getTName()), exp);
		outputFolder.mkdirs();
		return outputFolder;
	}
	
	private void runExperiment(String exp, boolean saveReducedVisibility, boolean useOptimization) throws IOException {
		NeededResources resources = initializeResources(exp);
		VisibilityReducer.reduce(resources.pg);
		double before = resources.visibilityCalculator.calculate(resources.pg);
		if(saveReducedVisibility) {
			resources.pg.eResource().save(new FileOutputStream(resources.model), Collections.EMPTY_MAP);
		}
		TransformationResultManager results = performSearch(useOptimization, resources);		
		saveResults(results, resources, before, exp);
		unload(resources.rs);
	}
	
	

	private int getNumInterPackageMoves(Solution sol) {
		int interpackageMoves = 0;
		try {
			for (int i = 0; i < sol.getNumberOfVariables(); i++) {
				Variable var = sol.getVariable(i);
				if (var instanceof UnitApplicationVariable) {
					TClass src = (TClass) ((UnitApplicationVariable) var).getParameterValue("sourceClass");
					TClass trg = (TClass) ((UnitApplicationVariable) var).getParameterValue("targetClass");

					TPackage cur = trg.getPackage();
					if (cur != src.getPackage()) {
						while (cur != null) {
							if (cur == src.getPackage()) {
								interpackageMoves++;
								break;
							}
							cur = cur.getParent();
						}
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return interpackageMoves;
	}

	private static void unload(ResourceSetImpl rs) {
		for (Resource r : rs.getResources()) {
			r.unload();
		}
		rs.getResources().clear();
		rs = null;
	}

	public static List<Double> calcMetrics(TypeGraph pg) {
		List<Double> values = new ArrayList<>(5);
		for (int i = 0; i < 5; i++) {
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
			if (next instanceof HTotalCouplingCalculator) {
				double fitness = 0;
				for (HAnnotation metric : next.getHAnnotation()) {
					if (metric instanceof HTotalCouplingMetric) {
						fitness += ((HMetric) metric).getValue();
					}
				}
				values.set(METRICS.CBO.getId(), fitness);
			} else if (next instanceof HLcom5Calculator) {
				double fitness = 0;
				for (HAnnotation metric : next.getHAnnotation()) {
					if (metric instanceof HLCOM5Metric) {
						fitness += ((HMetric) metric).getValue();
					}
				}
				values.set(METRICS.LCOM.getId(), fitness);
			} else if (next instanceof HBlobDetector) {
				double fitness = 0;
				for (HAnnotation metric : next.getHAnnotation()) {
					if (metric instanceof HBlobAntiPattern) {
						fitness++;

					}
				}
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

	public enum METRICS {
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
