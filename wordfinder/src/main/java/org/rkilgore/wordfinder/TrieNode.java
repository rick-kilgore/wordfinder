package org.rkilgore.wordfinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class TrieNode {
  public TrieNode(char ch) {
    this.ch = ch;
    this.isword = false;
    this._children = new ArrayList<TrieNode>();
  }

  public TrieNode(Scanner dictFileScanner) {
    this('*');
    this.readFromFile(dictFileScanner);
  }

  public TrieNode(String dictFilename) {
    this('*');
    try {
      Scanner scanner = new Scanner(new File(dictFilename));
      this.readFromFile(scanner);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public TrieNode isPrefix(String prefix) {
    TrieNode node = this;
    char[] chars = prefix.toLowerCase().toCharArray();
    for (char ch : chars) {
      Optional<TrieNode> child = node._children.stream().filter(tn -> tn.ch == ch).findFirst();
      if (!child.isPresent()) {
        return null;
      }
      node = child.get();
    }
    return node;
  }

  private void readFromFile(Scanner scanner) {
    try {
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
    node.isword = true;
  }

  private TrieNode getOrCreateChild(char ch) {
    Optional<TrieNode> opt = this._children.stream().filter(tn -> tn.ch == ch).findFirst();
    if (opt.isPresent()) {
      return opt.get();
    }
    TrieNode child = new TrieNode(ch);
    this._children.add(child);
    return child;
  }

  private char ch;
  public boolean isword;
  private List<TrieNode> _children;
}
