package io.vertx.starter.entry.wiki;

import io.vertx.starter.entry.BaseEntry;

import java.util.List;

public class Content extends BaseEntry {
  private List<String> imageUrls;
  private List<Description> descriptions;
  private List<String> vedioUrls;

  public List<String> getImageUrls() {
    return imageUrls;
  }

  public void setImageUrls(List<String> imageUrls) {
    this.imageUrls = imageUrls;
  }

  public List<Description> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(List<Description> descriptions) {
    this.descriptions = descriptions;
  }

  public List<String> getVedioUrls() {
    return vedioUrls;
  }

  public void setVedioUrls(List<String> vedioUrls) {
    this.vedioUrls = vedioUrls;
  }
}
