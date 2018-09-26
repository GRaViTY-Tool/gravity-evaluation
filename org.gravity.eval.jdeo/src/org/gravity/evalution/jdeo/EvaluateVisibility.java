package org.gravity.evalution.jdeo;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.gravity.eval.fase2018.Experiment;
import org.gravity.eval.fase2018.Experiment.METRICS;
import org.gravity.evalution.jdeo.popup.actions.EvaluateGodClass;
import org.gravity.goblin.repair.VisibilityReducer;
import org.gravity.tgg.modisco.MoDiscoTGGConverter;
import org.gravity.typegraph.basic.TypeGraph;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.ExtractClassCandidateGroup;
import gr.uom.java.distance.ExtractClassCandidateRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractClassRefactoring;

@RunWith(Parameterized.class)
public class EvaluateVisibility {

	public static final int limit = 100;

	private IJavaProject project;

	private String time;

	public EvaluateVisibility(String name, IJavaProject project, String time) {
		this.project = project;
		this.time = time;
	}

	@Parameters(name = "JDeo check visibility: {0}")
	public static Collection<Object[]> collectProjects() {
		String time = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss").format(new Date(System.currentTimeMillis()));
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<Object[]> results = new ArrayList<>(projects.length);
		for (IProject p : projects) {
			results.add(new Object[] { p.getName(), JavaCore.create(p), time });
		}
		return results;
	}

	@Test
	public void checkVisibility()
			throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, CoreException {
		File folder = new File(new File("output"), time);
		folder.mkdirs();
		File out = new File(folder, project.getProject().getName() + ".csv");

		NullProgressMonitor pm = new NullProgressMonitor();
		
		project.getProject().refreshLocal(IResource.DEPTH_INFINITE, pm);
		try (PrintWriter printer = new PrintWriter(out)) {
			printer.println("id;refactorings;blobs;lcom;cbo;visibility;visibilityReduced");
			MoDiscoTGGConverter converter = new MoDiscoTGGConverter();
			if (converter.convertProject(project, pm)) {
				TypeGraph pg = converter.getPG();

				List<Double> metrics = Experiment.calcMetrics(pg);
				Double visibility = metrics.get(METRICS.VISIBILITY.getId());
				Double lcom = metrics.get(METRICS.LCOM.getId());
				Double cbo = metrics.get(METRICS.CBO.getId());
				Double blobs = metrics.get(METRICS.BLOBS.getId());
				
				VisibilityReducer.reduce(pg);
				
				Double reduced = Experiment.calcMetrics(pg).get(METRICS.VISIBILITY.getId());
				
				printer.println("initial;0;"+blobs+";"+lcom+";"+cbo+";"+visibility+";"+reduced);
				
			}
			
			int i = 0;

			ExtractClassCandidateGroup[] results = EvaluateGodClass.detect(project);
			for (ExtractClassCandidateGroup extract : results) {
				for (ExtractClassCandidateRefactoring cand : extract.getCandidates()) {
					if (cand.isApplicable()) {
						try {

							Change undo = refactor(cand, pm);

							if (undo != null) {
								try {
									converter = new MoDiscoTGGConverter();
									if (converter.convertProject(project, pm)) {
										TypeGraph pg = converter.getPG();

										List<Double> metrics = Experiment.calcMetrics(pg);
										Double visibility = metrics.get(METRICS.VISIBILITY.getId());
										Double lcom = metrics.get(METRICS.LCOM.getId());
										Double cbo = metrics.get(METRICS.CBO.getId());
										Double blobs = metrics.get(METRICS.BLOBS.getId());
										int refactorings = 1 + cand.getExtractedEntities().size();
										
										VisibilityReducer.reduce(pg);
										
										Double reduced = Experiment.calcMetrics(pg).get(METRICS.VISIBILITY.getId());
										
										printer.println(i+ ";"+refactorings+";"+blobs+";"+lcom+";"+cbo+";"+visibility+";"+reduced);
										
									}
									
								} catch (Exception e) {
									e.printStackTrace();
								}
								undo.perform(pm);

								if (i++ >= limit) {
									return;
								}
							} else {
								throw new RuntimeException("No undo change");
							}

						} catch (OperationCanceledException | CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	private Change refactor(CandidateRefactoring entry, IProgressMonitor pm)
			throws OperationCanceledException, CoreException {
		if (entry.getSourceClassTypeDeclaration() != null) {
			IFile sourceFile = entry.getSourceIFile();
			CompilationUnit sourceCompilationUnit = (CompilationUnit) entry.getSourceClassTypeDeclaration().getRoot();
			Refactoring refactoring = null;
			if (entry instanceof ExtractClassCandidateRefactoring) {
				ExtractClassCandidateRefactoring candidate = (ExtractClassCandidateRefactoring) entry;
				String[] tokens = candidate.getTargetClassName().split("\\.");
				String extractedClassName = tokens[tokens.length - 1];
				Set<VariableDeclaration> extractedFieldFragments = candidate.getExtractedFieldFragments();
				Set<MethodDeclaration> extractedMethods = candidate.getExtractedMethods();

				refactoring = new ExtractClassRefactoring(sourceFile, sourceCompilationUnit,
						candidate.getSourceClassTypeDeclaration(), extractedFieldFragments, extractedMethods,
						candidate.getDelegateMethods(), extractedClassName);
				refactoring.checkAllConditions(pm);
				Change change = refactoring.createChange(pm);
				return change.perform(pm);
			}
		}
		return null;
	}
}
