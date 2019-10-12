package io.vertx.starter.entry;

import java.util.Date;

public class BaseEntry {
  private Long id;
  private Date createdOn;
  private Date modifiedOn;
  private String createdBy;
  private Integer version;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
  }

  public Date getModifiedOn() {
    return modifiedOn;
  }

  public void setModifiedOn(Date modifiedOn) {
    this.modifiedOn = modifiedOn;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
