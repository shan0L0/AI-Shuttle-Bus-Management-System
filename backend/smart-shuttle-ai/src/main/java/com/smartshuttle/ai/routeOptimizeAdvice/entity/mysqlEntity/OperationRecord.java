package com.smartshuttle.ai.routeOptimizeAdvice.entity.mysqlEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public class OperationRecord {
    private Long id;
    private Long scheduleId;
    private BigDecimal occupationRate;    // 上座率
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private BigDecimal onePointRate;      // 1星好评率
    private BigDecimal twoPointRate;      // 2星好评率
    private BigDecimal threePointRate;    // 3星好评率
    private BigDecimal fourPointRate;     // 4星好评率
    private BigDecimal fivePointRate;     // 5星好评率

    // getter and setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public BigDecimal getOccupationRate() {
        return occupationRate;
    }

    public void setOccupationRate(BigDecimal occupationRate) {
        this.occupationRate = occupationRate;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public BigDecimal getOnePointRate() {
        return onePointRate;
    }

    public void setOnePointRate(BigDecimal onePointRate) {
        this.onePointRate = onePointRate;
    }

    public BigDecimal getTwoPointRate() {
        return twoPointRate;
    }

    public void setTwoPointRate(BigDecimal twoPointRate) {
        this.twoPointRate = twoPointRate;
    }

    public BigDecimal getThreePointRate() {
        return threePointRate;
    }

    public void setThreePointRate(BigDecimal threePointRate) {
        this.threePointRate = threePointRate;
    }

    public BigDecimal getFourPointRate() {
        return fourPointRate;
    }

    public void setFourPointRate(BigDecimal fourPointRate) {
        this.fourPointRate = fourPointRate;
    }

    public BigDecimal getFivePointRate() {
        return fivePointRate;
    }

    public void setFivePointRate(BigDecimal fivePointRate) {
        this.fivePointRate = fivePointRate;
    }

    @Override
    public String toString() {
        return "OperationRecord{" +
                "id=" + id +
                ", scheduleId=" + scheduleId +
                ", occupationRate=" + occupationRate +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", onePointRate=" + onePointRate +
                ", twoPointRate=" + twoPointRate +
                ", threePointRate=" + threePointRate +
                ", fourPointRate=" + fourPointRate +
                ", fivePointRate=" + fivePointRate +
                '}';
    }
}
