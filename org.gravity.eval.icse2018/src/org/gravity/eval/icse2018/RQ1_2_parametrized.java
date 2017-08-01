package org.gravity.eval.icse2018;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveMethodDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.converter.IPGConverter;
import org.gravity.eval.icse2018.util.EclipseProjectUtil;
import org.gravity.eval.icse2018.util.sourcemeter.MetricCalculator;
import org.gravity.eval.icse2018.util.sourcemeter.SourcemeterMetricKeys;
import org.gravity.hulk.HAntiPatternDetection;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.HulkFactory;
import org.gravity.hulk.antipatterngraph.AntipatterngraphFactory;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HAntiPatternGraph;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.detection.HRelativeDetector;
import org.gravity.hulk.detection.HulkDetector;
import org.gravity.hulk.detection.antipattern.HBlobDetector;
import org.gravity.hulk.refactoringgraph.HBlobResolveAnnotation;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMember;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMembers;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMethod;
import org.gravity.hulk.refactoringgraph.refactorings.HRefactoring;
import org.gravity.hulk.resolve.antipattern.HAlternativeBlobresolver;
import org.gravity.typegraph.basic.TAbstractType;
import org.gravity.typegraph.basic.TClass;
import org.gravity.typegraph.basic.TMethodDefinition;
import org.gravity.typegraph.basic.TMethodSignature;
import org.gravity.typegraph.basic.TParameter;
import org.gravity.typegraph.basic.TParameterList;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moflon.core.dfs.DFSGraph;

import com.beust.jcommander.ParameterException;

import at.ac.tuwien.big.momot.TransformationResultManager;
import at.ac.tuwien.big.momot.problem.solution.variable.RuleApplicationVariable;
import at.ac.tuwien.big.momot.problem.solution.variable.TransformationPlaceholderVariable;
import at.ac.tuwien.big.momot.problem.solution.variable.UnitApplicationVariable;
import de.uni_hamburg.informatik.swt.accessanalysis.Analysis;
import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisException;
import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisFactory;
import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisFactory.AnalysisMode;
import de.uni_hamburg.informatik.swt.accessanalysis.results.ResultFormatter;
import momotFiles.MOMotExecutor;
import momotFiles.SearchParameters;
import momotFiles.SearchTypeGraph;

@RunWith(Parameterized.class)
public class RQ1_2_parametrized {

	private static final String AP_SUFFIX = "blob";

	private static final boolean ENABLE_METRIC_RECORDING = true;

	// @Rule
	// public ToFileLogger logToFile = new ToFileLogger(Paths.get(new
	// File("logs").toURI())); //$NON-NLS-1$

	private IJavaProject project;

	private IJavaProject java_project_copy;

	public RQ1_2_parametrized(IJavaProject project) {
		this.project = project;
	}

	@Parameters(name = "{index}: ICSE2018 Eval RQ1+Q2: {0}")
	public static Collection<Object[]> data() {
		List<Object[]> testcases = new ArrayList<>();

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

		for (IProject test : projects) {
			if (!test.exists() || !test.isOpen()) {
				continue;
			}
			try {
				if (test.getNature(JavaCore.NATURE_ID) != null) {
					if (test.getName().endsWith("_tmp_" + AP_SUFFIX)) {
						test.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT,
								new NullProgressMonitor());
					} else {
						testcases.add(new Object[] { JavaCore.create(test) });
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return testcases;
	}

	@Test
	public void resolveBlob() throws FileNotFoundException, IOException {
		new Eval().run(project, AP_SUFFIX, ENABLE_METRIC_RECORDING);
	}

	@After
	public void after() {
		try {
			if (java_project_copy != null && java_project_copy.exists()) {
				java_project_copy.getProject().delete(false, true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
