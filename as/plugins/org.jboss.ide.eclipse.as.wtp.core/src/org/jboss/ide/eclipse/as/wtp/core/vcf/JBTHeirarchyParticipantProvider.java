package org.jboss.ide.eclipse.as.wtp.core.vcf;

import java.util.Properties;

import org.eclipse.jst.j2ee.componentcore.J2EEModuleVirtualArchiveComponent;
import org.eclipse.jst.j2ee.componentcore.J2EEModuleVirtualComponent;
import org.eclipse.jst.j2ee.componentcore.util.EARVirtualComponent;
import org.eclipse.wst.common.componentcore.internal.flat.AbstractFlattenParticipant;
import org.eclipse.wst.common.componentcore.internal.flat.FlatVirtualComponent.FlatComponentTaskModel;
import org.eclipse.wst.common.componentcore.internal.flat.IFlattenParticipant;
import org.eclipse.wst.common.componentcore.internal.flat.IFlattenParticipantProvider;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;

public class JBTHeirarchyParticipantProvider implements IFlattenParticipantProvider {
	public static final String JBT_PROJ_IN_EAR_PARTICIPANT_ID = "jbtProjectInEarHeirarchyParticipant";
	public JBTHeirarchyParticipantProvider() {
	}
	public IFlattenParticipant findParticipant(String id, Properties properties) {
		if( JBT_PROJ_IN_EAR_PARTICIPANT_ID.equals(id)) {
			return new JBTHeirarchyParticipant();
		}
		return null;
	}
	
	public static class JBTHeirarchyParticipant extends AbstractFlattenParticipant {
		public boolean isChildModule(IVirtualComponent rootComponent,
				IVirtualReference referenced, FlatComponentTaskModel dataModel) {
			if( isJEEComponent(rootComponent) && 
					(referenced.getReferencedComponent() instanceof JBTVirtualComponent))
				return ((JBTVirtualComponent)referenced.getReferencedComponent()).canNestInsideEar();
			return false;
		}
		private boolean isJEEComponent(IVirtualComponent component) {
			IVirtualComponent tmp = component.getComponent(); // guard against caching type
			return tmp instanceof J2EEModuleVirtualComponent 
				|| tmp instanceof J2EEModuleVirtualArchiveComponent 
				|| tmp instanceof EARVirtualComponent;
		}
	}
}
