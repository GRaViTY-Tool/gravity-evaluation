package org.gravity.eval.fase2017;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.converter.IPGConverter;
import org.gravity.eclipse.exceptions.NoConverterRegisteredException;
import org.gravity.eval.fase2017.util.EclipseProjectUtil;
import org.gravity.eval.fase2017.util.ToFileLogger;
import org.gravity.hulk.HAntiPatternHandling;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.antipatterngraph.AntipatterngraphFactory;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HAntiPatternGraph;
import org.gravity.hulk.detection.HulkDetector;
import org.gravity.hulk.refactoringgraph.HBlobResolveAnnotation;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMember;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMembers;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMethod;
import org.gravity.hulk.refactoringgraph.refactorings.HRefactoring;
import org.gravity.hulk.resolve.ResolveFactory;
import org.gravity.hulk.resolve.antipattern.AntipatternPackage;
import org.gravity.hulk.resolve.antipattern.HAlternativeBlobresolver;
import org.gravity.refactorings.ui.EclipseMoveMethodRefactoring;
import org.gravity.typegraph.basic.TClass;
import org.gravity.typegraph.basic.TMethodDefinition;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.moflon.core.dfs.DFSGraph;

@RunWith(Parameterized.class)
public class RQ1_2 {

	@Rule
	public ToFileLogger logToFile = new ToFileLogger(Paths.get(new File("logs").toURI())); //$NON-NLS-1$

	private final IJavaProject project;

	private IJavaProject java_project_copy;

	public RQ1_2(final IJavaProject project) {
		this.project = project;
	}

	@Parameters(name = "{index}: FASE2017 Eval RQ1+Q2: {0}")
	public static Collection<Object[]> data() {
		final List<Object[]> testcases = new ArrayList<>();

		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

		for (final IProject test : projects) {
			try {
				if (test.getNature(JavaCore.NATURE_ID) != null) {
					testcases.add(new Object[] { JavaCore.create(test) });
				}
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}

		return testcases;
	}

	@Test
	public void resolveBlob() {
		final HashSet<EClass> set = new HashSet<>();
		set.add(AntipatternPackage.eINSTANCE.getHBlobResolver());
		assertTrue(perform(set, "blob"));
	}

	private boolean perform(final Set<EClass> selection, final String ap) {

		final NullProgressMonitor monitor = new NullProgressMonitor();

		this.java_project_copy = EclipseProjectUtil.copyJavaProject(this.project,
				this.project.getProject().getName() + "_tmp_" + ap);
		if (this.java_project_copy == null) {
			return false;
		}

		final HAntiPatternHandling hulk = ResolveFactory.eINSTANCE.createHAntiPatternResolving();
		final Set<HDetector> selected_detectors = new HashSet<>();
		final Set<HDetector> executed_detectors = new HashSet<>();

		final long t0 = System.currentTimeMillis();
		System.out.println(t0 + " Hulk Anti-Pattern Detection");
		System.out.println(t0 + " Init Model");

		final IPath project_location = this.java_project_copy.getProject().getLocation();
		IPGConverter converter;
		try {
			converter = GravityActivator.getDefault().getNewConverter(this.java_project_copy.getProject());
		} catch (NoConverterRegisteredException | CoreException e) {
			return false;
		}

		final boolean success = converter.convertProject(monitor);
		if (!success || (converter.getPG() == null)) {
			fail("Creating PG from project failed: " + this.java_project_copy.getProject().getName());
			return false;
		}

		final long t1 = System.currentTimeMillis();
		System.out.println(t1 + " Init Hulk");
		final HAntiPatternGraph apg = AntipatterngraphFactory.eINSTANCE.createHAntiPatternGraph();
		apg.setPg(converter.getPG());

		hulk.setApg(apg);
		hulk.setProgramlocation(project_location.toString());

		//		final ResourceSet rs = converter.getResourceSet();
		final ResourceSet rs = GravityActivator.getDefault().getResourceSet(this.project.getProject());
		rs.createResource(URI.createURI("Hulk.xmi")).getContents().add(hulk); //$NON-NLS-1$

		final DFSGraph dependencies = hulk.getDependencyGraph();

		final Resource res = rs.createResource(URI.createURI("SemllDependencyGraph.xmi")); //$NON-NLS-1$
		res.getContents().add(dependencies);
		final long t2 = System.currentTimeMillis();
		System.out.println(t2 + " Init Hulk - done " + (t2 - t1) + "ms");
		System.out.println(t2 + " Init Model - done " + (t2 - t0) + "ms");

		// long t3 = System.currentTimeMillis();
		// System.out.println(t3 + " Sync Bwd");
		// converter.syncProjectBwd(IPGConverter -> {

		System.out.println(System.currentTimeMillis() + " Hulk Detect AP");
		assertTrue(new HulkDetector(hulk, HulkDetector.getDefaultThresholds()).detectSelectedAntiPattern(selection,
				selected_detectors, executed_detectors));
		System.out.println(System.currentTimeMillis() + " Hulk Detect AP - done");

		// }, monitor);

		// IFolder folder =
		// java_project.getProject().getFolder("src/org/gravity/hulk/annotations");
		// //$NON-NLS-1$
		// if (folder.exists()) {
		// try {
		// folder.delete(true, monitor);
		// } catch (CoreException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }

		final long t4 = System.currentTimeMillis();
		System.out.println(t4 + " Hulk Anti-Pattern Detection - Done " + (t4 - t0) + "ms");

		return true;

	}

	@After
	public void after() {
		try {
			if ((this.java_project_copy != null) && this.java_project_copy.exists()) {
				this.java_project_copy.getProject().delete(true, new NullProgressMonitor());
			}
		} catch (final CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void refactor(final NullProgressMonitor monitor, final HAlternativeBlobresolver resolver, final IJavaProject project) {
		final LinkedList<HRefactoring> moves = new LinkedList<>();
		for (final HAnnotation hAnnotation : resolver.getHAnnotation()) {
			if (hAnnotation instanceof HBlobResolveAnnotation) {
				moves.addAll(((HBlobResolveAnnotation) hAnnotation).getHRefactorings());
			}
		}

		try {
			final EclipseMoveMethodRefactoring refactor = new EclipseMoveMethodRefactoring(this.java_project_copy);
			for (final HRefactoring move : moves) {
				if (move instanceof HMoveMembers) {
					final HMoveMembers moveMembers = (HMoveMembers) move;
					for (final HMoveMember moveMember : moveMembers.getHMoveMembers()) {
						if (moveMember instanceof HMoveMethod) {
							final TClass tTargetClass = moveMembers.getTargetClass();
							final TClass tSourceClass = moveMembers.getSourceClass();

							final TMethodDefinition tMethod = (TMethodDefinition) ((HMoveMethod) moveMember).getTAnnotated();

							refactor.moveMethod(tSourceClass, tTargetClass, tMethod.getSignature(), monitor);
						}
					}
				}
			}

		} catch (final CoreException e) {
			e.printStackTrace();
		}

	}

}
