package com.amalto.core.objects.routing;


public class FailedRoutingOrderV2POJO extends AbstractRoutingOrderV2POJO{
	
	public FailedRoutingOrderV2POJO() {
		this.setStatus(FAILED);
	}
	
	public FailedRoutingOrderV2POJO(AbstractRoutingOrderV2POJO roPOJO) {
		super();
		setName(roPOJO.getName());
		setItemPOJOPK(roPOJO.getItemPOJOPK());
		setMessage(roPOJO.getMessage());
		setStatus(FAILED);
		setTimeCreated(roPOJO.getTimeCreated());
		setTimeLastRunCompleted(System.currentTimeMillis());
		setTimeLastRunStarted(roPOJO.getTimeLastRunStarted());
		setTimeScheduled(roPOJO.getTimeScheduled());
		setServiceJNDI(roPOJO.getServiceJNDI());
		setServiceParameters(roPOJO.getServiceParameters());
		setBindingUniverseName(roPOJO.getBindingUniverseName());
		setBindingUserToken(roPOJO.getBindingUserToken());
	}
	
}
