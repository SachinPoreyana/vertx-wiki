package io.vertx.starter.entry.social;

public class Comment {
  private String commentString;
  private User user;

  public String getCommentString() {
    return commentString;
  }

  public void setCommentString(String commentString) {
    this.commentString = commentString;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
