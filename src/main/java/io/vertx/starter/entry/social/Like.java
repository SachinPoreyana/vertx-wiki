package io.vertx.starter.entry.social;

import java.util.List;

public class Like {
  private Integer likeCount;
  private List<User> likers;

  public Integer getLikeCount() {
    return likeCount;
  }

  public void setLikeCount(Integer likeCount) {
    this.likeCount = likeCount;
  }

  public List<User> getLikers() {
    return likers;
  }

  public void setLikers(List<User> likers) {
    this.likers = likers;
  }
}
