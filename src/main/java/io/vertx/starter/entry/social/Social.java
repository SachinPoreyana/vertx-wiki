package io.vertx.starter.entry.social;

public class Social {
  private Like like;
  private Comment comment;

  public Like getLike() {
    return like;
  }

  public void setLike(Like like) {
    this.like = like;
  }

  public Comment getComment() {
    return comment;
  }

  public void setComment(Comment comment) {
    this.comment = comment;
  }
}
