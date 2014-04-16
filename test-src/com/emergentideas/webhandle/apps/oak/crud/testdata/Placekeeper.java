package com.emergentideas.webhandle.apps.oak.crud.testdata;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Placekeeper {

	@Id
	@GeneratedValue
	protected Integer id;
	protected String name;
	protected String desc;
	protected Integer itemOrder = Integer.MAX_VALUE;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public Integer getItemOrder() {
		return itemOrder;
	}
	public void setItemOrder(Integer itemOrder) {
		this.itemOrder = itemOrder;
	}
	
}
