package com.emergentideas.webhandle.apps.oak.crud;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import org.apache.commons.lang.StringUtils;

import com.emergentideas.logging.Logger;
import com.emergentideas.logging.SystemOutLogger;
import com.emergentideas.utils.BeanInfoUtils;
import com.emergentideas.utils.ReflectionUtils;
import com.emergentideas.webhandle.Inject;
import com.emergentideas.webhandle.InvocationContext;
import com.emergentideas.webhandle.Location;
import com.emergentideas.webhandle.NotNull;
import com.emergentideas.webhandle.Wire;
import com.emergentideas.webhandle.assumptions.oak.RequestMessages;
import com.emergentideas.webhandle.assumptions.oak.dob.tables.TableDataModel;
import com.emergentideas.webhandle.assumptions.oak.interfaces.User;
import com.emergentideas.webhandle.composites.db.Db;
import com.emergentideas.webhandle.eventbus.EventBus;
import com.emergentideas.webhandle.handlers.Handle;
import com.emergentideas.webhandle.handlers.HttpMethod;
import com.emergentideas.webhandle.output.DirectRespondent;
import com.emergentideas.webhandle.output.Show;
import com.emergentideas.webhandle.output.Template;
import com.emergentideas.webhandle.output.Wrap;

public abstract class CRUDHandle<T> {
	
	@Resource
	protected EventBus applicationEventBus;
	
	protected EntityManager entityManager;
	protected Class<?> entityType;
	protected Logger log = SystemOutLogger.get(getClass());

	protected boolean paginateByDefault = false;
	protected Integer defaultPageSize = 100;

	@Handle(value = "/create", method = HttpMethod.POST)
	@Template
	@Wrap("app_page")
	public Object createPost(InvocationContext context, @NotNull @Inject T focus, Location location, RequestMessages messages) {

		if(validateCreate(focus, messages)) {
			entityManager.persist(focus);
			if(applicationEventBus != null) {
				applicationEventBus.emit("/object/create", focus);
			}
			return new Show(getPostCreateURL(context, focus, location, messages));
		}
		
		location.add(focus);
		addAssociatedData(context, focus, location);
		return getTemplatePrefix() + "create";
	}

	protected String getPostCreateURL(InvocationContext context, T focus, Location location, RequestMessages messages) {
		return getUrlPrefix() + "/list";
	}
	
	
	@Handle(value = "/create", method = HttpMethod.GET)
	@Template
	@Wrap("app_page")
	public Object createGet(InvocationContext context, User user, Location location) {
		addAssociatedData(context, null, location);
		return getTemplatePrefix() + "create";
	}
	
	@Handle(value = "/{id:\\d+}", method = HttpMethod.GET)
	@Template
	@Wrap("app_page")
	public Object editGet(InvocationContext context, User user, Integer id, Location location) {
		Object entity = entityManager.find(getEntityClass(), id);
		location.add(entity);
		addAssociatedData(context, (T)entity, location);
		return getTemplatePrefix() + "edit";
	}
	
	@Handle(value = "/{id:\\d+}", method = HttpMethod.POST)
	@Template
	@Wrap("app_page")
	public Object editPost(InvocationContext context, User user, @Db("id") @Inject T focus, Location location, RequestMessages messages) {
		if(validateEdit(focus, messages)) {
			if(applicationEventBus != null) {
				applicationEventBus.emit("/object/update", focus);
			}

			return new Show(getPostEditURL(context, focus, location, messages));
		}
		location.add(focus);
		addAssociatedData(context, focus, location);
		entityManager.detach(focus);
		return getTemplatePrefix() + "edit";
	}
	
	protected String getPostEditURL(InvocationContext context, T focus, Location location, RequestMessages messages) {
		return "list";
	}
	
	@Handle(value = "/{id:\\d+}/delete", method = HttpMethod.POST)
	@Template
	@Wrap("app_page")
	public Object deletePost(InvocationContext context, User user, @Db("id") T focus, Location location, RequestMessages messages) {
		if(applicationEventBus != null) {
			applicationEventBus.emit("/object/delete", focus);
		}

		deleteTheObject(context, user, focus);
		return new Show(getPostDeleteURL(context, focus, location, messages));
	}

	@Handle(value = "/sort", method = HttpMethod.POST)
	@Template
	@Wrap("app_page")
	public Object sortRows(InvocationContext context, User user, String[] order, Location location) {
		ObjectSort sort = new ObjectSort();
		
		int place = 0;
		for(String itemId : order) {
			T entity = (T)entityManager.find(getEntityClass(), transformId(itemId));
			setOrder(entity, place++);
			sort.getObjects().add(entity);
		}
		
		if(applicationEventBus != null) {
			applicationEventBus.emit("/object/sort", sort);
		}
		
		return new DirectRespondent(null, 200, null);
	}
	
