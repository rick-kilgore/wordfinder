package org.rkilgore.wordfinder;

public class TrieResult {
  public TrieResult(boolean isprefix, boolean isword) {
    this.isprefix = isprefix;
    this.isword = isword;
  }
  public String toString() {
    return "[isprefix=" + isprefix + ", isword=" + isword + "]";
  }

  public boolean isprefix;
  public boolean isword;
}
