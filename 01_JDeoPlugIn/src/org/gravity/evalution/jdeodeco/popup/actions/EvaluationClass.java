package org.gravity.evalution.jdeodeco.popup.actions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.distance.ExtractClassCandidateGroup;
import gr.uom.java.jdeodorant.refactoring.views.GodClass;

public class EvaluationClass implements IObjectActionDelegate {

	private Shell shell;
	
	/**
	 * Constructor for Action1.
	 */
	public EvaluationClass() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}
	
	
	/**
	 * Gets selected project from workspace
	 * @return
	 */
	private IJavaProject getSelectedProject() {
			IJavaProject javaProject = null;
			
			ISelection selection =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();

			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object element = structuredSelection.getFirstElement();
				
				if(element instanceof IJavaProject) {
					javaProject = (IJavaProject)element;
				}
			}
	return javaProject;
	}


	/**
	 * 
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
	 
		GodClass viewGodClass = new GodClass();
		
		try {
			
			// Get the currently selected project
			IJavaProject sProject = getSelectedProject();
			String projName = sProject.toString();
			
			// Set up time
			System.out.println("Start time measurement for project " +	projName + ".");
			long timePrev = System.currentTimeMillis();
			
			// Set selectedProject and activeProject in the GodClass by Reflection
			Field selProject = viewGodClass.getClass().getDeclaredField("selectedProject");
			selProject.setAccessible(true);
			selProject.set(viewGodClass, sProject);
			
			Field actProject = viewGodClass.getClass().getDeclaredField("activeProject");
			actProject.setAccessible(true);
			actProject.set(viewGodClass, sProject);
			
			
			
			
			// Clear Cache
			CompilationUnitCache.getInstance().clearCache();
			
			
			// Probably not needed
			ExtractClassCandidateGroup[] candidateRefactoringTable;
			
			
			// Execute the search for code smells and refactoring options by getTable() via reflection
			Method getTable = viewGodClass.getClass().getDeclaredMethod("getTable");
			getTable.setAccessible(true);
			candidateRefactoringTable = (ExtractClassCandidateGroup[]) getTable.invoke(viewGodClass);
			
			
			// End time measurement
			long timeEnd = System.currentTimeMillis();
			System.out.println("Time taken for JDeodorant check for project " +	projName + " : " + (timeEnd - timePrev) + "ms");
			System.out.println("");
			
			
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		

		
		
		
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
