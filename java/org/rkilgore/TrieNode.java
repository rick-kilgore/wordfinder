package org.rkilgore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import org.rkilgore.TrieResult;

public class TrieNode {
  public TrieNode(char ch) {
    this._ch = ch;
    this._children = new ArrayList<TrieNode>();
    this._isword = false;
  }

  public TrieNode(String dictFilename) {
    this('*');
    this.readFromFile(dictFilename);
  }

  public TrieResult isPrefix(String prefix) {
    TrieNode node = this;
    char[] chars = prefix.toLowerCase().toCharArray();
    for (char ch : chars) {
      Optional<TrieNode> child = node._children.stream().filter(tn -> tn._ch == ch).findFirst();
      if (!child.isPresent()) {
        return new TrieResult(false, false);
      }
      node = child.get();
    }
    return new TrieResult(true, node._isword);
  }

  private void readFromFile(String dictFilename) {
    try {
      Scanner scanner = new Scanner(new File(dictFilename));
      while (scanner.hasNextLine()) {
        String word = scanner.nextLine();
        this.insert(word);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void insert(String word) {
    TrieNode node = this;
    char[] chars = word.toLowerCase().toCharArray();
    for (char ch : chars) {
      node = node.getOrCreateChild(ch);
    }
    node._isword = true;
  }

  private TrieNode getOrCreateChild(char ch) {
    Optional<TrieNode> opt = this._children.stream().filter(tn -> tn._ch == ch).findFirst();
    if (opt.isPresent()) {
      return opt.get();
    }
    TrieNode child = new TrieNode(ch);
    this._children.add(child);
    return child;
  }

  private char _ch;
  private List<TrieNode> _children;
  private boolean _isword;
}
