package com.emergentideas.webhandle.apps.oak.crud.testhandles;

import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.commons.lang.StringUtils;

import com.emergentideas.webhandle.InvocationContext;
import com.emergentideas.webhandle.Location;
import com.emergentideas.webhandle.apps.oak.crud.CRUDHandle;
import com.emergentideas.webhandle.apps.oak.crud.testdata.Placekeeper;
import com.emergentideas.webhandle.assumptions.oak.interfaces.User;
import com.emergentideas.webhandle.output.Template;
import com.emergentideas.webhandle.output.Wrap;

@Path("/paginatedtest")
public class PaginatedTestHandle extends CRUDHandle<Placekeeper> {

	public PaginatedTestHandle() {
		paginateByDefault = true;
	}
	
	@Override
	public String getTemplatePrefix() {
		return "paginated/";
	}

	@Override
	protected void setOrder(Placekeeper entity, int order) {
		entity.setItemOrder(order);
	}

	@Override
	protected String getOrderByString() {
		return " order by r.itemOrder";
	}
	
	@Path("search")
	@GET
	@Template
	@Wrap("app_page")
	public Object getSearchForm(Location location) {
		return "paginated/search-form";
	}

	@Override
	protected Query createQueryAddingPredicate(InvocationContext context,
			User user, HttpServletRequest request, String queryString, boolean useOrderBy) {
		String predicate = "";
		
		String searchBy = request.getParameter("searchBy");
		if(StringUtils.isNotBlank(searchBy)) {
			predicate = " where r.name like :searchByName ";
		}
		
		Query q = entityManager.createQuery(queryString + predicate + (useOrderBy ? getOrderByString() : ""));
		
		if(StringUtils.isNotBlank(searchBy)) {
			q.setParameter("searchByName", "%" + searchBy + "%");
		}
		
		return q;
	}

	@Override
	protected String getNextPageSearchUrlAddition(HttpServletRequest request,
			Location location) {
		String searchBy = request.getParameter("searchBy");
		if(StringUtils.isNotBlank(searchBy)) {
			return "searchBy=" + searchBy;
		}
		
		return super.getNextPageSearchUrlAddition(request, location);
	}
	

	
}
