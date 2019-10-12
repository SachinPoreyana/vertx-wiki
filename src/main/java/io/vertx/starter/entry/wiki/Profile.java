package io.vertx.starter.entry.wiki;

import io.vertx.starter.entry.BaseEntry;

import java.util.List;

public class Profile  extends BaseEntry{
  private String name;
  private List<Wiki> wikis;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Wiki> getWikis() {
    return wikis;
  }

  public void setWikis(List<Wiki> wikis) {
    this.wikis = wikis;
  }
}
