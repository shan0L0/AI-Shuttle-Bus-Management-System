package com.smartshuttle.ai.routeOptimizeAdvice.entity.mysqlEntity;
import java.time.LocalDateTime;
public class Schedule {
    private Long id;
    private Long routeId;
    private LocalDateTime departureTime;
    private Long vehicleId;
    private Integer arrivalTime;    // 到达时长（分钟或秒，根据业务定义）

    // getter and setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Integer getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Integer arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "id=" + id +
                ", routeId=" + routeId +
                ", departureTime=" + departureTime +
                ", vehicleId=" + vehicleId +
                ", arrivalTime=" + arrivalTime +
                '}';
    }
}
