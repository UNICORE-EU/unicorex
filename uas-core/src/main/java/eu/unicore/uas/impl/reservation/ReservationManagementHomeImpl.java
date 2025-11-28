package eu.unicore.uas.impl.reservation;

import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class ReservationManagementHomeImpl extends DefaultHome {

	@Override
	protected Resource doCreateInstance() {
		return new ReservationManagementImpl();
	}

}
