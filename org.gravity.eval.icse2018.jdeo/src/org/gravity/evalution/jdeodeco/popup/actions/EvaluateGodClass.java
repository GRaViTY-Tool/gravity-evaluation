package org.gravity.evalution.jdeodeco.popup.actions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jdt.core.IJavaProject;

import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.distance.ExtractClassCandidateGroup;
import gr.uom.java.jdeodorant.refactoring.views.GodClass;


public class EvaluateGodClass {

	public static ExtractClassCandidateGroup[] detect(IJavaProject sProject)
			throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		GodClass viewGodClass = new GodClass();
		
		
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
		Object invoke = getTable.invoke(viewGodClass);
		if(invoke == null){
			return null;
		}
		candidateRefactoringTable = (ExtractClassCandidateGroup[]) invoke;
		return candidateRefactoringTable;
	}
}