	protected Object transformId(String id) {
		return Integer.parseInt(id);
	}
	
	/**
	 * You must override this method if you're using the sorted items mode. This method
	 * sets the order (however that's done) for a given entity. The order is 0 based.
	 * @param entity
	 * @param order
	 */
	protected void setOrder(T entity, int order) {
		
	}

	/**
	 * Adds information to the location object need to show the create or edit screens. These are usually objects that can
	 * be selected from a list. For example, if a new order is being created, a list of current customers might be added
	 * to the location by this method.
	 * @param context
	 * @param focus
	 * @param location
	 */
	protected void addAssociatedData(InvocationContext context, T focus, Location location) {
		
	}
	
	protected String getPostDeleteURL(InvocationContext context, T focus, Location location, RequestMessages messages) {
		return getUrlPrefix() + "/list";
	}
	
	public void deleteTheObject(InvocationContext context, User user, T focus) {
		entityManager.remove(focus);
	}
	
	@Handle(value = {"/list", "", "/"}, method = HttpMethod.GET)
	@Template
	@Wrap("app_page")
	public Object list(InvocationContext context, User user, Location location, HttpServletRequest request) {
		
		List<T> all = findEntitiesToShow(context, user, request);
		all = sortEntities(all);
		location.put("all", all);
		
		TableDataModel table = createDataTable(all);
	
		location.put("table", table);
		location.put("tileTemplate", getTileTemplate());
		location.put("tileDataTemplate", getTileDataTemplate());

		return getTemplatePrefix() + "list";
	}
	
	/**
	 * Returns the template name to use to render a tile for the row
	 * @return
	 */
	protected String getTileTemplate() {
		return "dataTables/tile";
	}
	
	/**
	 * Returns the template name to use to render just the data for a given tile
	 * @return
	 */
	protected String getTileDataTemplate() {
		return "dataTables/tile-data";
	}
	
	/**
	 * Sorts or returns a new list of sorted entities
	 * @param all
	 * @return The list passed if it was sorted in place or a new list if a new one was created.
	 */
	public List<T> sortEntities(List<T> all) {
		return all;
	}
	
	/**
	 * Finds the entities to show
	 * @param context
	 * @param user
	 * @return
	 */
	@SuppressWarnings(value = "unchecked")
	public List<T> findEntitiesToShow(InvocationContext context, User user, HttpServletRequest request) {
		
		Query q = createQueryAddingPredicate(context, user, request, "select r from " + getEntityShortName() + " r", true); 
		configureQueryForPageNumber(context, user, request, q);
		
		return q.getResultList();
	}
	
	protected Query createQueryAddingPredicate(InvocationContext context, User user, HttpServletRequest request, String queryString, boolean useOrderBy) {
		Query q = entityManager.createQuery(queryString + getOrderByString()); 
		return q;
	}
	
	protected void configureQueryForPageNumber(InvocationContext context, User user, HttpServletRequest request, Query q) {
		Location location = context.getLocation();
		
		String pageSize = request.getParameter("pageSize");
		String pageNumber = request.getParameter("pageNumber");
		
		Integer iPageNumber = null;
		Integer iPageSize = defaultPageSize;
		
		if(StringUtils.isNotBlank(pageNumber)) {
			iPageNumber = Integer.parseInt(pageNumber);
		}
		
		if(StringUtils.isNotBlank(pageSize)) {
			iPageSize = Integer.parseInt(pageSize);
		}
		
		if(iPageNumber == null && isPaginateByDefault()) {
			iPageNumber = 0;
		}
		
		if(iPageNumber != null) {
			q.setFirstResult(iPageNumber * iPageSize);
			q.setMaxResults(iPageSize);
			long total = getTotalCountOfObjects(context, user, request);
			addPageCountData(request, location, iPageNumber, iPageSize, total);
		}
	}
	
	protected long getTotalCountOfObjects(InvocationContext context, User user, HttpServletRequest request) {
		
		return (Long)createQueryAddingPredicate(context, user, request, "select count(r) from " + getEntityShortName() + " r", false).getSingleResult();
	}
	
