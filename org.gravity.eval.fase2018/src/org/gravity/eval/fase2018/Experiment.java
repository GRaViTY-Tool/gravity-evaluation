package org.gravity.eval.fase2018;

import Repair.visibility.VisibilityReducer;
import at.ac.tuwien.big.momot.TransformationResultManager;
import momotFiles.SearchTypeGraph;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.gravity.hulk.HAntiPatternDetection;
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

	private String name;
	private String time;
	
	public Experiment(String name) {
		this.name = name;
		this.time = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss").format(new Date(System.currentTimeMillis()));
	}
	
	@Parameters(name="{index}:{0}")
	public static Collection<String[]> params(){
		LinkedList<String[]> param = new LinkedList<String[]>();
		File[] files = new File("input").listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".xmi");
			}
		});
		for(File f : files) {
			param.add(new String[] {f.getName()});
		}
		return param;
	}

	/**
	 * Experiment 1 - Global visibility reduction before all refactorings
	 * @throws IOException
	 */
	@Test
	public void exp1() throws IOException {
		Resource r = new ResourceSetImpl().createResource(URI.createFileURI("input/"+name));
		r.load(Collections.EMPTY_MAP);

		TypeGraph pg = (TypeGraph) r.getContents().get(0);

		VisibilityCalculator visibilityCalculator = new VisibilityCalculator();
		double initial = visibilityCalculator.calculate(pg);

		VisibilityReducer.reduce(pg);

		double before = visibilityCalculator.calculate(pg);

		File outputFolder = new File(new File(new File(new File("output"), pg.getTName()),time),"exp1");
		outputFolder.mkdirs();

		File model = new File(outputFolder, pg.getTName() + ".xmi");
		r.save(new FileOutputStream(model), Collections.EMPTY_MAP);

		SearchTypeGraph search = new SearchTypeGraph();
		search.initializeFitnessFunctions();
		search.initializeConstraints();
		TransformationResultManager results = search.performSearch(model.getAbsolutePath(), 10, outputFolder);

		try (FileWriter s = new FileWriter(new File(outputFolder, time+"_exp2.csv"), true)) {
			s.append("version;refactorings;coupling;cohesion;blobs;visibility\n");
			s.append("initial;0;;;;"+initial+'\n');
			s.append("reduced;0;;;;"+before+'\n');
			
			for (List<NondominatedPopulation> val : results.getResults().values()) {
				for (NondominatedPopulation pop : val) {
					for (Solution sol : pop) {
						String fileName = name;
						for(double obj : sol.getObjectives()) {
							fileName += "_"+obj;
						}
						fileName += ".xmi";
						
						s.append(fileName);
						for(double obj : sol.getObjectives()) {
							s.append(";"+obj);
						}
					}
				}

			}
		}
	}

	/**
	 * Experiment 2 - Global visibility reduction after all refactorings
	 * @throws IOException
	 */
	@Test
	public void exp2() throws IOException {
		Resource r = new ResourceSetImpl().createResource(URI.createFileURI("input/"+name));
		r.load(Collections.EMPTY_MAP);

		TypeGraph pg = (TypeGraph) r.getContents().get(0);

		VisibilityCalculator visibilityCalculator = new VisibilityCalculator();

		File outputFolder = new File(new File(new File(new File("output"), pg.getTName()),time),"exp2");
		outputFolder.mkdirs();

		outputFolder.mkdirs();

		try (FileWriter s = new FileWriter(new File(outputFolder, time+"_exp2.csv"), true)) {
			s.append("version;refactorings;coupling;cohesion;blobs;visibility;visibility_reduced\n");

			File model = new File(outputFolder, pg.getTName() + ".xmi");
			r.save(new FileOutputStream(model), Collections.EMPTY_MAP);

			SearchTypeGraph search = new SearchTypeGraph();
			search.initializeFitnessFunctions();
			search.initializeConstraints();
			TransformationResultManager results = search.performSearch(model.getAbsolutePath(), 10, outputFolder);

			for (List<NondominatedPopulation> val : results.getResults().values()) {
				for (NondominatedPopulation pop : val) {
					for (Solution sol : pop) {
						File file = new File(outputFolder, "models");
						String fileName = name;
						for(double obj : sol.getObjectives()) {
							fileName += "_"+obj;
						}
						fileName += ".xmi";
						file = new File(file, fileName);
						if(!file.exists()) {
							Files.write(new File(outputFolder, "errors.log").toPath(), ("Exp2: Result model file \""+file+"\" not found.").getBytes(), StandardOpenOption.CREATE,StandardOpenOption.APPEND);
							continue;
						}
						
						Resource res = new ResourceSetImpl().createResource(URI.createFileURI(file.getAbsolutePath() ));
						res.load(Collections.EMPTY_MAP);

						TypeGraph solPG = null;
						EObject eObject = res.getContents().get(0);
						if (eObject instanceof TypeGraph) {
							solPG = (TypeGraph) eObject;
						} else if (eObject instanceof HAntiPatternDetection) {
							solPG = ((HAntiPatternDetection) eObject).getApg().getPg();

						}

						VisibilityReducer.reduce(pg);
						double vis = visibilityCalculator.calculate(solPG);

						s.append(fileName);
						for(double obj : sol.getObjectives()) {
							s.append(";"+obj);
						}
						s.append(";"+vis+"\n");
					}
				}

			}
		}
	}
	
	/**
	 * Experiment 3 - reduce visibility in each step
	 * @throws IOException
	 */
	@Test
	public void exp3() throws IOException {
		fail("Not implemented yet");
		Resource r = new ResourceSetImpl().createResource(URI.createFileURI("input/"+name));
		r.load(Collections.EMPTY_MAP);

		TypeGraph pg = (TypeGraph) r.getContents().get(0);

		VisibilityCalculator visibilityCalculator = new VisibilityCalculator();
		double initial = visibilityCalculator.calculate(pg);

		File outputFolder = new File(new File(new File(new File("output"), pg.getTName()),time),"exp3");
		outputFolder.mkdirs();

	}

	/**
	 * Experiment 4 - Visibility changes only due to refactorings
	 * @throws IOException
	 */
	@Test
	public void exp4() throws IOException {
		Resource r = new ResourceSetImpl().createResource(URI.createFileURI("input/"+name));
		r.load(Collections.EMPTY_MAP);

		TypeGraph pg = (TypeGraph) r.getContents().get(0);

		VisibilityCalculator visibilityCalculator = new VisibilityCalculator();
		double initial = visibilityCalculator.calculate(pg);

		File outputFolder = new File(new File(new File(new File("output"), pg.getTName()),time),"exp4");
		outputFolder.mkdirs();

		File model = new File(outputFolder, pg.getTName() + ".xmi");
		r.save(new FileOutputStream(model), Collections.EMPTY_MAP);

		SearchTypeGraph search = new SearchTypeGraph();
		search.initializeFitnessFunctions();
		search.initializeConstraints();
		TransformationResultManager results = search.performSearch(model.getAbsolutePath(), 10, outputFolder);

		try (FileWriter s = new FileWriter(new File(outputFolder, time+"_exp4.csv"), true)) {
			s.append("version;refactorings;coupling;cohesion;blobs;visibility\n");
			s.append("initial;0;;;;"+initial+'\n');
			
			for (List<NondominatedPopulation> val : results.getResults().values()) {
				for (NondominatedPopulation pop : val) {
					for (Solution sol : pop) {
						String fileName = name;
						for(double obj : sol.getObjectives()) {
							fileName += "_"+obj;
						}
						fileName += ".xmi";
						
						s.append(fileName);
						for(double obj : sol.getObjectives()) {
							s.append(";"+obj);
						}
					}
				}

			}
		}
	}
}
