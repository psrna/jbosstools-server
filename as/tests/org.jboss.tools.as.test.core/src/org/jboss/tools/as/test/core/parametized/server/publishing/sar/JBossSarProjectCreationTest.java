package org.jboss.tools.as.test.core.parametized.server.publishing.sar;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.application.internal.operations.AddReferenceToEnterpriseApplicationDataModelProvider;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.properties.IAddReferenceDataModelProperties;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualReference;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelProvider;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.ide.eclipse.as.core.server.internal.DeployableServer;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.tools.as.test.core.ASMatrixTests;
import org.jboss.tools.as.test.core.internal.utils.ProjectRuntimeUtil;
import org.jboss.tools.as.test.core.internal.utils.ServerCreationTestUtils;
import org.jboss.tools.as.test.core.internal.utils.wtp.CreateProjectOperationsUtility;
import org.jboss.tools.as.test.core.internal.utils.wtp.JavaEEFacetConstants;
import org.jboss.tools.as.test.core.internal.utils.wtp.OperationTestCase;
import org.jboss.tools.test.util.JobUtils;

public class JBossSarProjectCreationTest extends TestCase {
	public void tearDown() throws Exception {
		ASMatrixTests.cleanup();
	}
	
	public void testProjectCreation1() {
		try {
			IDataModel sar1Model = CreateProjectOperationsUtility.getSarDataModel("sar1a", null, null, null, JavaEEFacetConstants.SAR_1);
	    	OperationTestCase.runAndVerify(sar1Model);
	    	JobUtils.waitForIdle(1000);
	    	
	    	IProject sar = ResourcesPlugin.getWorkspace().getRoot().getProject("sar1a");
	    	assertTrue(sar.getFolder("sarcontent").exists());
	    	assertTrue(sar.getFolder("sarcontent").getFolder("META-INF").exists());
	    	assertTrue(sar.getFolder("src").exists());
	    	assertTrue(sar.getFolder("build").exists());
	    	assertTrue(sar.getFolder("build").getFolder("classes").exists());
	    	assertNull(ProjectRuntimeUtil.getRuntime(sar));
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testProjectCreationNonDefaultSrcContentOutput() {
		try {
			IDataModel sar1Model = CreateProjectOperationsUtility.getSarDataModel("sar1b", "src2", "sarcontent2", "bin", JavaEEFacetConstants.SAR_1);
	    	OperationTestCase.runAndVerify(sar1Model);
	    	JobUtils.waitForIdle(1000);
	    	
	    	IProject sar = ResourcesPlugin.getWorkspace().getRoot().getProject("sar1b");
	    	System.out.println(sar.getLocation().toOSString());
	    	assertTrue(sar.getFolder("sarcontent2").exists());
	    	assertTrue(sar.getFolder("sarcontent2").getFolder("META-INF").exists());
	    	assertTrue(sar.getFolder("src2").exists());
	    	assertTrue(sar.getFolder("bin").exists());
	    	assertNull(ProjectRuntimeUtil.getRuntime(sar));
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSarInsideEarAsChildModule()  {
		createSarInEar("sar1dear", "sar1g", "server1", new Path("lib").makeAbsolute());
	}
	
	public void testSarInsideEarPublish()  {
		createSarInEar("sar1fear", "sar1h", "server2", new Path("lib").makeAbsolute());
    	IProject earProj = ResourcesPlugin.getWorkspace().getRoot().getProject("sar1fear");
    	IProject sarProj = ResourcesPlugin.getWorkspace().getRoot().getProject("sar1h");
    	
    	// add a file to sar
    	IFile testClass = sarProj.getFolder("src").getFile("MyTestClass.java");
    	String contents = "public class MyTestClass { }";
    	try {
    		testClass.create(new ByteArrayInputStream(contents.getBytes()), false, null);
    	} catch(CoreException ce) {
    		fail("Unable to create test class at path " + testClass.getFullPath().toOSString());
    	}
    	
		IModule earModule = ServerUtil.getModule(earProj);
		IServer s = ServerCore.findServer("server2");
		try {
			IServerWorkingCopy wc = s.createWorkingCopy();
			wc.modifyModules(new IModule[]{earModule}, new IModule[0], new NullProgressMonitor());
			s = wc.save(false, null);
		} catch(CoreException ce) {
			fail("Unable to add ear module with nested sar to server");
		}
		s.publish(IServer.PUBLISH_FULL, null);
		DeployableServer ds = (DeployableServer)s.loadAdapter(DeployableServer.class, null);
		IPath deepDeployFolder = ds.getDeploymentLocation(new IModule[]{earModule}, true);
		assertTrue(deepDeployFolder.toFile().exists());
		assertTrue(deepDeployFolder.toFile().isDirectory());
		assertTrue(deepDeployFolder.append("lib").toFile().exists());
		assertTrue(deepDeployFolder.append("lib").toFile().isDirectory());
		IPath sar1h = deepDeployFolder.append("lib").append("sar1h.sar"); 
		assertTrue(sar1h.toFile().exists());
		assertTrue(sar1h.toFile().isDirectory());
		assertTrue(sar1h.append("META-INF").toFile().exists());
		assertTrue(sar1h.append("META-INF").toFile().isDirectory());
		assertTrue(sar1h.append("MyTestClass.class").toFile().exists());
		assertTrue(sar1h.append("MyTestClass.class").toFile().isFile());
		assertFalse(sar1h.append("MyTestClass.java").toFile().exists());
		
	}
	
	protected void createSarInEar(String earName, String sarName, String serverName, IPath rootRelativePath) {

		try {
	    	createSarAndEarProjects(earName, sarName);
	    	addSarReferenceToEar(earName, sarName, rootRelativePath);
	    	// create the server
			IServer s = ServerCreationTestUtils.createMockServerWithRuntime(IJBossToolingConstants.SERVER_AS_60, serverName);
			assertNotNull(s);
			assertEquals(serverName, s.getName());

			verifyParentChildRelationship(s, earName, sarName);
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
	
	protected void createSarAndEarProjects(String earName, String sarName) throws Exception {
		// create ear proj and sar proj
		IDataModel dm = CreateProjectOperationsUtility.getEARDataModel(earName, "earContent", null, null, JavaEEFacetConstants.EAR_5, false);
		OperationTestCase.runAndVerify(dm);
		IDataModel sar1Model = CreateProjectOperationsUtility.getSarDataModel(sarName, null, null, null, JavaEEFacetConstants.SAR_1);
    	OperationTestCase.runAndVerify(sar1Model);
	}
	
	protected void verifyParentChildRelationship(IServer s, String earName, String sarName) {
    	IProject earProj = ResourcesPlugin.getWorkspace().getRoot().getProject(earName);
    	IProject sarProj = ResourcesPlugin.getWorkspace().getRoot().getProject(sarName);

		IModule earModule = ServerUtil.getModule(earProj);
		IModule sarModule = ServerUtil.getModule(sarProj);
		IModule[] childModules = s.getChildModules(new IModule[]{earModule}, null);
		
		assertNotNull(childModules);
		for( int i = 0; i < childModules.length; i++ ) {
			if( childModules[i].equals(sarModule))
				return;
		}
		fail("Sar module not found as child of ear module");
	}
	protected void addSarReferenceToEar(String earName, String sarName, IPath rootRelativePath) throws CoreException {
    	IProject earProj = ResourcesPlugin.getWorkspace().getRoot().getProject(earName);
    	IProject sarProj = ResourcesPlugin.getWorkspace().getRoot().getProject(sarName);

    	// get the projects and their components
    	IVirtualComponent earComp = ComponentCore.createComponent(earProj);
    	IVirtualComponent sarComp = ComponentCore.createComponent(sarProj);
    	
    	// Create the new reference
    	IVirtualReference ref = new VirtualReference(earComp, sarComp);
		ref.setArchiveName(sarProj.getName() + ".sar");
		ref.setRuntimePath(rootRelativePath);
    	
    	// Add the sar reference to the ear
    	IDataModelProvider provider = new AddReferenceToEnterpriseApplicationDataModelProvider();
		IDataModel addDm = DataModelFactory.createDataModel(provider);
		addDm.setProperty(IAddReferenceDataModelProperties.SOURCE_COMPONENT, earComp);
		addDm.setProperty(IAddReferenceDataModelProperties.TARGET_REFERENCE_LIST, Arrays.asList(ref));
		
		IStatus stat = addDm.validateProperty(IAddReferenceDataModelProperties.TARGET_REFERENCE_LIST);
		if (stat != IDataModelProvider.OK_STATUS)
			throw new CoreException(stat);
		try {
			addDm.getDefaultOperation().execute(new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e.getMessage());
		}	

	}
	// This test will fail so it is commented
	// Need to implement something like J2EEFacetRuntimeChangedDelegate
	// See also: ProjectRuntimeTest
//	public void testProjectRuntimeModifyingClasspath() {
//		try {
//			// runtime part
//			IServer s = ServerCreationTestUtils.createMockServerWithRuntime(IJBossToolingConstants.SERVER_AS_60, "server1");
//			assertNotNull(s);
//			assertEquals("server1", s.getName());
//			IRuntime rt = s.getRuntime();
//			assertNotNull(rt);
//			
//			IDataModel sar1Model = CreateProjectOperationsUtility.getSarDataModel("sar1c", 
//					null,null, null, JavaEEFacetConstants.SAR_1);
//			
//	    	OperationTestCase.runAndVerify(sar1Model);
//	    	JobUtils.waitForIdle(1000);
//	    	
//	    	IProject sar = ResourcesPlugin.getWorkspace().getRoot().getProject("sar1c");
//			try {
//				IJavaProject jp = JavaCore.create(sar);
//				IClasspathEntry[] raw1 = jp.getRawClasspath();
//				
//				ProjectRuntimeUtil.setTargetRuntime(rt, sar);
//				raw1 = jp.getRawClasspath();
//				
//				ProjectRuntimeUtil.clearRuntime(sar);
//				raw1 = jp.getRawClasspath();
//				
//				System.out.println("done");
//			} catch( JavaModelException jme ) {
//				jme.printStackTrace();
//				fail(jme.getMessage());
//			} catch( CoreException ce ) {
//				ce.printStackTrace();
//				fail(ce.getMessage());
//			}
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
}
