package io.vertx.starter.entry.wiki;

import io.vertx.starter.entry.BaseEntry;
import io.vertx.starter.entry.social.Social;
import io.vertx.starter.entry.social.User;
import io.vertx.starter.entry.wiki.enums.Subscription;

import java.util.List;

public class Wiki extends BaseEntry{
  private String name;
  private Content content;
  private Location location;
  private Description description;
  private User contributor;
  private Social social;
  private List<Subscription> subscriptionList;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Content getContent() {
    return content;
  }

  public void setContent(Content content) {
    this.content = content;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public Description getDescription() {
    return description;
  }

  public void setDescription(Description description) {
    this.description = description;
  }

  public User getContributor() {
    return contributor;
  }

  public void setContributor(User contributor) {
    this.contributor = contributor;
  }

  public Social getSocial() {
    return social;
  }

  public void setSocial(Social social) {
    this.social = social;
  }

  public List<Subscription> getSubscriptionList() {
    return subscriptionList;
  }

  public void setSubscriptionList(List<Subscription> subscriptionList) {
    this.subscriptionList = subscriptionList;
  }
}
