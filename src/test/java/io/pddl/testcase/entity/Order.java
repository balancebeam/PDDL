package io.pddl.testcase.entity;

public class Order {

	private long userId;
	
	private long orderId;
	
	private String status;
	
	public void setUserId(long userId){
		this.userId= userId;
	}
	
	public long getUserId(){
		return userId;
	}
	
	public void setOrderId(long orderId){
		this.orderId= orderId;
	}
	
	public long getOrderId(){
		return orderId;
	}
	
	public void setStatus(String status){
		this.status= status;
	}
	
	public String getStatus(){
		return status;
	}
}