	protected void addPageCountData(HttpServletRequest request, Location location, Integer pageNumber, Integer pageSize, Long total) {
		location.put("pageNumber", pageNumber);
		location.put("pageSize", pageSize);
		location.put("totalRows", total);
		location.put("fromRow", pageNumber * pageSize);
		
		Integer toRow = (pageNumber + 1) * pageSize;
		if(toRow > total) {
			toRow = (int)total.longValue();
		}
		location.put("toRow", toRow);
		
		String searchPredicate = getNextPageSearchUrlAddition(request, location);
		
		location.put("pageChangePrefix", getUrlPrefix() + "/list?" + 
		(StringUtils.isBlank(searchPredicate) ? "" : searchPredicate + "&") + "pageSize=" + pageSize + "&pageNumber=");
		location.put("hasPreviousPage", pageNumber > 0);
		location.put("hasNextPage", total > ((pageNumber + 1) * pageSize));
		
	}
	
	protected String getNextPageSearchUrlAddition(HttpServletRequest request, Location location) {
		return null;
	}
	
	/**
	 * You must override this method if you're using the sorted mode. This method adds a query predicate to sort the
	 * items in the way they should initially be ordered.
	 * @return
	 */
	protected String getOrderByString() {
		return "";
	}
	
	/**
	 * Creates the data table to be shown by the list view
	 * @param all All of the entities to show
	 * @return
	 */
	public TableDataModel createDataTable(List<T> all) {
		List<String> propertyNames = determinePropertyNames();
		List<String> headers = determinePropertyLabels(propertyNames);
		
		String prefix = getUrlPrefix();
		if(prefix.endsWith("/") == false) {
			prefix += "/";
		}
		
		String idProperty = BeanInfoUtils.determineIdPropertyNameWithGetter(getEntityClass());
		TableDataModel table = new TableDataModel()
		.setHeaders(headers.toArray(new String[headers.size()]))
		.setProperties(propertyNames.toArray(new String[propertyNames.size()]))
		.setDeleteURLPattern(prefix, idProperty, "/delete")
		.setEditURLPattern(0, prefix, idProperty, "")
		.setCreateNewURL(prefix + "create")
		.setSortURL(prefix + "sort")
		.setItems(all.toArray());
		
		modifyDataTable(all, table);
		
		return table;
	}
	
	/**
	 * Determines with property names should be shown for this table.  By default, this examines
	 * the entity class for non-object, non-id, properties.
	 * @return
	 */
	protected List<String> determinePropertyNames() {
		return BeanInfoUtils.determineNonIdNonObjectPropertyNamesWithGetters(getEntityClass());
	}
	
	/**
	 * For the property names, determine what labels should be shown for each property.
	 * @param propertyNames The properties that will be shown in the table.
	 * @return
	 */
	protected List<String> determinePropertyLabels(List<String> propertyNames) {
		return BeanInfoUtils.formatCamelCasePropertyNames(propertyNames);
	}
	
	/**
	 * A hook to add template specs, change column headers, etc
	 * @param allEntities
	 * @param table
	 */
	public void modifyDataTable(List<T> allEntities, TableDataModel table) {
		
	}

	/**
	 * Return true if valid, false otherwise
	 * @param focus
	 * @param messages
	 * @return
	 */
	public boolean validateEdit(T focus, RequestMessages messages) {
		return true;
	}
	
	/**
	 * Return true if valid, false otherwise
	 * @param focus
	 * @param messages
	 * @return
	 */
	public boolean validateCreate(T focus, RequestMessages messages) {
		return true;
	}
	
	public Class<?> getEntityClass() {
		if(entityType == null) {
			entityType = (Class<?>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		}
		return entityType;
	}
	
	
	public String getEntityShortName() {
		String name = getEntityClass().getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		return name;
	}
	
	/**
	 * Returns the url prefix which this handle uses like <code>/people</code>.  By default, this is the first
	 * string of the value attribute of this class's {@link Handle} attribute.
	 * @return
	 */
	public String getUrlPrefix() {
		Handle h = ReflectionUtils.getAnnotationOnClass(getClass(), Handle.class);
		if(h != null) {
			return h.value()[0];
		}
		Path p = ReflectionUtils.getAnnotationOnClass(getClass(), Path.class);
		if(p != null) {
			return p.value();
		}
		
		return "";
	}
	
	/**
	 * Truncates the list to be the cut list. Returns the list that is truncated (the one passed in).
	 * @param l The list to truncate
	 * @param cutLength The length of the list after it has been truncated
	 */
	protected <T> List<T> cutAfter(List<T> l, int cutLength) {
		while(l.size() > cutLength) {
			l.remove(cutLength);
		}
		
		return l;
	}

	
	/**
	 * Defines the template prefix for the create, edit, and list templates. A value here might be
	 * "/people/contacts/" which will be prefixed to the string "edit" to come up with the template
	 * name
	 * @return
	 */
	public abstract String getTemplatePrefix();

	public EntityManager getEntityManager() {
		return entityManager;
	}

	@Wire
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}
	
	public boolean isPaginateByDefault() {
		return paginateByDefault;
	}
	
	
}
