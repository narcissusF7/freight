package com.lukachangtong.freight.web.dto;

/** 接单入参：不传车辆则用名下第一辆审核通过的车。 */
public class DriverAcceptRequest {

    private Long vehicleId;

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
}
