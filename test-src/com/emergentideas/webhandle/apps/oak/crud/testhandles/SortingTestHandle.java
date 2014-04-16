package com.emergentideas.webhandle.apps.oak.crud.testhandles;

import javax.ws.rs.Path;

import com.emergentideas.webhandle.apps.oak.crud.CRUDHandle;
import com.emergentideas.webhandle.apps.oak.crud.testdata.Placekeeper;

@Path("/sortingtest")
public class SortingTestHandle extends CRUDHandle<Placekeeper> {

	@Override
	public String getTemplatePrefix() {
		return "placekeeper/";
	}

	@Override
	protected void setOrder(Placekeeper entity, int order) {
		entity.setItemOrder(order);
	}

	@Override
	protected String getOrderByString() {
		return "order by itemOrder";
	}

	
	
}
