package io.vertx.starter.entry.wiki;

import io.vertx.starter.entry.BaseEntry;

public class Description extends BaseEntry{
  private String descriptionString;
  private String notes;

  public String getDescriptionString() {
    return descriptionString;
  }

  public void setDescriptionString(String descriptionString) {
    this.descriptionString = descriptionString;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
