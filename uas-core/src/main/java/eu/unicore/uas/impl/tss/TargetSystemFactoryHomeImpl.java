package eu.unicore.uas.impl.tss;

import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class TargetSystemFactoryHomeImpl extends DefaultHome {

	public static final String DEFAULT_TSF="default_target_system_factory";

	@Override
	protected Resource doCreateInstance(InitParameters initObjs)
			throws Exception {
		String clazz = initObjs.resourceClassName;
		return(Resource)(Class.forName(clazz).getConstructor().newInstance());
	}

	protected Resource doCreateInstance() throws Exception {
		return new TargetSystemFactoryImpl();
	}
}
