package org.gravity.eval.runtime.tgg.modisco;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.modisco.java.AbstractMethodDeclaration;
import org.eclipse.modisco.java.AbstractTypeDeclaration;
import org.eclipse.modisco.java.BodyDeclaration;
import org.eclipse.modisco.java.CompilationUnit;
import org.eclipse.modisco.java.MethodDeclaration;
import org.eclipse.modisco.java.Package;
import org.eclipse.modisco.java.emf.JavaFactory;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.tests.TestHelper;
import org.gravity.modisco.MGravityModel;
import org.gravity.modisco.ModiscoFactory;
import org.gravity.modisco.util.MoDiscoUtil;
import org.gravity.tgg.modisco.pm.MoDiscoTGGConverter;
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
public class SyncMeasurements {

	/*
	 * The order of this constants is important for the initialization!
	 */
	private static final Path OUTPUT = Paths.get("results.csv");
	private static final String DATE = new SimpleDateFormat().format(new Date());
	private static final Logger LOGGER = Logger.getLogger(SyncMeasurements.class);

	private static final List<String> SKIP = Arrays.asList(
			"00_JavaSolitaire1.3",
			"01_QuickUML2001", "02_JSciCalc2.1.0", "03_JUnit3.8.2",
			//			"04_JSSE_OpenJDK8",
			"05_Gantt1.10.2", "06_Nutch0.9",
			"07_Lucene1.4.3", "08_log4j1.2.17", "09_JHotDraw7.6", "10_JEdit4.0", "11_PMD3.9", "12_JTransform3.1",
			//						"13_iTrust21.0",
			"14_JabRef2.7", "15_Xerces2.7.0", "16_ArgoUML0.19.8", "17_jfreechart1.0.19",
			"18_Tomcat6.0.45", "19_Azureus2.3.0.6",
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
	public SyncMeasurements(final String name, final IJavaProject project) {
		this.name = name;
		this.project = project;
		Logger.getRootLogger().setLevel(Level.ERROR);
	}

	@Test
	public void measurePM() throws Exception {
		final var monitor = new NullProgressMonitor();
		final var converter = new MoDiscoTGGConverter(this.project, false);
		converter.disableAutosave();

		GravityActivator.setRecordKey("pm");
		GravityActivator.recordMessage("Measure " + this.name);
		System.gc();

		final var start = System.nanoTime();
		final var success = converter.convertProject(monitor);
		final var stop = System.nanoTime();
		System.out.println("Initial: " + ((stop - start) / 1000 / 1000) + "ms");

		final List<Change> changes = Arrays.asList(new DeleteMethod(), new CreateClass(), new RenameClass(),
				new CreateMethod(), new DeleteMethod(), new CreateClass(), new RenameClass(), new CreateMethod(),
				new DeleteMethod(), new CreateClass(), new RenameClass(), new CreateMethod());
		Collections.shuffle(changes);
		for (final Change c : changes) {
			final var time = c.execute(converter, monitor);
			GravityActivator.recordMessage("Sync " + c.getName() + ": " + (time / 1000 / 1000) + "ms");
		}

		final var set = converter.getResourceSet();
		converter.discard();
		set.getResources().forEach(Resource::unload);
		set.getResources().clear();
		assertTrue(success);
		// addResults(start / 1000 / 1000, stop / 1000 / 1000);
		System.gc();
	}

	private interface Change {
		long execute(final MoDiscoTGGConverter converter, final NullProgressMonitor monitor);

		String getName();
	}

	private class CreateClass implements Change {

		@Override
		public long execute(final MoDiscoTGGConverter converter, final NullProgressMonitor monitor) {
			final List<String> added = new LinkedList<>();
			final var start = System.nanoTime();
			final List<Long> change = new ArrayList<>(1);
			converter.syncProjectFwd(c -> {
				final var startDelete = System.nanoTime();

				var p = getRandomPackage((MGravityModel) c);
				for (var i = 0; i < RANDOM.nextInt(3); i++) {
					final var next = JavaFactory.eINSTANCE.createPackage();
					next.setName("p" + i);
					p.getOwnedPackages().add(next);
					p = next;
				}
				final var clazz = ModiscoFactory.eINSTANCE.createMClass();
				clazz.setName("NewClass" + System.currentTimeMillis());
				clazz.setPackage(p);
				clazz.setProxy(false);
				final var cu = JavaFactory.eINSTANCE.createCompilationUnit();
				cu.setName(clazz.getName() + ".java");
				cu.setOriginalFilePath(MoDiscoUtil.getNameSpace(p).replace(".", "/"));
				clazz.setOriginalCompilationUnit(cu);
				((MGravityModel) c).getCompilationUnits().add(cu);
				added.add(MoDiscoUtil.getQualifiedName(clazz));
				change.add(System.nanoTime() - startDelete);
			}, monitor);
			final var time = System.nanoTime() - start - change.get(0);

			// Check validity of change
			for (final String name : added) {
				assertNotNull(converter.getTrg().getType(name));
			}
			return time;
		}

		@Override
		public String getName() {
			return "create class";
		}
	}

	private class CreateMethod implements Change {

		@Override
		public long execute(final MoDiscoTGGConverter converter, final NullProgressMonitor monitor) {
			final List<String> deleted = new LinkedList<>();
			final var start = System.nanoTime();
			final List<Long> change = new ArrayList<>(1);
			converter.syncProjectFwd(c -> {
				final var startDelete = System.nanoTime();
				final var model = (MGravityModel) c;

				final var returnType = MoDiscoUtil.getType(model, "java.lang.String");
				final var returnAccess = JavaFactory.eINSTANCE.createTypeAccess();
				returnAccess.setType(returnType);

				final var name = ModiscoFactory.eINSTANCE.createMMethodName();
				name.setMName("callToString" + System.currentTimeMillis());
				model.getMMethodNames().add(name);

				final var signature = ModiscoFactory.eINSTANCE.createMMethodSignature();
				name.getMSignatures().add(signature);
				signature.setReturnType(returnType);

				final var block = JavaFactory.eINSTANCE.createBlock();
				final var ret = JavaFactory.eINSTANCE.createReturnStatement();
				block.getStatements().add(ret);
				final var invocation = ModiscoFactory.eINSTANCE.createMSuperMethodInvocation();
				ret.setExpression(invocation);
				invocation.setMethod(
						(AbstractMethodDeclaration) MoDiscoUtil.getType(model, "java.lang.Object").getBodyDeclarations()
						.parallelStream().filter(d -> "toString".equals(d.getName())).findAny().orElse(null));

				final var definition = ModiscoFactory.eINSTANCE.createMMethodDefinition();
				definition.setName(name.getMName());
				definition.setMSignature(signature);
				definition.setReturnType(returnAccess);
				definition.setBody(block);
				definition.getMMethodInvocations().add(invocation);
				model.getMAbstractMethodDefinitions().add(definition);

				final var type = getRandomType((MGravityModel) c);
				type.getBodyDeclarations().add(definition);

				deleted.add(MoDiscoUtil.getQualifiedName(type));
				deleted.add(MoDiscoUtil.getSignature(definition));

				change.add(System.nanoTime() - startDelete);
			}, monitor);
			final var time = System.nanoTime() - start - change.get(0);

			// Check validity of change
			final var type = converter.getTrg().getType(deleted.get(0));
			assertNotNull(type);
			assertNotNull(type.getTMethodDefinition(deleted.get(1)));
			return time;
		}

		@Override
		public String getName() {
			return "create method";
		}
	}

	private class DeleteClass implements Change {

		@Override
		public long execute(final MoDiscoTGGConverter converter, final NullProgressMonitor monitor) {
			final List<String> deleted = new LinkedList<>();
			final var start = System.nanoTime();
			final List<Long> change = new ArrayList<>(1);
			converter.syncProjectFwd(c -> {
				final var startDelete = System.nanoTime();
				final var cu = getRandomCompilationUnit((MGravityModel) c);
				deleted.addAll(cu.getTypes().stream().map(MoDiscoUtil::getQualifiedName).collect(Collectors.toList()));
				final List<EObject> delete = new LinkedList<>();
				delete.add(cu);
				delete.addAll(cu.getTypes());
				EcoreUtil.deleteAll(delete, true);
				change.add(System.nanoTime() - startDelete);
			}, monitor);
			final var time = System.nanoTime() - start - change.get(0);

			// Check validity of change
			for (final String name : deleted) {
				assertNull(converter.getTrg().getType(name));
			}
			return time;
		}

		@Override
		public String getName() {
			return "delete class";
		}
	}

	private class DeleteMethod implements Change {

		@Override
		public long execute(final MoDiscoTGGConverter converter, final NullProgressMonitor monitor) {
			final List<String> deleted = new LinkedList<>();
			final var start = System.nanoTime();
			final List<Long> change = new ArrayList<>(1);
			converter.syncProjectFwd(c -> {
				final var startDelete = System.nanoTime();
				final var m = getRandomMethodDefinition((MGravityModel) c);
				deleted.add(MoDiscoUtil.getQualifiedName(m.getAbstractTypeDeclaration()));
				deleted.add(MoDiscoUtil.getSignature(m));
				EcoreUtil.deleteAll(Collections.singleton(m), true);
				change.add(System.nanoTime() - startDelete);
			}, monitor);
			final var time = System.nanoTime() - start - change.get(0);

			// Check validity of change
			final var type = converter.getTrg().getType(deleted.get(0));
			assertNotNull(type);
			assertNull(type.getTMethodDefinition(deleted.get(1)));
			return time;
		}

		@Override
		public String getName() {
			return "delete method";
		}
	}

	private class RenameClass implements Change {

		@Override
		public long execute(final MoDiscoTGGConverter converter, final NullProgressMonitor monitor) {
			final List<String> deleted = new LinkedList<>();
			final var start = System.nanoTime();
			final List<Long> change = new ArrayList<>(1);
			converter.syncProjectFwd(c -> {
				final var startDelete = System.nanoTime();
				final var type = getRandomType((MGravityModel) c);
				deleted.add(MoDiscoUtil.getQualifiedName(type));
				type.setName("RenamedType"+System.currentTimeMillis());
				deleted.add(MoDiscoUtil.getQualifiedName(type));
				change.add(System.nanoTime() - startDelete);
			}, monitor);
			final var time = System.nanoTime() - start - change.get(0);

			// Check validity of change
			assertNull(converter.getTrg().getType(deleted.get(0)));
			assertNotNull(converter.getTrg().getType(deleted.get(1)));
			return time;
		}

		@Override
		public String getName() {
			return "rename class";
		}
	}

	private static final Random RANDOM = new Random();

	/**
	 * @param modisco
	 * @return
	 */
	public CompilationUnit getRandomCompilationUnit(final MGravityModel modisco) {
		return modisco.getCompilationUnits().get(RANDOM.nextInt(modisco.getCompilationUnits().size()));
	}

	private MethodDeclaration getRandomMethodDefinition(final MGravityModel c) {
		final List<BodyDeclaration> methods = getTypeStream(c).flatMap(t -> t.getBodyDeclarations().parallelStream())
				.filter(MethodDeclaration.class::isInstance).collect(Collectors.toList());
		return (MethodDeclaration) methods.get(RANDOM.nextInt(methods.size()));
	}

	public Stream<AbstractTypeDeclaration> getTypeStream(final MGravityModel model) {
		return model.getCompilationUnits().parallelStream().flatMap(cu -> cu.getTypes().parallelStream());
	}

	private AbstractTypeDeclaration getRandomType(final MGravityModel c) {
		final List<AbstractTypeDeclaration> types = getTypeStream(c).collect(Collectors.toList());
		return types.get(RANDOM.nextInt(types.size()));
	}

	private Package getRandomPackage(final MGravityModel model) {
		final List<Package> packages = new LinkedList<>();
		final Deque<Package> stack = new LinkedList<>(model.getOwnedElements());
		while (!stack.isEmpty()) {
			final var p = stack.pop();
			packages.add(p);
			stack.addAll(p.getOwnedPackages());
		}
		return packages.get(RANDOM.nextInt(packages.size()));
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
					if (o1 == o2) {
						return 0;
					}
					return o1.getName().compareTo(o2.getName()) * -1;
				}).collect(Collectors.toList());
		LOGGER.info("Imported " + projects.size() + "projects into workspace.");
		return TestHelper.prepareTestData(projects);
	}

}
