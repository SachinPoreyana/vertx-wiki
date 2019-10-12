package io.vertx.starter.entry.wiki;

import io.vertx.starter.entry.BaseEntry;
import io.vertx.starter.entry.wiki.enums.Direction;

import java.util.Queue;

public class Location extends BaseEntry{
  private String latitude;
  private String longitude;
  private String altitude;
  private int accuracyInMeter;
  private Direction facingDirection;
  private Queue<Location> FourNearLocations;

  public String getLatitude() {
    return latitude;
  }

  public void setLatitude(String latitude) {
    this.latitude = latitude;
  }

  public String getLongitude() {
    return longitude;
  }

  public void setLongitude(String longitude) {
    this.longitude = longitude;
  }

  public int getAccuracyInMeter() {
    return accuracyInMeter;
  }

  public void setAccuracyInMeter(int accuracyInMeter) {
    this.accuracyInMeter = accuracyInMeter;
  }

  public Direction getFacingDirection() {
    return facingDirection;
  }

  public void setFacingDirection(Direction facingDirection) {
    this.facingDirection = facingDirection;
  }

  public Queue<Location> getFourNearLocations() {
    return FourNearLocations;
  }

  public void setFourNearLocations(Queue<Location> fourNearLocations) {
    FourNearLocations = fourNearLocations;
  }

  public String getAltitude() {
    return altitude;
  }

  public void setAltitude(String altitude) {
    this.altitude = altitude;
  }
}
